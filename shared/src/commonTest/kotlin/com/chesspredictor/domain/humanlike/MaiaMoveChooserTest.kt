package com.chesspredictor.domain.humanlike

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

    @Test
    fun movesAreSampledInProportionToTheirProbability() = runTest {
        val chooser = MaiaMoveChooser(predictor("c7c5" to 20f, "e7e5" to 20f), random = Random(5))
        val picks = List(2_000) { chooser.choose(listOf(afterE4), 1500, 1500)?.toUci() }.groupingBy { it }.eachCount()
        assertEquals(setOf("c7c5", "e7e5"), picks.keys)
        assertTrue(picks.getValue("c7c5") in 900..1100, "c7c5 picked ${picks["c7c5"]} times")
    }
}
