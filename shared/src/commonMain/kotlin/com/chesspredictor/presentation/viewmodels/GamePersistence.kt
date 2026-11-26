package com.chesspredictor.presentation.viewmodels

import com.chesspredictor.data.repositories.GameStateRepository
import com.chesspredictor.data.repositories.SavedGameState
import com.chesspredictor.domain.entities.GameEnding
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.presentation.ChessConstants
import com.chesspredictor.presentation.managers.GameClock
import com.chesspredictor.presentation.managers.MoveController
import com.chesspredictor.presentation.managers.PlayMode
import com.chesspredictor.presentation.managers.PlayerColor
import com.chesspredictor.presentation.managers.SettingsManager

internal class GamePersistence(
    private val repository: GameStateRepository,
    private val settingsManager: SettingsManager,
    private val moveController: MoveController,
    private val clock: GameClock
) {
    suspend fun save(gameState: GameState, ending: GameEnding?) {
        val timeControl = clock.timeControl.value
        repository.saveGameState(
            SavedGameState(
                fen = gameState.toFen(),
                moveHistory = gameState.moveHistory,
                capturedPieces = gameState.capturedPieces,
                playMode = settingsManager.playMode.value.name,
                playerColor = settingsManager.playerColor.value.name,
                engineSettings = settingsManager.engineSettings.value,
                isFlipped = settingsManager.isFlipped.value,
                showCoordinates = settingsManager.showCoordinates.value,
                positionEvaluations = emptyList(),
                startFen = gameState.startingFen(),
                timeControl = timeControl,
                remainingTimes = if (timeControl != null) clock.remaining.value else null,
                ending = ending
            )
        )
    }

    suspend fun load(): RestoredGame? {
        val savedState = repository.loadGameState() ?: return null
        val restored = moveController.restoreGame(
            startFen = savedState.startFen ?: ChessConstants.STARTING_POSITION_FEN,
            moves = savedState.moveHistory,
            finalFen = savedState.fen
        )
        val gameState = if (restored.moveHistory.size == savedState.moveHistory.size) {
            restored
        } else {
            restored.copy(capturedPieces = savedState.capturedPieces)
        }

        settingsManager.setFlipped(savedState.isFlipped)
        settingsManager.setShowCoordinates(savedState.showCoordinates)
        settingsManager.setPlayMode(PlayMode.valueOf(savedState.playMode))
        settingsManager.setPlayerColor(PlayerColor.valueOf(savedState.playerColor))
        val engineSettings = savedState.engineSettings
        settingsManager.setEngineSettings(engineSettings.withDifficulty(engineSettings.difficulty))
        clock.restore(savedState.timeControl, savedState.remainingTimes)

        val fullyReplayed = restored.moveHistory.size == savedState.moveHistory.size
        return RestoredGame(gameState, savedState.ending.takeIf { fullyReplayed })
    }

    suspend fun clear() = repository.clearGameState()
}

internal data class RestoredGame(
    val gameState: GameState,
    val ending: GameEnding?
)
