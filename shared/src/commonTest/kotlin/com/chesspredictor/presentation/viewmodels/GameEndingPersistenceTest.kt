package com.chesspredictor.presentation.viewmodels

import com.chesspredictor.data.repositories.GameStateRepository
import com.chesspredictor.data.repositories.SavedGameCodec
import com.chesspredictor.data.repositories.SavedGameState
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.GameEnding
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.entities.TimeControl
import com.chesspredictor.domain.usecases.ChessRulesEngine
import com.chesspredictor.presentation.ChessConstants
import com.chesspredictor.presentation.managers.AnimationController
import com.chesspredictor.presentation.managers.AnimationManager
import com.chesspredictor.presentation.managers.GameClock
import com.chesspredictor.presentation.managers.GameStateManager
import com.chesspredictor.presentation.managers.GameStatus
import com.chesspredictor.presentation.managers.MoveController
import com.chesspredictor.presentation.managers.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GameEndingPersistenceTest {

    private class InMemoryRepository : GameStateRepository {
        val stored = MutableStateFlow<SavedGameState?>(null)
        override suspend fun saveGameState(gameState: SavedGameState) { stored.value = gameState }
        override suspend fun loadGameState(): SavedGameState? = stored.value
        override suspend fun clearGameState() { stored.value = null }
        override fun observeGameState(): Flow<SavedGameState?> = stored
    }

    private val engine = ChessRulesEngine()

    private fun TestScope.persistence(repository: GameStateRepository): Pair<GamePersistence, GameClock> {
        val manager = GameStateManager(engine)
        val controller = MoveController(engine, manager, AnimationController(AnimationManager()))
        val clock = GameClock(backgroundScope, testScheduler.timeSource) {}
        return GamePersistence(repository, SettingsManager(), controller, clock) to clock
    }

    private fun afterE4E5(): GameState {
        val start = engine.parseGameState(ChessConstants.STARTING_POSITION_FEN)
        return listOf("e2e4", "e7e5").fold(start) { state, uci ->
            val from = Square.fromString(uci.substring(0, 2))
            val move = ChessMove(from, Square.fromString(uci.substring(2, 4)), state.board.getValue(from))
            assertNotNull(engine.makeMove(state, move))
        }
    }

    @Test
    fun anAgreedDrawAndTheClockSurviveARestart() = runTest {
        val repository = InMemoryRepository()
        val (saver, clockBefore) = persistence(repository)
        clockBefore.restore(TimeControl.of(300_000, 0), mapOf(ChessColor.WHITE to 41_000L, ChessColor.BLACK to 52_000L))
        saver.save(afterE4E5(), GameEnding.DRAW_AGREED)

        val (loader, clockAfter) = persistence(repository)
        val restored = assertNotNull(loader.load())

        assertEquals(GameEnding.DRAW_AGREED, restored.ending)
        assertEquals(2, restored.gameState.moveHistory.size)
        assertEquals(TimeControl.of(300_000, 0), clockAfter.timeControl.value)
        assertEquals(mapOf(ChessColor.WHITE to 41_000L, ChessColor.BLACK to 52_000L), clockAfter.remaining.value)
    }

    @Test
    fun aGameWithoutAClockRestoresWithoutOne() = runTest {
        val repository = InMemoryRepository()
        persistence(repository).first.save(afterE4E5(), null)

        val (loader, clock) = persistence(repository)
        val restored = assertNotNull(loader.load())

        assertNull(restored.ending)
        assertNull(clock.timeControl.value)
    }

    @Test
    fun adjudicatedResultsUseTheFlaggedSide() {
        assertEquals("1/2-1/2", GameStatus(isDraw = true, ending = GameEnding.DRAW_AGREED).adjudicatedResult())
        assertEquals("0-1", GameStatus(ending = GameEnding.WHITE_OUT_OF_TIME).adjudicatedResult())
        assertEquals("1-0", GameStatus(ending = GameEnding.BLACK_OUT_OF_TIME).adjudicatedResult())
        assertEquals("1/2-1/2", GameStatus(isDraw = true, ending = GameEnding.BLACK_OUT_OF_TIME).adjudicatedResult())
        assertNull(GameStatus(isCheckmate = true).adjudicatedResult())
    }

    @Test
    fun pgnTimeControlIsInSeconds() {
        val settings = SettingsManager()
        assertEquals("180+2", settings.exportMetadata(null, TimeControl.of(180_000, 2_000), GameStatus()).timeControl)
        assertEquals("-", settings.exportMetadata(null, null, GameStatus()).timeControl)
    }

    @Test
    fun timeControlCodecRejectsGarbage() {
        val control = TimeControl.of(900_000, 10_000)
        assertEquals(control, SavedGameCodec.decodeTimeControl(SavedGameCodec.encodeTimeControl(control)))
        assertNull(SavedGameCodec.decodeTimeControl("abc"))
        assertNull(SavedGameCodec.decodeTimeControl("0+0"))
        assertNull(SavedGameCodec.decodeTimeControl(null))
        assertNull(SavedGameCodec.remainingTimes(-1, 5))
    }
}
