package com.chesspredictor.domain.usecases

import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.repositories.ChessEngineRepository

class GetBestMoveUseCase(
    private val repository: ChessEngineRepository
) {
    suspend operator fun invoke(
        board: ChessBoard,
        settings: EngineSettings = EngineSettings()
    ): Result<ChessMove> {
        return try {
            val move = repository.analyzeBestMove(board, settings)
            if (move != null) {
                Result.success(move)
            } else {
                Result.failure(NoMovesAvailableException())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class NoMovesAvailableException : Exception("No legal moves available")