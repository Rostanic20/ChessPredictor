package com.chesspredictor.data.repositories

import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.DetailedMove
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.utils.TimeProvider
import kotlinx.coroutines.flow.Flow

interface GameStateRepository {
    suspend fun saveGameState(gameState: SavedGameState)
    suspend fun loadGameState(): SavedGameState?
    suspend fun clearGameState()
    fun observeGameState(): Flow<SavedGameState?>
}

data class SavedGameState(
    val fen: String,
    val moveHistory: List<DetailedMove>,
    val capturedPieces: List<ChessPiece>,
    val playMode: String,
    val playerColor: String,
    val engineSettings: EngineSettings,
    val isFlipped: Boolean,
    val showCoordinates: Boolean,
    val timestamp: Long = TimeProvider.currentTimeMillis(),
    val positionEvaluations: List<Float> = listOf()
)