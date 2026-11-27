package com.chesspredictor.domain.humanlike

import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HumanThinkTimeTest {
    private val blitz = ThinkPace(budgetSeconds = 4.5, timeLeft = TimeLeft.PLENTY)

    private fun medianMs(cell: ThinkCell, pace: ThinkPace, seed: Int = 3): Long {
        val sampler = HumanThinkTime(Random(seed))
        return List(4_001) { sampler.sampleMs(cell, pace) }.sorted()[2_000]
    }

    @Test
    fun samplingReproducesTheFittedMedian() {
        val cell = ThinkCell(GamePhase.MIDDLEGAME, TimeLeft.MOST, MoveKind.NORMAL)
        val quantiles = ThinkTimeTable.logSecondQuantiles.getValue(cell)
        val fitted = exp(quantiles[quantiles.size / 2]) * 1000
        val sampled = medianMs(cell, blitz)
        assertTrue(kotlin.math.abs(sampled - fitted) / fitted < 0.1, "sampled $sampled ms, fitted $fitted ms")
    }

    @Test
    fun recapturesAndTimeTroubleAreFaster() {
        val normal = medianMs(ThinkCell(GamePhase.MIDDLEGAME, TimeLeft.MOST, MoveKind.NORMAL), blitz)
        val recapture = medianMs(ThinkCell(GamePhase.MIDDLEGAME, TimeLeft.MOST, MoveKind.RECAPTURE), blitz)
        val critical = medianMs(ThinkCell(GamePhase.MIDDLEGAME, TimeLeft.CRITICAL, MoveKind.NORMAL), blitz)
        assertTrue(recapture < normal * 0.7, "recapture $recapture vs normal $normal")
        assertTrue(critical < normal * 0.6, "critical $critical vs normal $normal")
    }

    @Test
    fun longerTimeControlsThinkLongerButNotProportionally() {
        val cell = ThinkCell(GamePhase.MIDDLEGAME, TimeLeft.MOST, MoveKind.NORMAL)
        val threeMinutes = medianMs(cell, blitz)
        val tenMinutes = medianMs(cell, ThinkPace(budgetSeconds = 15.0, timeLeft = TimeLeft.MOST))
        val expectedRatio = exp(ThinkTimeTable.ALPHA * ln(15.0 / 4.5))
        assertEquals(expectedRatio, tenMinutes.toDouble() / threeMinutes, 0.15 * expectedRatio)
        assertTrue(tenMinutes.toDouble() / threeMinutes < 15.0 / 4.5)
    }

    @Test
    fun everyCellCanBeSampled() {
        val sampler = HumanThinkTime(Random(1))
        for (phase in GamePhase.entries) for (left in TimeLeft.entries) for (kind in MoveKind.entries) {
            val ms = sampler.sampleMs(ThinkCell(phase, left, kind), blitz)
            assertTrue(ms in 1..600_000, "$phase $left $kind gave $ms")
        }
    }

    @Test
    fun paceFollowsTheClock() {
        val pace = ThinkPace.of(initialMs = 180_000, incrementMs = 2_000, remainingMs = 30_000)
        assertEquals(TimeLeft.LOW, pace.timeLeft)
        assertEquals((180 + 80) / 40.0, pace.budgetSeconds, 1e-9)
        assertEquals(TimeLeft.CRITICAL, ThinkPace.of(180_000, 0, 10_000).timeLeft)
    }
}
