package com.chesspredictor.presentation.managers

import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.humanlike.GamePhase
import com.chesspredictor.domain.humanlike.HumanThinkTime
import com.chesspredictor.domain.humanlike.MaiaMoveChooser
import com.chesspredictor.domain.humanlike.MoveKind
import com.chesspredictor.domain.humanlike.StockfishJudge
import com.chesspredictor.domain.humanlike.ThinkCell
import com.chesspredictor.domain.humanlike.ThinkPace
import com.chesspredictor.domain.repositories.ChessEngineRepository
import com.chesspredictor.domain.usecases.ChessRulesEngine
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
        private const val MIN_JUDGE_BUDGET_MS = 3_000L
    }

    private val mistakes = StockfishMistakes(repository)
    private val judge = StockfishJudge(repository)
    private val drawOffers = DrawOfferJudge(repository)

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _error = MutableStateFlow<EngineError?>(null)
    val error: StateFlow<EngineError?> = _error.asStateFlow()

    val shouldOfferDraw: StateFlow<Boolean> = drawOffers.shouldOfferDraw

    fun setReady(ready: Boolean) {
        _isReady.value = ready
    }

    suspend fun awaitReady(timeoutMs: Long = 5000L): Boolean {
        if (_isReady.value) return true
        return withTimeoutOrNull(timeoutMs) {
            _isReady.first { it }
        } != null
    }

    fun clearDrawOffer() = drawOffers.clear()

    fun playsWithoutEngine(settings: EngineSettings): Boolean =
        moveChooser != null && settings.humanStyle && settings.difficulty.playsLikeHuman

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
                if (!_isReady.value && !playsWithoutEngine(settings)) {
                    ChessLogger.error(TAG, "Engine not ready for move request")
                    _error.value = EngineError.NOT_INITIALIZED
                    return@launch
                }

                ChessLogger.info(TAG, "Engine request - Board FEN: ${board.fen}")
                _isThinking.value = true
                _error.value = null

                val started = TimeSource.Monotonic.markNow()
                val context = if (settings.humanStyle && settings.timeLimit > 200) MoveContext.of(board, rules) else null
                var targetMs = context?.let { sampleThink(it, it.expectedKind, pace, maxThinkMs) }
                val searchSettings = targetMs?.let {
                    settings.copy(timeLimit = minOf(settings.timeLimit, maxOf(it.toInt(), MIN_SEARCH_MS)))
                } ?: settings

                val finalMove = chooseHumanMove(history, settings, maxThinkMs) ?: engineMove(board, settings, searchSettings)
                if (finalMove != null) {
                    if (context != null) {
                        val kind = context.kindOf(finalMove)
                        if (kind != context.expectedKind) targetMs = sampleThink(context, kind, pace, maxThinkMs)
                        val waitMs = (targetMs ?: 0L) - started.elapsedNow().inWholeMilliseconds
                        if (waitMs > 0) humanBehaviorManager?.think(waitMs) ?: delay(waitMs)
                    }

                    ensureActive()
                    _isThinking.value = false
                    onMoveReady(finalMove)

                    if (settings.humanStyle && _isReady.value) {
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

    private suspend fun chooseHumanMove(history: List<String>, settings: EngineSettings, maxThinkMs: Long?): ChessMove? {
        if (!playsWithoutEngine(settings)) return null
        val chooser = moveChooser ?: return null
        val elo = settings.difficulty.eloRating
        val choose = suspend { withContext(background) { chooser.choose(history, elo, elo, judgeFor(maxThinkMs)) } }
        return try {
            if (maxThinkMs == null) {
                choose()
            } else {
                withTimeoutOrNull(maxThinkMs) { choose() }
                    ?: null.also { ChessLogger.warning(TAG, "Human move model ran out of time, using the engine") }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ChessLogger.warning(TAG, "Human move model failed, falling back to the engine: ${e.message}")
            null
        }
    }

    private fun judgeFor(maxThinkMs: Long?) =
        judge.takeIf { _isReady.value && (maxThinkMs ?: Long.MAX_VALUE) >= MIN_JUDGE_BUDGET_MS }

    private suspend fun engineMove(board: ChessBoard, settings: EngineSettings, searchSettings: EngineSettings): ChessMove? {
        if (!_isReady.value) {
            _error.value = EngineError.NOT_INITIALIZED
            return null
        }
        val move = repository.analyzeBestMove(board, searchSettings) ?: return null
        return if (settings.humanStyle) mistakes.apply(move, board, searchSettings) else move
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
                val evaluation = repository.analyzePosition(board, settings).evaluation
                humanBehaviorManager.updateEmotionalState(settings.behaviorProfile, board, move, evaluation)
                humanBehaviorManager.generateMoveCommentary(settings.behaviorProfile, board, move, evaluation)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ChessLogger.warning(TAG, "Failed to update human behavior state: ${e.message}")
            }
        }
        drawOffers.evaluate(board, settings)
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
