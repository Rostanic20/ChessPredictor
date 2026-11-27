package com.chesspredictor.presentation.managers

import com.chesspredictor.domain.entities.CastlingRights
import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.EngineAnalysis
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.humanlike.GamePhase
import com.chesspredictor.domain.humanlike.HumanMovePredictor
import com.chesspredictor.domain.humanlike.HumanThinkTime
import com.chesspredictor.domain.humanlike.MaiaEncoding
import com.chesspredictor.domain.humanlike.MaiaMoveChooser
import com.chesspredictor.domain.humanlike.MoveKind
import com.chesspredictor.domain.humanlike.ThinkCell
import com.chesspredictor.domain.humanlike.ThinkPace
import com.chesspredictor.domain.humanlike.TimeLeft
import com.chesspredictor.domain.repositories.ChessEngineRepository
import com.chesspredictor.domain.repositories.PositionAnalysis
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class EngineManagerTest {

    private class PendingRepository : ChessEngineRepository {
        val replies = mutableListOf<CompletableDeferred<ChessMove?>>()

        override suspend fun analyzeBestMove(board: ChessBoard, settings: EngineSettings): ChessMove? {
            val reply = CompletableDeferred<ChessMove?>()
            replies += reply
            return reply.await()
        }

        override suspend fun analyzePosition(board: ChessBoard, settings: EngineSettings) =
            PositionAnalysis(bestMove = null, evaluation = 0f)

        override suspend fun analyzeWithEvaluation(board: ChessBoard, settings: EngineSettings): EngineAnalysis? = null
        override suspend fun validateMove(board: ChessBoard, move: ChessMove) = true
        override suspend fun getAvailableMoves(board: ChessBoard) = emptyList<ChessMove>()
        override fun observeEvaluation(): Flow<Float> = emptyFlow()
    }

    private val board = ChessBoard(
        fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
        turn = ChessColor.BLACK,
        castlingRights = CastlingRights(true, true, true, true)
    )
    private val settings = EngineSettings(humanStyle = false)
    private val e5 = ChessMove(Square('e', 7), Square('e', 5), ChessPiece.Pawn(ChessColor.BLACK))
    private val c5 = ChessMove(Square('c', 7), Square('c', 5), ChessPiece.Pawn(ChessColor.BLACK))

    private fun TestScope.readyManager(repository: ChessEngineRepository): EngineManager {
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        return EngineManager(repository, scope).also { it.setReady(true) }
    }

    @Test
    fun cancelledRequestNeverDeliversItsMove() = runTest {
        val repository = PendingRepository()
        val manager = readyManager(repository)
        val delivered = mutableListOf<ChessMove>()

        manager.requestEngineMove(board, settings) { delivered += it }
        runCurrent()
        assertTrue(manager.isThinking.value)

        manager.cancelPendingMove()
        repository.replies.single().complete(e5)
        runCurrent()

        assertEquals(emptyList(), delivered)
        assertFalse(manager.isThinking.value)
        assertNull(manager.error.value)
    }

    private class SlowAnalysisRepository : ChessEngineRepository {
        val pendingAnalyses = mutableListOf<CompletableDeferred<PositionAnalysis>>()

        override suspend fun analyzeBestMove(board: ChessBoard, settings: EngineSettings): ChessMove? =
            ChessMove(Square('e', 7), Square('e', 5), ChessPiece.Pawn(ChessColor.BLACK))

        override suspend fun analyzePosition(board: ChessBoard, settings: EngineSettings): PositionAnalysis {
            val reply = CompletableDeferred<PositionAnalysis>()
            pendingAnalyses += reply
            return reply.await()
        }

        override suspend fun analyzeWithEvaluation(board: ChessBoard, settings: EngineSettings): EngineAnalysis? = null
        override suspend fun validateMove(board: ChessBoard, move: ChessMove) = true
        override suspend fun getAvailableMoves(board: ChessBoard) = emptyList<ChessMove>()
        override fun observeEvaluation(): Flow<Float> = emptyFlow()
    }

    @Test
    fun moveIsDeliveredBeforeTheFollowUpAnalysisFinishes() = runTest {
        val repository = SlowAnalysisRepository()
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        val manager = EngineManager(repository, scope, HumanBehaviorManager(scope)).also { it.setReady(true) }
        val humanSettings = EngineSettings(humanStyle = true, timeLimit = 100)
        val delivered = mutableListOf<ChessMove>()

        manager.requestEngineMove(board, humanSettings) { delivered += it }
        runCurrent()

        assertEquals(listOf(e5), delivered)
        assertEquals(1, repository.pendingAnalyses.size)
        assertFalse(manager.isThinking.value)

        manager.requestEngineMove(board, humanSettings) { delivered += it }
        runCurrent()

        assertEquals(2, repository.pendingAnalyses.size)
        repository.pendingAnalyses.first().complete(PositionAnalysis(bestMove = null, evaluation = 0f))
        runCurrent()

        assertEquals(listOf(e5, e5), delivered)
        assertFalse(manager.isThinking.value)
        assertNull(manager.error.value)
    }

    @Test
    fun aHumanStyleMoveArrivesAfterTheSampledThinkTime() = runTest {
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        val manager = EngineManager(SlowAnalysisRepository(), scope, thinkTime = HumanThinkTime(Random(7))).also { it.setReady(true) }
        val expected = HumanThinkTime(Random(7)).sampleMs(ThinkCell(GamePhase.OPENING, TimeLeft.PLENTY, MoveKind.NORMAL), ThinkPace.CASUAL)
        val delivered = mutableListOf<ChessMove>()

        manager.requestEngineMove(board, EngineSettings(humanStyle = true)) { delivered += it }
        advanceTimeBy(expected - 200)
        assertEquals(emptyList(), delivered)
        assertTrue(manager.isThinking.value)

        advanceTimeBy(201)
        assertEquals(listOf(e5), delivered)
    }

    @Test
    fun theClockCapShortensTheThinkTime() = runTest {
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        val manager = EngineManager(SlowAnalysisRepository(), scope, thinkTime = HumanThinkTime(Random(7))).also { it.setReady(true) }
        val uncapped = HumanThinkTime(Random(7)).sampleMs(ThinkCell(GamePhase.OPENING, TimeLeft.PLENTY, MoveKind.NORMAL), ThinkPace.CASUAL)
        assertTrue(uncapped > 400, "seed must give a think time above the cap, got $uncapped")
        val delivered = mutableListOf<ChessMove>()

        manager.requestEngineMove(board, EngineSettings(humanStyle = true), maxThinkMs = 300) { delivered += it }
        advanceTimeBy(301)

        assertEquals(listOf(e5), delivered)
    }

    private fun favouring(uci: String) = HumanMovePredictor { _, _, _ ->
        FloatArray(MaiaEncoding.MOVE_COUNT).also { it[MaiaEncoding.moveIndex(uci, blackToMove = true)] = 50f }
    }

    @Test
    fun theHumanModelChoosesTheMoveWithoutAskingStockfish() = runTest {
        val repository = PendingRepository()
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        val manager = EngineManager(
            repository, scope,
            moveChooser = MaiaMoveChooser(favouring("c7c5")),
            background = StandardTestDispatcher(testScheduler)
        ).also { it.setReady(true) }
        val delivered = mutableListOf<ChessMove>()

        manager.requestEngineMove(board, EngineSettings(humanStyle = true)) { delivered += it }
        advanceTimeBy(120_000)

        assertEquals(listOf("c7c5"), delivered.map { it.toUci() })
        assertEquals(0, repository.replies.size)
    }

    @Test
    fun theHumanModelKeepsPlayingInTimeTrouble() = runTest {
        val repository = PendingRepository()
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        val manager = EngineManager(
            repository, scope,
            moveChooser = MaiaMoveChooser(favouring("c7c5")),
            background = StandardTestDispatcher(testScheduler)
        ).also { it.setReady(true) }
        val delivered = mutableListOf<ChessMove>()

        manager.requestEngineMove(board, EngineSettings(humanStyle = true, timeLimit = 100)) { delivered += it }
        advanceTimeBy(1_000)

        assertEquals(listOf("c7c5"), delivered.map { it.toUci() })
        assertEquals(0, repository.replies.size)
    }

    @Test
    fun aFailingHumanModelFallsBackToStockfish() = runTest {
        val repository = PendingRepository()
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        val broken = HumanMovePredictor { _, _, _ -> error("model missing") }
        val manager = EngineManager(
            repository, scope,
            moveChooser = MaiaMoveChooser(broken),
            background = StandardTestDispatcher(testScheduler)
        ).also { it.setReady(true) }
        val delivered = mutableListOf<ChessMove>()

        manager.requestEngineMove(board, EngineSettings(humanStyle = true)) { delivered += it }
        runCurrent()
        repository.replies.single().complete(e5)
        advanceTimeBy(120_000)

        assertEquals(listOf(e5), delivered)
        assertNull(manager.error.value)
    }

    @Test
    fun newRequestSupersedesThePreviousOne() = runTest {
        val repository = PendingRepository()
        val manager = readyManager(repository)
        val delivered = mutableListOf<ChessMove>()

        manager.requestEngineMove(board, settings) { delivered += it }
        runCurrent()
        manager.requestEngineMove(board, settings) { delivered += it }
        runCurrent()
        assertTrue(manager.isThinking.value)

        repository.replies[0].complete(e5)
        repository.replies[1].complete(c5)
        runCurrent()

        assertEquals(listOf(c5), delivered)
        assertFalse(manager.isThinking.value)
    }
}
