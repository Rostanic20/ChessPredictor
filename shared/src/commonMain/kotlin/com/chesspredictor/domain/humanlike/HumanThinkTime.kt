package com.chesspredictor.domain.humanlike

import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random

enum class GamePhase {
    OPENING, MIDDLEGAME, ENDGAME;

    companion object {
        fun of(ply: Int, pieceCount: Int): GamePhase = when {
            ply < 20 -> OPENING
            pieceCount <= 12 -> ENDGAME
            else -> MIDDLEGAME
        }
    }
}

enum class TimeLeft {
    PLENTY, MOST, HALF, LOW, CRITICAL;

    companion object {
        fun of(fraction: Double): TimeLeft = when {
            fraction < 0.10 -> CRITICAL
            fraction < 0.25 -> LOW
            fraction < 0.50 -> HALF
            fraction < 0.75 -> MOST
            else -> PLENTY
        }
    }
}

enum class MoveKind { NORMAL, FORCED, RECAPTURE }

data class ThinkCell(val phase: GamePhase, val timeLeft: TimeLeft, val kind: MoveKind)

data class ThinkPace(val budgetSeconds: Double, val timeLeft: TimeLeft) {
    companion object {
        val CASUAL = ThinkPace(budgetSeconds = 7.5, timeLeft = TimeLeft.PLENTY)

        fun of(initialMs: Long, incrementMs: Long, remainingMs: Long): ThinkPace = ThinkPace(
            budgetSeconds = (initialMs + 40 * incrementMs) / 40_000.0,
            timeLeft = TimeLeft.of(remainingMs.toDouble() / initialMs)
        )
    }
}

class HumanThinkTime(private val random: Random = Random.Default) {

    fun sampleMs(cell: ThinkCell, pace: ThinkPace): Long {
        val quantiles = quantilesFor(cell)
        val position = (random.nextDouble() * (quantiles.size + 1) - 0.5).coerceIn(0.0, quantiles.size - 1.0)
        val low = position.toInt()
        val high = minOf(low + 1, quantiles.size - 1)
        val logSeconds = quantiles[low] + (quantiles[high] - quantiles[low]) * (position - low)
        val scale = ThinkTimeTable.ALPHA * ln(pace.budgetSeconds / ThinkTimeTable.REFERENCE_BUDGET_SECONDS)
        return (exp(logSeconds + scale) * 1000).toLong()
    }

    private fun quantilesFor(cell: ThinkCell): DoubleArray =
        ThinkTimeTable.logSecondQuantiles[cell]
            ?: TimeLeft.entries.sortedBy { kotlin.math.abs(it.ordinal - cell.timeLeft.ordinal) }
                .firstNotNullOfOrNull { ThinkTimeTable.logSecondQuantiles[cell.copy(timeLeft = it)] }
            ?: ThinkTimeTable.logSecondQuantiles.getValue(ThinkCell(GamePhase.MIDDLEGAME, cell.timeLeft, MoveKind.NORMAL))
}
