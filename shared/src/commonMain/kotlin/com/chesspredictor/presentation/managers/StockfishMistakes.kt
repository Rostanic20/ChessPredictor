package com.chesspredictor.presentation.managers

import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.repositories.ChessEngineRepository
import com.chesspredictor.presentation.ChessConstants
import com.chesspredictor.utils.ChessLogger
import kotlinx.coroutines.CancellationException
import kotlin.random.Random

class StockfishMistakes(private val repository: ChessEngineRepository) {

    suspend fun apply(bestMove: ChessMove, board: ChessBoard, settings: EngineSettings): ChessMove {
        val random = Random.nextFloat()
        val complexity = estimatePositionComplexity(board)
        val blunderRate = settings.blunderProbability * (1f + complexity * BLUNDER_COMPLEXITY_FACTOR)
        val inaccuracyRate = settings.inaccuracyRate * (1f + complexity * INACCURACY_COMPLEXITY_FACTOR)

        return when {
            random < blunderRate -> weakerMove(board, settings, skillDrop = 8, minDepth = 3, depthDrop = 4, timeShare = 1f / 3)
            random < blunderRate + inaccuracyRate -> weakerMove(board, settings, skillDrop = 3, minDepth = 4, depthDrop = 2, timeShare = 0.7f)
            else -> null
        } ?: bestMove
    }

    private fun estimatePositionComplexity(board: ChessBoard): Float {
        val pieceCount = board.fen.substringBefore(' ').count { it.isLetter() }
        val gameLength = board.moveHistory.size

        return when {
            pieceCount < ChessConstants.PieceThresholds.MIDDLEGAME -> 0.2f + (gameLength / 80f).coerceAtMost(0.3f)
            pieceCount > ChessConstants.PieceThresholds.OPENING -> 0.8f + Random.nextFloat() * 0.2f
            else -> 0.4f + Random.nextFloat() * 0.4f
        }
    }

    private suspend fun weakerMove(
        board: ChessBoard,
        settings: EngineSettings,
        skillDrop: Int,
        minDepth: Int,
        depthDrop: Int,
        timeShare: Float
    ): ChessMove? {
        val weaker = settings.copy(
            skillLevel = maxOf(0, settings.skillLevel - skillDrop),
            analysisDepth = maxOf(minDepth, settings.analysisDepth - depthDrop),
            timeLimit = (settings.timeLimit * timeShare).toInt()
        )
        return try {
            repository.analyzeBestMove(board, weaker)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ChessLogger.warning(TAG, "Failed to get a weaker move: ${e.message}")
            null
        }
    }

    private companion object {
        const val TAG = "StockfishMistakes"
        const val BLUNDER_COMPLEXITY_FACTOR = 0.3f
        const val INACCURACY_COMPLEXITY_FACTOR = 0.2f
    }
}
