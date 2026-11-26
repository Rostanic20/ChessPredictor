package com.chesspredictor.presentation.managers

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.TimeControl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class GameClockTest {

    private val oneMinutePlusTwo = TimeControl.of(60_000, 2_000)

    private fun TestScope.clock(onTimeout: (ChessColor) -> Unit = {}) =
        GameClock(backgroundScope, testScheduler.timeSource, onTimeout)

    @Test
    fun countedTurnLosesItsTimeAndGainsTheIncrement() = runTest {
        val clock = clock()
        clock.setTimeControl(oneMinutePlusTwo)

        clock.startTurn(ChessColor.WHITE, counting = true)
        advanceTimeBy(5_000)

        assertEquals(5_000, clock.completeTurn())
        assertEquals(57_000, clock.remaining.value[ChessColor.WHITE])
        assertEquals(60_000, clock.remaining.value[ChessColor.BLACK])
    }

    @Test
    fun aTakebackStillChargesTheTimeUsedButGivesNoIncrement() = runTest {
        val clock = clock()
        clock.setTimeControl(oneMinutePlusTwo)

        clock.startTurn(ChessColor.WHITE, counting = true)
        advanceTimeBy(5_000)
        clock.completeTurn(withIncrement = false)

        assertEquals(55_000, clock.remaining.value[ChessColor.WHITE])
    }

    @Test
    fun firstMoveIsMeasuredButNotCharged() = runTest {
        val clock = clock()
        clock.setTimeControl(oneMinutePlusTwo)

        clock.startTurn(ChessColor.WHITE, counting = false)
        advanceTimeBy(5_000)

        assertEquals(5_000, clock.completeTurn())
        assertEquals(60_000, clock.remaining.value[ChessColor.WHITE])
    }

    @Test
    fun runningOutOfTimeReportsTheSideToMove() = runTest {
        var flagged: ChessColor? = null
        val clock = clock { flagged = it }
        clock.setTimeControl(TimeControl.of(3_000, 0))

        clock.startTurn(ChessColor.BLACK, counting = true)
        advanceTimeBy(2_900)
        assertNull(flagged)
        advanceTimeBy(300)

        assertEquals(ChessColor.BLACK, flagged)
        assertEquals(0, clock.remaining.value[ChessColor.BLACK])
    }

    @Test
    fun pausedTimeIsNotCharged() = runTest {
        var flagged: ChessColor? = null
        val clock = clock { flagged = it }
        clock.setTimeControl(TimeControl.of(3_000, 0))

        clock.startTurn(ChessColor.WHITE, counting = true)
        advanceTimeBy(1_000)
        clock.pause()
        advanceTimeBy(60_000)
        clock.resume()
        advanceTimeBy(1_000)

        assertNull(flagged)
        assertEquals(2_000, clock.completeTurn())
        assertEquals(1_000, clock.remaining.value[ChessColor.WHITE])
    }

    @Test
    fun aTurnStartedWhileBackgroundedWaitsForResume() = runTest {
        val clock = clock()
        clock.setTimeControl(oneMinutePlusTwo)
        clock.pause()

        clock.startTurn(ChessColor.BLACK, counting = true)
        advanceTimeBy(10_000)
        clock.resume()
        advanceTimeBy(1_000)

        assertEquals(1_000, clock.completeTurn())
    }

    @Test
    fun withoutAClockNobodyRunsOutOfTime() = runTest {
        var flagged: ChessColor? = null
        val clock = clock { flagged = it }

        clock.startTurn(ChessColor.WHITE, counting = true)
        advanceTimeBy(3_600_000)
        runCurrent()

        assertNull(flagged)
        assertNull(clock.budgetFor(ChessColor.WHITE))
    }

    @Test
    fun restoreKeepsTheSavedTimes() = runTest {
        val clock = clock()

        clock.restore(oneMinutePlusTwo, mapOf(ChessColor.WHITE to 1_234L, ChessColor.BLACK to 5_678L))

        assertEquals(oneMinutePlusTwo, clock.timeControl.value)
        assertEquals(mapOf(ChessColor.WHITE to 1_234L, ChessColor.BLACK to 5_678L), clock.remaining.value)
    }

    @Test
    fun engineBudgetShrinksWithItsClock() = runTest {
        val clock = clock()
        clock.restore(TimeControl.of(300_000, 0), mapOf(ChessColor.WHITE to 300_000L, ChessColor.BLACK to 2_000L))

        assertEquals(7_500, clock.budgetFor(ChessColor.WHITE))
        assertEquals(100, clock.budgetFor(ChessColor.BLACK))
    }
}
