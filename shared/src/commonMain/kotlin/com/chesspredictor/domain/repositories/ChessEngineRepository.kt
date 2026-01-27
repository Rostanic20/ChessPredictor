package com.chesspredictor.domain.repositories

import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.entities.EngineAnalysis
import kotlinx.coroutines.flow.Flow

interface ChessEngineRepository {
    suspend fun analyzeBestMove(board: ChessBoard, settings: EngineSettings): ChessMove?
    suspend fun analyzePosition(board: ChessBoard, settings: EngineSettings): PositionAnalysis
    suspend fun analyzeWithEvaluation(board: ChessBoard, settings: EngineSettings): EngineAnalysis?
    suspend fun validateMove(board: ChessBoard, move: ChessMove): Boolean
    suspend fun getAvailableMoves(board: ChessBoard): List<ChessMove>
    fun observeEvaluation(): Flow<Float>
}

data class PositionAnalysis(
    val bestMove: ChessMove?,
    val evaluation: Float,
    val mate: Int? = null, // moves to mate, null if not mate
    val principalVariation: List<ChessMove> = emptyList()
)