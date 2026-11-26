package com.chesspredictor.presentation.viewmodels

import com.chesspredictor.data.repositories.GameStateRepository
import com.chesspredictor.data.repositories.SavedGameState
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.presentation.ChessConstants
import com.chesspredictor.presentation.managers.MoveController
import com.chesspredictor.presentation.managers.PlayMode
import com.chesspredictor.presentation.managers.PlayerColor
import com.chesspredictor.presentation.managers.SettingsManager

internal class GamePersistence(
    private val repository: GameStateRepository,
    private val settingsManager: SettingsManager,
    private val moveController: MoveController
) {
    suspend fun save(gameState: GameState) {
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
                startFen = gameState.startingFen()
            )
        )
    }

    suspend fun load(): GameState? {
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

        return gameState
    }

    suspend fun clear() = repository.clearGameState()
}
