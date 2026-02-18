package com.chesspredictor.domain.rules

import com.chesspredictor.domain.usecases.SimpleOpeningBook
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SimpleOpeningBookTest {
    private val book = SimpleOpeningBook()

    @Test
    fun testStartingPositionHasBookMoves() {
        val move = book.getBookMove("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 10)
        assertNotNull(move, "Starting position should have book moves")
        assertTrue(move.length == 4 || move.length == 5, "UCI move should be 4-5 chars")
    }

    @Test
    fun testAfterE4HasBookMoves() {
        val move = book.getBookMove("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", 10)
        assertNotNull(move, "Position after 1.e4 should have book moves")
    }

    @Test
    fun testAfterD4HasBookMoves() {
        val move = book.getBookMove("rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1", 10)
        assertNotNull(move, "Position after 1.d4 should have book moves")
    }

    @Test
    fun testAfterC4HasBookMoves() {
        val move = book.getBookMove("rnbqkbnr/pppppppp/8/8/2P5/8/PP1PPPPP/RNBQKBNR b KQkq c3 0 1", 10)
        assertNotNull(move, "Position after 1.c4 should have book moves")
    }

    @Test
    fun testAfterNf3HasBookMoves() {
        val move = book.getBookMove("rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1", 10)
        assertNotNull(move, "Position after 1.Nf3 should have book moves")
    }

    @Test
    fun testDeeperPositionHasBookMoves() {
        val move = book.getBookMove("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3", 10)
        assertNotNull(move, "Position after 1.e4 e5 2.Nf3 Nc6 should have book moves")
    }

    @Test
    fun testRandomPositionNoBookMove() {
        val move = book.getBookMove("8/8/8/3k4/8/8/8/4K2R w - - 0 1", 10)
        assertTrue(move == null, "Endgame position should not have book moves")
    }

    @Test
    fun testClocksDontAffectLookup() {
        val move1 = book.getBookMove("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 10)
        val move2 = book.getBookMove("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 5 10", 10)
        assertNotNull(move1)
        assertNotNull(move2)
    }

    @Test
    fun testStartingPositionReturnsValidUci() {
        val validFirstMoves = setOf(
            "e2e4", "d2d4", "c2c4", "g1f3", "b1c3", "f2f4",
            "e2e3", "d2d3", "c2c3", "a2a3", "a2a4", "b2b3",
            "b2b4", "f2f3", "g2g3", "g2g4", "h2h3", "h2h4",
            "b1a3", "g1h3"
        )
        repeat(20) {
            val move = book.getBookMove("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 10)
            assertNotNull(move)
            assertTrue(move in validFirstMoves, "Move $move should be a valid first move")
        }
    }
}
