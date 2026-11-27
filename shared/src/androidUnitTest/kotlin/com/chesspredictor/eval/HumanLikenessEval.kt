package com.chesspredictor.eval

import com.chesspredictor.data.repositories.ChessEngineRepositoryImpl
import com.chesspredictor.data.utils.MoveParserImpl
import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.EngineDifficulty
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.usecases.ChessRulesEngine
import com.chesspredictor.presentation.ChessConstants
import com.chesspredictor.presentation.managers.EngineManager
import com.chesspredictor.presentation.managers.HumanBehaviorManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.time.Duration.Companion.hours

class HumanLikenessEval {

    @Serializable
    data class Position(
        val fen: String,
        val human: String,
        val rating: Int,
        val band: Int,
        val ply: Int,
        val moves: List<String>,
        val game: String,
        @SerialName("clock_ms") val clockMs: Long,
        @SerialName("think_ms") val thinkMs: Long
    )

    @Serializable
    data class Result(
        val game: String,
        val ply: Int,
        val band: Int,
        val variant: String,
        val human: String,
        val engine: String,
        val match: Boolean
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val maxConsecutiveFailures = 5
    private val rules = ChessRulesEngine()
    private val parser = MoveParserImpl()

    @Test
    fun measureMoveMatching() = runTest(timeout = 12.hours) {
        val data = System.getProperty("evalData")
        assumeTrue("set -PevalData to run", data != null)
        val variant = System.getProperty("evalVariant") ?: "app"
        val limit = System.getProperty("evalLimit")?.toInt() ?: Int.MAX_VALUE
        val out = File(System.getProperty("evalOut") ?: "$data.$variant.results.jsonl")
        val script = File(System.getProperty("evalEngine") ?: "src/jsMain/resources/stockfish.js")
        val failures = File("${out.path}.failures").apply { writeText("") }

        val positions = File(data).readLines().filter { it.isNotBlank() }
            .map { json.decodeFromString<Position>(it) }
            .drop(System.getProperty("evalSkip")?.toInt() ?: 0)
            .take(limit)

        NodeStockfish(script, File("${out.path}.engine.log")).use { engine ->
            engine.initialize()
            val manager = EngineManager(
                ChessEngineRepositoryImpl(engine, parser),
                backgroundScope,
                HumanBehaviorManager(backgroundScope)
            )
            manager.setReady(true)

            out.bufferedWriter().use { writer ->
                var consecutiveFailures = 0
                positions.forEachIndexed { index, position ->
                    val board = boardAfter(position)
                    val settings = settingsFor(position.band, variant)
                    val chosen = CompletableDeferred<ChessMove?>()
                    manager.requestEngineMove(board, settings) { chosen.complete(it) }
                    val watchdog = CoroutineScope(Dispatchers.Default).launch {
                        delay(60_000)
                        failures.appendText(
                            "no move at ${position.game} ply ${position.ply}: thinking=${manager.isThinking.value} " +
                                "error=${manager.error.value} ${engine.diagnostics()}\n"
                        )
                        chosen.complete(null)
                    }
                    val move = chosen.await()
                    watchdog.cancel()
                    manager.cancelPendingMove()
                    if (move == null) {
                        consecutiveFailures++
                        check(consecutiveFailures < maxConsecutiveFailures) {
                            "$consecutiveFailures positions in a row got no move; see ${failures.path}"
                        }
                        return@forEachIndexed
                    }
                    consecutiveFailures = 0

                    val engineUci = move.toUci()
                    val result = Result(position.game, position.ply, position.band, variant, position.human, engineUci, engineUci == position.human)
                    writer.write(json.encodeToString(result))
                    writer.newLine()
                    writer.flush()
                    if (index % 50 == 49) println("${index + 1}/${positions.size}")
                }
            }
        }
        println(summary(out))
    }

    private fun boardAfter(position: Position): ChessBoard {
        var state = rules.parseGameState(ChessConstants.STARTING_POSITION_FEN)
        for (uci in position.moves) {
            val move = requireNotNull(parser.parseUciMove(uci, toBoard(state))) { "unparsable $uci in ${position.game}" }
            state = requireNotNull(rules.makeMove(state, move)) { "illegal $uci in ${position.game}" }
        }
        check(state.toFen().split(" ").take(3) == position.fen.split(" ").take(3)) {
            "replay of ${position.game} reached ${state.toFen()} instead of ${position.fen}"
        }
        return toBoard(state)
    }

    private fun toBoard(state: GameState) = ChessBoard(
        fen = state.toFen(),
        moveHistory = state.moveHistory.map { it.move },
        turn = state.turn,
        castlingRights = state.castlingRights,
        enPassantSquare = state.enPassantSquare,
        halfMoveClock = state.halfMoveClock,
        fullMoveNumber = state.fullMoveNumber
    )

    private fun settingsFor(band: Int, variant: String): EngineSettings {
        val difficulty = when (band) {
            1200 -> EngineDifficulty.CASUAL
            1600 -> EngineDifficulty.CLUB_PLAYER
            else -> EngineDifficulty.EXPERT
        }
        val settings = EngineSettings().withDifficulty(difficulty)
        return when (variant) {
            "app" -> settings
            "plain" -> settings.copy(humanStyle = false)
            else -> error("unknown variant $variant")
        }
    }

    private fun summary(results: File): String {
        val rows = results.readLines().filter { it.isNotBlank() }.map { json.decodeFromString<Result>(it) }
        return rows.groupBy { it.band }.toSortedMap().entries.joinToString("\n") { (band, list) ->
            val hits = list.count { it.match }
            "band $band: ${hits}/${list.size} = ${"%.1f".format(100.0 * hits / list.size)}%"
        }
    }
}
