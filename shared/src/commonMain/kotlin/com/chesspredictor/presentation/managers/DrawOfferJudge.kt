package com.chesspredictor.presentation.managers

import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.repositories.ChessEngineRepository
import com.chesspredictor.presentation.ChessConstants
import com.chesspredictor.utils.ChessLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class DrawOfferJudge(private val repository: ChessEngineRepository) {

    private val _shouldOfferDraw = MutableStateFlow(false)
    val shouldOfferDraw: StateFlow<Boolean> = _shouldOfferDraw.asStateFlow()

    private var lastEvaluation: Float? = null
    private val evaluationHistory = mutableListOf<Float>()
    private var movesSinceLastOffer = 0

    fun clear() {
        _shouldOfferDraw.value = false
        movesSinceLastOffer = 0
    }

    suspend fun evaluate(board: ChessBoard, settings: EngineSettings) {
        movesSinceLastOffer++
        if (movesSinceLastOffer < MIN_MOVES_BETWEEN_OFFERS) return

        val previous = lastEvaluation
        try {
            val current = repository.analyzePosition(board, settings).evaluation
            evaluationHistory.add(current)
            if (evaluationHistory.size > EVAL_HISTORY_MAX_SIZE) evaluationHistory.removeAt(0)

            val losing = when {
                current <= -3.0f && evaluationHistory.size >= 3 -> {
                    val recent = evaluationHistory.takeLast(3)
                    recent.all { it <= -2.5f } && recent.first() > recent.last()
                }
                isEndgame(board) && current <= -2.0f -> materialBalance(board) <= -5
                evaluationHistory.size >= 6 -> evaluationHistory.takeLast(6).all { it <= -1.5f }
                previous != null && previous > -1.0f && current <= -2.5f -> Random.nextFloat() < 0.3f
                else -> false
            }

            if (losing && Random.nextFloat() < offerChance(settings.difficulty.eloRating)) {
                ChessLogger.info(TAG, "Offering a draw (eval: $current, material: ${materialBalance(board)})")
                _shouldOfferDraw.value = true
                movesSinceLastOffer = 0
            }
            lastEvaluation = current
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ChessLogger.warning(TAG, "Failed to evaluate draw offer: ${e.message}")
        }
    }

    private fun offerChance(elo: Int): Float = when {
        elo < 1200 -> 0.8f
        elo < 1800 -> 0.6f
        else -> 0.4f
    }

    private fun isEndgame(board: ChessBoard): Boolean =
        board.fen.substringBefore(' ').count { it.isLetter() } < ChessConstants.PieceThresholds.ENDGAME

    private fun materialBalance(board: ChessBoard): Int {
        val balance = board.fen.substringBefore(' ').sumOf { piece ->
            val value = when (piece.lowercaseChar()) {
                'p' -> 1
                'n', 'b' -> 3
                'r' -> 5
                'q' -> 9
                else -> 0
            }
            if (piece.isUpperCase()) value else -value
        }
        return if (board.turn == ChessColor.WHITE) balance else -balance
    }

    private companion object {
        const val TAG = "DrawOfferJudge"
        const val MIN_MOVES_BETWEEN_OFFERS = 8
        const val EVAL_HISTORY_MAX_SIZE = 10
    }
}
