package com.chesspredictor.domain.humanlike

import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.usecases.ChessRulesEngine
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaiaMoveChooserTest {
    private val afterE4 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"

    private fun predictor(vararg favourite: Pair<String, Float>) = HumanMovePredictor { fens, _, _ ->
        FloatArray(MaiaEncoding.MOVE_COUNT).also { logits ->
            favourite.forEach { (uci, value) -> logits[MaiaEncoding.moveIndex(uci, MaiaEncoding.blackToMove(fens.last()))] = value }
        }
    }

    @Test
    fun theFavouredLegalMoveIsPlayed() = runTest {
        val chooser = MaiaMoveChooser(predictor("c7c5" to 40f), random = Random(1))
        assertEquals("c7c5", chooser.choose(listOf(afterE4), 1500, 1500)?.toUci())
    }

    @Test
    fun anIllegalMoveIsNeverPlayedHoweverLikely() = runTest {
        val chooser = MaiaMoveChooser(predictor("e7e4" to 100f, "d7d5" to 30f), random = Random(1))
        repeat(50) { assertEquals("d7d5", chooser.choose(listOf(afterE4), 1500, 1500)?.toUci()) }
    }

    private class AlwaysFirst : Random() {
        override fun nextBits(bitCount: Int) = 0
        override fun nextDouble() = 0.0
    }

    private val rules = ChessRulesEngine()
    private val start = rules.parseGameState(afterE4)
    private val legal = rules.getLegalMoves(start)
    private fun after(move: ChessMove) = requireNotNull(rules.makeMove(start, move)).toFen()

    private class RecordingJudge(private val blunders: Set<String>, private val fail: Boolean = false) : MoveJudge {
        val judged = mutableListOf<String>()
        override suspend fun winChanceLoss(before: GameState, after: GameState): Double {
            judged += after.toFen()
            if (fail) error("engine gone")
            return if (after.toFen() in blunders) 0.4 else 0.01
        }
    }

    @Test
    fun anUnlikelyBlunderIsReplacedByAnotherSample() = runTest {
        val (first, second) = legal
        val judge = RecordingJudge(blunders = setOf(after(first)))
        val chooser = MaiaMoveChooser(predictor("e7e5" to 6f), random = AlwaysFirst())

        val played = chooser.choose(listOf(afterE4), 1500, 1500, judge)

        assertEquals(second.toUci(), played?.toUci())
        assertEquals(listOf(after(first), after(second)), judge.judged)
    }

    @Test
    fun anUnlikelyMoveThatHoldsIsKept() = runTest {
        val first = legal.first()
        val judge = RecordingJudge(blunders = emptySet())
        val chooser = MaiaMoveChooser(predictor("e7e5" to 6f), random = AlwaysFirst())

        assertEquals(first.toUci(), chooser.choose(listOf(afterE4), 1500, 1500, judge)?.toUci())
        assertEquals(1, judge.judged.size)
    }

    @Test
    fun aLikelyMoveIsPlayedWithoutAskingTheJudge() = runTest {
        val judge = RecordingJudge(blunders = legal.map(::after).toSet())
        val chooser = MaiaMoveChooser(predictor("e7e5" to 40f), random = Random(3))

        repeat(20) { assertEquals("e7e5", chooser.choose(listOf(afterE4), 1500, 1500, judge)?.toUci()) }
        assertEquals(emptyList(), judge.judged)
    }

    @Test
    fun aFailingJudgeKeepsTheSampledMove() = runTest {
        val first = legal.first()
        val judge = RecordingJudge(blunders = emptySet(), fail = true)
        val chooser = MaiaMoveChooser(predictor("e7e5" to 6f), random = AlwaysFirst())

        assertEquals(first.toUci(), chooser.choose(listOf(afterE4), 1500, 1500, judge)?.toUci())
    }

    @Test
    fun movesAreSampledInProportionToTheirProbability() = runTest {
        val chooser = MaiaMoveChooser(predictor("c7c5" to 20f, "e7e5" to 20f), random = Random(5))
        val picks = List(2_000) { chooser.choose(listOf(afterE4), 1500, 1500)?.toUci() }.groupingBy { it }.eachCount()
        assertEquals(setOf("c7c5", "e7e5"), picks.keys)
        assertTrue(picks.getValue("c7c5") in 900..1100, "c7c5 picked ${picks["c7c5"]} times")
    }
}
