package com.chesspredictor.domain.rules

import com.chesspredictor.domain.usecases.ChessRulesEngine
import com.chesspredictor.domain.usecases.SimpleOpeningBook
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SimpleOpeningBookTest {
    private val book = SimpleOpeningBook()
    private val rules = ChessRulesEngine()

    private fun assertOnlyLegalBookMoves(fen: String) {
        val legal = rules.getLegalMoves(rules.parseGameState(fen)).map { "${it.from}${it.to}" }.toSet()
        repeat(30) {
            val move = assertNotNull(book.getBookMove(fen, 10), "$fen should be in the book")
            assertTrue(move.take(4) in legal, "book move $move is illegal in $fen")
        }
    }

    @Test
    fun testStartingPositionHasBookMoves() =
        assertOnlyLegalBookMoves("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")

    @Test
    fun testAfterE4HasBookMoves() =
        assertOnlyLegalBookMoves("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")

    @Test
    fun testAfterD4HasBookMoves() =
        assertOnlyLegalBookMoves("rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1")

    @Test
    fun testAfterC4HasBookMoves() =
        assertOnlyLegalBookMoves("rnbqkbnr/pppppppp/8/8/2P5/8/PP1PPPPP/RNBQKBNR b KQkq c3 0 1")

    @Test
    fun testAfterNf3HasBookMoves() =
        assertOnlyLegalBookMoves("rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1")

    @Test
    fun testDeeperPositionHasBookMoves() =
        assertOnlyLegalBookMoves("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3")

    @Test
    fun testRandomPositionNoBookMove() {
        assertNull(book.getBookMove("8/8/8/3k4/8/8/8/4K2R w - - 0 1", 10), "Endgame position should not have book moves")
    }

    @Test
    fun testClocksDontAffectLookup() =
        assertOnlyLegalBookMoves("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 5 10")
}
