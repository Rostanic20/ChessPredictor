package com.chesspredictor.presentation.viewmodels

import com.chesspredictor.data.datasources.ChessEngineDataSource
import com.chesspredictor.data.datasources.EngineAnalysis
import com.chesspredictor.data.repositories.GameStateRepository
import com.chesspredictor.data.repositories.SavedGameState
import com.chesspredictor.di.AppModule
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.entities.TimeControl
import com.chesspredictor.presentation.managers.PlayMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ChessBoardViewModelLifecycleTest {

    private class CountingRepository : GameStateRepository {
        var saves = 0
        var last: SavedGameState? = null
        override suspend fun saveGameState(gameState: SavedGameState) {
            saves++
            last = gameState
        }
        override suspend fun loadGameState(): SavedGameState? = last
        override suspend fun clearGameState() { last = null }
        override fun observeGameState(): Flow<SavedGameState?> = flowOf(last)
    }

    private class SilentEngine : ChessEngineDataSource {
        var analyses = 0
        override suspend fun sendCommand(command: String): String = ""
        override suspend fun initialize() {}
        override suspend fun setPosition(fen: String) {}
        override suspend fun analyze(settings: EngineSettings): EngineAnalysis {
            analyses++
            awaitCancellation()
        }
        override suspend fun stop() {}
        override fun getOutput(): Flow<String> = emptyFlow()
    }

    private val engine = SilentEngine()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.humanGame(repository: GameStateRepository): ChessBoardViewModel {
        val viewModel = ChessBoardViewModel(
            providedAppModule = AppModule { engine },
            gameStateRepository = repository,
            background = Dispatchers.Main
        )
        viewModel.initializeBoard()
        viewModel.setPlayMode(PlayMode.VS_HUMAN)
        advanceUntilIdle()
        return viewModel
    }

    private fun TestScope.play(viewModel: ChessBoardViewModel, from: String, to: String) {
        viewModel.onSquareClick(Square.fromString(from))
        viewModel.onSquareClick(Square.fromString(to))
        advanceTimeBy(1_000)
        runCurrent()
    }

    @Test
    fun closeSavesTheGameOnceAndNothingWritesAfterwards() = runTest {
        val repository = CountingRepository()
        val viewModel = humanGame(repository)
        play(viewModel, "e2", "e4")

        viewModel.close()
        advanceUntilIdle()
        val savesAtClose = repository.saves
        assertEquals(1, repository.last?.moveHistory?.size)

        viewModel.saveGameState()
        viewModel.setPlayMode(PlayMode.VS_ENGINE)
        advanceUntilIdle()

        assertEquals(savesAtClose, repository.saves)
        assertEquals(0, engine.analyses)
    }

    @Test
    fun theClockIsLockedWhileAGameIsInProgress() = runTest {
        val viewModel = humanGame(CountingRepository())
        val chosen = TimeControl.PRESETS.first()
        viewModel.setTimeControl(chosen)
        play(viewModel, "e2", "e4")

        viewModel.setTimeControl(TimeControl.PRESETS.last())
        viewModel.setTimeControl(null)

        assertEquals(chosen, viewModel.currentTimeControl.value)
        viewModel.close()
        advanceUntilIdle()
    }
}
