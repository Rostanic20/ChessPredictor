package com.chesspredictor.eval

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.chesspredictor.domain.humanlike.MaiaEncoding
import com.chesspredictor.domain.humanlike.MaiaNetwork
import com.chesspredictor.domain.humanlike.MaiaPolicy
import com.chesspredictor.domain.humanlike.MaiaWeights
import com.chesspredictor.domain.usecases.ChessRulesEngine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assume.assumeTrue
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
                val legal = rules.getLegalMoves(rules.parseGameState(fen)).map { it.toUci() }
                val logits = run(environment, session, tokens, reference.selfElo, reference.oppoElo)
                val best = MaiaPolicy.distribution(logits, legal, blackToMove).maxBy { it.value }.key
                if (best == reference.best) agree++
                if (best == reference.human) humanHits++
            }
            println("tokens identical for $tokensChecked positions; same move as Python on $agree/${references.size}; human move on $humanHits")
            assertEquals(references.size, agree, "Kotlin must choose the Python move everywhere with the fp32 model")
        }
    }

    @Test
    fun kotlinNetworkMatchesTheOnnxModel() {
        val referencePath = System.getProperty("maiaReference")
        val modelPath = System.getProperty("maiaModel")
        val weightsPath = System.getProperty("maiaWeights")
        assumeTrue("set -PmaiaReference, -PmaiaModel and -PmaiaWeights to run", referencePath != null && modelPath != null && weightsPath != null)
        val references = File(referencePath).readLines().filter { it.isNotBlank() }.map { json.decodeFromString<Reference>(it) }
        val network = MaiaNetwork(MaiaWeights.parse(File(weightsPath).readBytes()))

        val environment = OrtEnvironment.getEnvironment()
        environment.createSession(File(modelPath).readBytes(), OrtSession.SessionOptions()).use { session ->
            var agree = 0
            var humanHits = 0
            var worstGap = 0f
            var nanos = 0L
            references.forEach { reference ->
                val fen = reference.fens.last()
                val legal = rules.getLegalMoves(rules.parseGameState(fen)).map { it.toUci() }
                val blackToMove = MaiaEncoding.blackToMove(fen)
                val started = System.nanoTime()
                val kotlinLogits = network.logits(reference.fens, reference.selfElo, reference.oppoElo)
                nanos += System.nanoTime() - started
                val onnxLogits = run(environment, session, MaiaEncoding.tokens(reference.fens), reference.selfElo, reference.oppoElo)
                legal.forEach { uci ->
                    val index = MaiaEncoding.moveIndex(uci, blackToMove)
                    worstGap = maxOf(worstGap, kotlin.math.abs(kotlinLogits[index] - onnxLogits[index]))
                }
                val best = MaiaPolicy.distribution(kotlinLogits, legal, blackToMove).maxBy { it.value }.key
                if (best == reference.best) agree++
                if (best == reference.human) humanHits++
            }
            println(
                "kotlin network: same move as Python on $agree/${references.size}, human move on $humanHits, " +
                    "largest legal-move logit gap $worstGap, ${nanos / references.size / 1_000_000} ms per move"
            )
            assertTrue(agree >= references.size * 0.98, "only $agree/${references.size} moves agree")
            assertTrue(worstGap < 0.5f, "logit gap $worstGap")
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
