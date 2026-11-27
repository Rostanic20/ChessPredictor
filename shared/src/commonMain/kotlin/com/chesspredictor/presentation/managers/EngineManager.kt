package com.chesspredictor.presentation.managers

import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.EngineAnalysis
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.humanlike.GamePhase
import com.chesspredictor.domain.entities.EngineDifficulty
import com.chesspredictor.domain.humanlike.HumanThinkTime
import com.chesspredictor.domain.humanlike.MaiaMoveChooser
import com.chesspredictor.domain.humanlike.MoveKind
import com.chesspredictor.domain.humanlike.ThinkCell
import com.chesspredictor.domain.humanlike.ThinkPace
import com.chesspredictor.domain.repositories.ChessEngineRepository
import com.chesspredictor.domain.usecases.ChessRulesEngine
import com.chesspredictor.presentation.ChessConstants
import com.chesspredictor.utils.ChessLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random
import kotlin.time.TimeSource

class EngineManager(
    private val repository: ChessEngineRepository,
    private val scope: CoroutineScope,
    private val humanBehaviorManager: HumanBehaviorManager? = null,
    private val thinkTime: HumanThinkTime = HumanThinkTime(),
    private val rules: ChessRulesEngine = ChessRulesEngine(),
    private val moveChooser: MaiaMoveChooser? = null,
    private val background: CoroutineDispatcher = Dispatchers.Default
) {
    companion object {
        private const val TAG = "EngineManager"
        private const val MIN_SEARCH_MS = 300
        private const val FORCED_MOVE_LIMIT = 5
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
    
    private var moveJob: Job? = null

    fun cancelPendingMove() {
        moveJob?.cancel()
        moveJob = null
        _isThinking.value = false
    }

    fun requestEngineMove(
        board: ChessBoard,
        settings: EngineSettings,
        maxThinkMs: Long? = null,
        pace: ThinkPace = ThinkPace.CASUAL,
        history: List<String> = listOf(board.fen),
        onMoveReady: (ChessMove) -> Unit
    ) {
        moveJob?.cancel()
        val job = scope.launch(start = CoroutineStart.LAZY) {
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

                _isThinking.value = true
                _error.value = null

                val started = TimeSource.Monotonic.markNow()
                val context = if (settings.humanStyle && settings.timeLimit > 200) MoveContext.of(board, rules) else null
                var targetMs = context?.let { sampleThink(it, it.expectedKind, pace, maxThinkMs) }
                val searchSettings = targetMs?.let {
                    settings.copy(timeLimit = minOf(settings.timeLimit, maxOf(it.toInt(), MIN_SEARCH_MS)))
                } ?: settings

                val humanMove = if (context != null) chooseHumanMove(history, settings) else null
                val finalMove = humanMove ?: repository.analyzeBestMove(board, searchSettings)?.let { move ->
                    if (settings.humanStyle) applyHumanMistakes(move, board, searchSettings) else move
                }
                if (finalMove != null) {
                    if (context != null) {
                        val kind = context.kindOf(finalMove)
                        if (kind != context.expectedKind) targetMs = sampleThink(context, kind, pace, maxThinkMs)
                        val waitMs = (targetMs ?: 0L) - started.elapsedNow().inWholeMilliseconds
                        if (waitMs > 0) humanBehaviorManager?.think(waitMs) ?: delay(waitMs)
                    }

                    ChessLogger.info(TAG, "Engine move successful: ${finalMove.from} -> ${finalMove.to}")
                    ensureActive()
                    _isThinking.value = false
                    onMoveReady(finalMove)

                    if (settings.humanStyle) {
                        reactToOwnMove(board, finalMove, settings)
                    }
                } else {
                    ChessLogger.warning(TAG, "Engine returned no move")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = EngineError.ANALYSIS_FAILED(e.message ?: "Unknown error")
                ChessLogger.error(TAG, "Engine move request failed", e)
            } finally {
                if (moveJob === coroutineContext[Job]) {
                    moveJob = null
                    _isThinking.value = false
                }
            }
        }
        moveJob = job
        job.start()
    }
    
    fun clearError() {
        _error.value = null
    }

    private suspend fun chooseHumanMove(history: List<String>, settings: EngineSettings): ChessMove? {
        val chooser = moveChooser ?: return null
        if (settings.difficulty == EngineDifficulty.MAXIMUM) return null
        val elo = settings.difficulty.eloRating
        return try {
            withContext(background) { chooser.choose(history, elo, elo) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ChessLogger.warning(TAG, "Human move model failed, falling back to the engine: ${e.message}")
            null
        }
    }

    private fun sampleThink(context: MoveContext, kind: MoveKind, pace: ThinkPace, maxThinkMs: Long?): Long =
        thinkTime.sampleMs(ThinkCell(context.phase, pace.timeLeft, kind), pace).coerceAtMost(maxThinkMs ?: Long.MAX_VALUE)

    private class MoveContext(val phase: GamePhase, private val forced: Boolean, private val lastCapture: ChessMove?) {
        val expectedKind: MoveKind = when {
            lastCapture != null -> MoveKind.RECAPTURE
            forced -> MoveKind.FORCED
            else -> MoveKind.NORMAL
        }

        fun kindOf(move: ChessMove): MoveKind = when {
            lastCapture != null && move.capturedPiece != null && move.to == lastCapture.to -> MoveKind.RECAPTURE
            forced -> MoveKind.FORCED
            else -> MoveKind.NORMAL
        }

        companion object {
            fun of(board: ChessBoard, rules: ChessRulesEngine): MoveContext {
                val state = rules.withStatus(rules.parseGameState(board.fen))
                val pieces = board.fen.substringBefore(' ').count { it.isLetter() }
                return MoveContext(
                    phase = GamePhase.of(board.moveHistory.size, pieces),
                    forced = state.isCheck || rules.getLegalMoves(state).size <= FORCED_MOVE_LIMIT,
                    lastCapture = board.moveHistory.lastOrNull()?.takeIf { it.capturedPiece != null }
                )
            }
        }
    }

    private suspend fun reactToOwnMove(board: ChessBoard, move: ChessMove, settings: EngineSettings) {
        if (humanBehaviorManager != null) {
            try {
                val positionAnalysis = repository.analyzePosition(board, settings)
                humanBehaviorManager.updateEmotionalState(settings.behaviorProfile, board, move, positionAnalysis.evaluation)
                humanBehaviorManager.generateMoveCommentary(settings.behaviorProfile, board, move, positionAnalysis.evaluation)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ChessLogger.warning(TAG, "Failed to update human behavior state: ${e.message}")
            }
        }
        evaluateDrawOffer(board, settings)
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
        } catch (e: CancellationException) {
            throw e
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
        } catch (e: CancellationException) {
            throw e
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
        } catch (e: CancellationException) {
            throw e
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
        return if (board.turn == ChessColor.WHITE) balance else -balance
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