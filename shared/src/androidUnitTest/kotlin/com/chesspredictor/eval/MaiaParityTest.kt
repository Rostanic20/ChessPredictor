package com.chesspredictor.eval

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.humanlike.MaiaEncoding
import com.chesspredictor.domain.humanlike.MaiaPolicy
import com.chesspredictor.domain.usecases.ChessRulesEngine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assume.assumeTrue
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class MaiaParityTest {

    @Serializable
    data class Reference(
        val fens: List<String>,
        @kotlinx.serialization.SerialName("self_elo") val selfElo: Int,
        @kotlinx.serialization.SerialName("oppo_elo") val oppoElo: Int,
        val best: String,
        val human: String,
        val band: Int,
        val tokens: List<Float>? = null
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val rules = ChessRulesEngine()

    @Test
    fun kotlinEncodingPicksTheSameMovesAsPythonMaia() {
        val referencePath = System.getProperty("maiaReference")
        val modelPath = System.getProperty("maiaModel")
        assumeTrue("set -PmaiaReference and -PmaiaModel to run", referencePath != null && modelPath != null)
        val references = File(referencePath).readLines().filter { it.isNotBlank() }.map { json.decodeFromString<Reference>(it) }

        val environment = OrtEnvironment.getEnvironment()
        environment.createSession(File(modelPath).readBytes(), OrtSession.SessionOptions()).use { session ->
            var tokensChecked = 0
            var agree = 0
            var humanHits = 0
            references.forEach { reference ->
                val tokens = MaiaEncoding.tokens(reference.fens)
                reference.tokens?.let { expected ->
                    assertEquals(expected.size, tokens.size)
                    expected.indices.forEach { i -> assertEquals(expected[i], tokens[i], "token $i of ${reference.fens.last()}") }
                    tokensChecked++
                }
                val fen = reference.fens.last()
                val blackToMove = MaiaEncoding.blackToMove(fen)
                val legal = rules.getLegalMoves(rules.parseGameState(fen)).map { move ->
                    "${move.from}${move.to}" + when (move.promotion) {
                        is ChessPiece.Queen -> "q"
                        is ChessPiece.Rook -> "r"
                        is ChessPiece.Bishop -> "b"
                        is ChessPiece.Knight -> "n"
                        else -> ""
                    }
                }
                val logits = run(environment, session, tokens, reference.selfElo, reference.oppoElo)
                val best = MaiaPolicy.distribution(logits, legal, blackToMove).maxBy { it.value }.key
                if (best == reference.best) agree++
                if (best == reference.human) humanHits++
            }
            println("tokens identical for $tokensChecked positions; same move as Python on $agree/${references.size}; human move on $humanHits")
            assertEquals(references.size, agree, "Kotlin must choose the Python move everywhere with the fp32 model")
        }
    }

    private fun run(environment: OrtEnvironment, session: OrtSession, tokens: FloatArray, selfElo: Int, oppoElo: Int): FloatArray {
        val shape = longArrayOf(1, MaiaEncoding.SQUARES.toLong(), MaiaEncoding.FEATURES.toLong())
        OnnxTensor.createTensor(environment, FloatBuffer.wrap(tokens), shape).use { input ->
            OnnxTensor.createTensor(environment, LongBuffer.wrap(longArrayOf(selfElo.toLong())), longArrayOf(1)).use { self ->
                OnnxTensor.createTensor(environment, LongBuffer.wrap(longArrayOf(oppoElo.toLong())), longArrayOf(1)).use { oppo ->
                    session.run(mapOf("tokens" to input, "self_elo" to self, "oppo_elo" to oppo), setOf("move_logits")).use { result ->
                        @Suppress("UNCHECKED_CAST")
                        return (result[0].value as Array<FloatArray>)[0]
                    }
                }
            }
        }
    }
}
