package com.chesspredictor.presentation.managers

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.TimeControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class GameClock(
    private val scope: CoroutineScope,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val onTimeout: (ChessColor) -> Unit
) {
    private val _timeControl = MutableStateFlow<TimeControl?>(null)
    val timeControl: StateFlow<TimeControl?> = _timeControl.asStateFlow()

    private val _remaining = MutableStateFlow(fullTimes(null))
    val remaining: StateFlow<Map<ChessColor, Long>> = _remaining.asStateFlow()

    private var committed = fullTimes(null)
    private var turn: ChessColor? = null
    private var counting = false
    private var suspended = false
    private var spentBeforePause = Duration.ZERO
    private var runningSince: TimeMark? = null
    private var tickJob: Job? = null

    private val isLimited get() = _timeControl.value != null

    fun setTimeControl(timeControl: TimeControl?) {
        _timeControl.value = timeControl
        reset()
    }

    fun reset() {
        stop()
        committed = fullTimes(_timeControl.value)
        publish()
    }

    fun restore(timeControl: TimeControl?, remaining: Map<ChessColor, Long>?) {
        stop()
        _timeControl.value = timeControl
        committed = if (timeControl != null && remaining != null) fullTimes(timeControl) + remaining else fullTimes(timeControl)
        publish()
    }

    fun startTurn(color: ChessColor, counting: Boolean) {
        stop()
        turn = color
        this.counting = counting
        spentBeforePause = Duration.ZERO
        runningSince = if (suspended) null else timeSource.markNow()
        startTicking()
    }

    fun completeTurn(withIncrement: Boolean = true): Long {
        val color = turn ?: return 0L
        val spent = spentMs()
        if (isLimited && counting) {
            val increment = if (withIncrement) _timeControl.value?.increment ?: 0L else 0L
            committed = committed + (color to (committed.getValue(color) - spent).coerceAtLeast(0) + increment)
        }
        stop()
        return spent
    }

    fun stop() {
        tickJob?.cancel()
        tickJob = null
        turn = null
        runningSince = null
        spentBeforePause = Duration.ZERO
        publish()
    }

    fun pause() {
        suspended = true
        val since = runningSince ?: return
        spentBeforePause += since.elapsedNow()
        runningSince = null
        tickJob?.cancel()
        tickJob = null
        publish()
    }

    fun resume() {
        suspended = false
        if (turn == null || runningSince != null) return
        runningSince = timeSource.markNow()
        startTicking()
    }

    fun budgetFor(color: ChessColor): Long? {
        val timeControl = _timeControl.value ?: return null
        val left = _remaining.value.getValue(color)
        val budget = left / 40 + timeControl.increment * 3 / 4
        return budget.coerceAtMost(left / 2).coerceAtLeast(MIN_BUDGET_MS)
    }

    private fun startTicking() {
        tickJob?.cancel()
        publish()
        if (!isLimited || !counting || runningSince == null) return
        tickJob = scope.launch {
            while (true) {
                publish()
                val color = turn ?: return@launch
                if (_remaining.value.getValue(color) <= 0L) {
                    committed = committed + (color to 0L)
                    turn = null
                    runningSince = null
                    publish()
                    onTimeout(color)
                    return@launch
                }
                delay(TICK_MS)
            }
        }
    }

    private fun spentMs(): Long =
        (spentBeforePause + (runningSince?.elapsedNow() ?: Duration.ZERO)).inWholeMilliseconds

    private fun publish() {
        val color = turn
        _remaining.value = if (color != null && isLimited && counting) {
            committed + (color to (committed.getValue(color) - spentMs()).coerceAtLeast(0))
        } else {
            committed
        }
    }

    private fun fullTimes(timeControl: TimeControl?): Map<ChessColor, Long> {
        val initial = timeControl?.initialTime ?: 0L
        return mapOf(ChessColor.WHITE to initial, ChessColor.BLACK to initial)
    }

    private companion object {
        const val TICK_MS = 100L
        const val MIN_BUDGET_MS = 100L
    }
}
