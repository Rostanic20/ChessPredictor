package com.chesspredictor.presentation.managers

import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.EngineAnalysis
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.entities.GameTimeControl
import com.chesspredictor.domain.repositories.ChessEngineRepository
import com.chesspredictor.presentation.ChessConstants
import com.chesspredictor.utils.ChessLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

class EngineManager(
    private val repository: ChessEngineRepository,
    private val scope: CoroutineScope,
    private val humanBehaviorManager: HumanBehaviorManager? = null
) {
    companion object {
        private const val TAG = "EngineManager"
        private const val MIN_REACTION_TIME_MS = 100
        private const val MAX_REACTION_TIME_MS = 200

        private const val ENDGAME_PIECE_THRESHOLD = ChessConstants.PieceThresholds.ENDGAME
        private const val OPENING_PIECE_THRESHOLD = ChessConstants.PieceThresholds.OPENING
        private const val MIDDLEGAME_PIECE_THRESHOLD = ChessConstants.PieceThresholds.MIDDLEGAME

        private const val DRAW_OFFER_MIN_MOVES = 8
        private const val EVAL_HISTORY_MAX_SIZE = 10

        private const val BLUNDER_COMPLEXITY_FACTOR = 0.3f
        private const val INACCURACY_COMPLEXITY_FACTOR = 0.2f

        private const val PAWN_VALUE = 1
        private const val KNIGHT_VALUE = 3
        private const val BISHOP_VALUE = 3
        private const val ROOK_VALUE = 5
        private const val QUEEN_VALUE = 9
    }
    
    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()
    
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    
    private val _lastAnalysis = MutableStateFlow<EngineAnalysis?>(null)
    val lastAnalysis: StateFlow<EngineAnalysis?> = _lastAnalysis.asStateFlow()
    
    private val _error = MutableStateFlow<EngineError?>(null)
    val error: StateFlow<EngineError?> = _error.asStateFlow()
    
    private val _shouldOfferDraw = MutableStateFlow(false)
    val shouldOfferDraw: StateFlow<Boolean> = _shouldOfferDraw.asStateFlow()
    
    private var lastEvaluation: Float? = null
    private var evaluationHistory = mutableListOf<Float>()
    private var movesSinceLastDrawOffer = 0
    
    fun setReady(ready: Boolean) {
        _isReady.value = ready
    }

    suspend fun awaitReady(timeoutMs: Long = 5000L): Boolean {
        if (_isReady.value) return true
        return withTimeoutOrNull(timeoutMs) {
            _isReady.first { it }
        } != null
    }
    
    fun clearDrawOffer() {
        _shouldOfferDraw.value = false
        movesSinceLastDrawOffer = 0
    }
    
    private var requestBoardState: ChessBoard? = null
    
    fun requestEngineMove(
        board: ChessBoard,
        settings: EngineSettings,
        onMoveReady: (ChessMove) -> Unit
    ) {
        scope.launch {
            try {
                if (!_isReady.value) {
                    ChessLogger.error(TAG, "Engine not ready for move request - initializing...")
                    _error.value = EngineError.NOT_INITIALIZED
                    try {
                        ChessLogger.info(TAG, "Engine initialization would be triggered here")
                    } catch (e: Exception) {
                        ChessLogger.error(TAG, "Failed to initialize engine", e)
                    }
                    return@launch
                }
                
                ChessLogger.info(TAG, "Starting engine move request")
                ChessLogger.info(TAG, "Engine request - Board FEN: ${board.fen}")
                ChessLogger.info(TAG, "Engine request - Board turn: ${board.turn}")
                
                val finalSettings = if (settings.timeLimit <= 200) {
                    settings
                } else if (settings.humanStyle && humanBehaviorManager != null) {
                    val timeControl = GameTimeControl.fromMoveTime(settings.timeLimit)
                    val humanThinkingTime = humanBehaviorManager.calculateDisplayThinkingTime(
                        settings.behaviorProfile,
                        board,
                        settings.timeLimit.toLong(),
                        timeControl
                    ).toInt()

                    humanBehaviorManager.simulateThinkingProcess(settings.behaviorProfile, humanThinkingTime.toLong())

                    settings.copy(timeLimit = humanThinkingTime)
                } else {
                    settings
                }
                
                _isThinking.value = true
                _error.value = null

                requestBoardState = board

                val move = repository.analyzeBestMove(board, finalSettings)
                if (move != null) {
                    val finalMove = if (settings.humanStyle) {
                        applyHumanMistakes(move, board, settings)
                    } else {
                        move
                    }

                    if (settings.humanStyle && humanBehaviorManager != null) {
                        try {
                            val positionAnalysis = repository.analyzePosition(board, settings)

                            humanBehaviorManager.updateEmotionalState(
                                settings.behaviorProfile,
                                board,
                                finalMove,
                                positionAnalysis.evaluation
                            )

                            humanBehaviorManager.generateMoveCommentary(
                                settings.behaviorProfile,
                                board,
                                finalMove,
                                positionAnalysis.evaluation
                            )
                        } catch (e: Exception) {
                            ChessLogger.warning(TAG, "Failed to update human behavior state: ${e.message}")
                        }
                    }

                    if (settings.humanStyle) {
                        evaluateDrawOffer(board, settings)
                    }
                    
                    ChessLogger.info(TAG, "Engine move successful: ${finalMove.from} -> ${finalMove.to}")
                    onMoveReady(finalMove)
                } else {
                    ChessLogger.warning(TAG, "Engine returned no move")
                }
                
                _isThinking.value = false
            } catch (e: Exception) {
                _isThinking.value = false
                _error.value = EngineError.ANALYSIS_FAILED(e.message ?: "Unknown error")
                ChessLogger.error(TAG, "Engine move request failed", e)
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }

    private suspend fun applyHumanMistakes(bestMove: ChessMove, board: ChessBoard, settings: EngineSettings): ChessMove {
        val random = Random.nextFloat()
        val complexity = estimatePositionComplexity(board)

        val adjustedBlunderRate = settings.blunderProbability * (1f + complexity * BLUNDER_COMPLEXITY_FACTOR)
        val adjustedInaccuracyRate = settings.inaccuracyRate * (1f + complexity * INACCURACY_COMPLEXITY_FACTOR)
        
        when {
            random < adjustedBlunderRate -> {
                return requestWeakerMove(board, settings) ?: bestMove
            }
            random < adjustedBlunderRate + adjustedInaccuracyRate -> {
                return requestSuboptimalMove(board, settings) ?: bestMove
            }
            else -> {
                return bestMove
            }
        }
    }

    private fun estimatePositionComplexity(board: ChessBoard): Float {
        val pieceCount = board.fen.count { it.isLetter() }
        val gameLength = board.moveHistory.size

        return when {
            pieceCount < MIDDLEGAME_PIECE_THRESHOLD -> 0.2f + (gameLength / 80f).coerceAtMost(0.3f)
            pieceCount > OPENING_PIECE_THRESHOLD -> 0.8f + Random.nextFloat() * 0.2f
            else -> 0.4f + Random.nextFloat() * 0.4f
        }
    }

    private fun isEndgamePosition(board: ChessBoard): Boolean {
        return board.fen.count { it.isLetter() } < ENDGAME_PIECE_THRESHOLD
    }

    private suspend fun requestWeakerMove(board: ChessBoard, settings: EngineSettings): ChessMove? {
        val blunderSettings = settings.copy(
            skillLevel = maxOf(0, settings.skillLevel - 8),
            analysisDepth = maxOf(3, settings.analysisDepth - 4),
            timeLimit = settings.timeLimit / 3
        )
        
        return try {
            repository.analyzeBestMove(board, blunderSettings)
        } catch (e: Exception) {
            ChessLogger.warning(TAG, "Failed to get blunder move: ${e.message}")
            null
        }
    }

    private suspend fun requestSuboptimalMove(board: ChessBoard, settings: EngineSettings): ChessMove? {
        val inaccurateSettings = settings.copy(
            skillLevel = maxOf(0, settings.skillLevel - 3),
            analysisDepth = maxOf(4, settings.analysisDepth - 2),
            timeLimit = (settings.timeLimit * 0.7f).toInt()
        )
        
        return try {
            repository.analyzeBestMove(board, inaccurateSettings)
        } catch (e: Exception) {
            ChessLogger.warning(TAG, "Failed to get inaccurate move: ${e.message}")
            null
        }
    }

    private suspend fun evaluateDrawOffer(board: ChessBoard, settings: EngineSettings) {
        movesSinceLastDrawOffer++

        if (movesSinceLastDrawOffer < DRAW_OFFER_MIN_MOVES) return

        val lastEval = lastEvaluation

        try {
            val positionAnalysis = repository.analyzePosition(board, settings)
            val currentEval = positionAnalysis.evaluation

            evaluationHistory.add(currentEval)
            if (evaluationHistory.size > EVAL_HISTORY_MAX_SIZE) {
                evaluationHistory.removeAt(0)
            }

            val shouldOffer = when {
                currentEval <= -3.0f && evaluationHistory.size >= 3 -> {
                    val recentEvals = evaluationHistory.takeLast(3)
                    recentEvals.all { it <= -2.5f } && recentEvals.first() > recentEvals.last()
                }

                isEndgamePosition(board) && currentEval <= -2.0f -> {
                    val materialBalance = calculateMaterialBalance(board)
                    materialBalance <= -5
                }

                evaluationHistory.size >= 6 -> {
                    evaluationHistory.takeLast(6).all { it <= -1.5f }
                }

                lastEval != null && lastEval > -1.0f && currentEval <= -2.5f -> {
                    Random.nextFloat() < 0.3f
                }

                else -> false
            }

            if (shouldOffer) {
                val eloFactor = when {
                    settings.difficulty.eloRating < 1200 -> 0.8f
                    settings.difficulty.eloRating < 1800 -> 0.6f
                    else -> 0.4f
                }
                
                if (Random.nextFloat() < eloFactor) {
                    ChessLogger.info(TAG, "Human-style: Offering draw (eval: $currentEval, material: ${calculateMaterialBalance(board)})")
                    _shouldOfferDraw.value = true
                    movesSinceLastDrawOffer = 0
                }
            }
            
            lastEvaluation = currentEval
            
        } catch (e: Exception) {
            ChessLogger.warning(TAG, "Failed to evaluate draw offer: ${e.message}")
        }
    }
    
    private fun calculateMaterialBalance(board: ChessBoard): Int {
        var balance = 0
        val boardPart = board.fen.substringBefore(' ')
        for (char in boardPart) {
            val value = when (char) {
                'P' -> PAWN_VALUE
                'N' -> KNIGHT_VALUE
                'B' -> BISHOP_VALUE
                'R' -> ROOK_VALUE
                'Q' -> QUEEN_VALUE
                'p' -> -PAWN_VALUE
                'n' -> -KNIGHT_VALUE
                'b' -> -BISHOP_VALUE
                'r' -> -ROOK_VALUE
                'q' -> -QUEEN_VALUE
                else -> 0
            }
            balance += value
        }
        return if (board.turn == ChessColor.WHITE) -balance else balance
    }
}

sealed class EngineError {
    object NOT_INITIALIZED : EngineError()
    object INVALID_POSITION : EngineError()
    object INVALID_SETTINGS : EngineError()
    data class ANALYSIS_FAILED(val message: String) : EngineError()
    
    fun errorMessage(): String = when (this) {
        NOT_INITIALIZED -> "Engine not initialized"
        INVALID_POSITION -> "Invalid chess position"
        INVALID_SETTINGS -> "Invalid engine settings"
        is ANALYSIS_FAILED -> "Analysis failed: $message"
    }
}