package com.chesspredictor.domain.rules

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.usecases.components.CheckDetector
import com.chesspredictor.domain.usecases.components.MoveGenerator
import com.chesspredictor.domain.utils.FenParser
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckDetectorTest {
    private val fenParser = FenParser()
    private val moveGenerator = MoveGenerator()
    private val checkDetector = CheckDetector(moveGenerator)

    @Test
    fun testNotInCheckStartingPosition() {
        val state = fenParser.parseGameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        assertFalse(checkDetector.isInCheck(state, ChessColor.WHITE))
        assertFalse(checkDetector.isInCheck(state, ChessColor.BLACK))
    }

    @Test
    fun testCheckByRook() {
        val state = fenParser.parseGameState("8/8/8/8/8/8/8/r3K2k w - - 0 1")
        assertTrue(checkDetector.isInCheck(state, ChessColor.WHITE))
    }

    @Test
    fun testCheckByBishop() {
        val state = fenParser.parseGameState("8/8/8/8/8/2b5/8/4K2k w - - 0 1")
        assertTrue(checkDetector.isInCheck(state, ChessColor.WHITE))
    }

    @Test
    fun testCheckByKnight() {
        val state = fenParser.parseGameState("8/8/8/8/8/3n4/8/4K2k w - - 0 1")
        assertTrue(checkDetector.isInCheck(state, ChessColor.WHITE))
    }

    @Test
    fun testCheckByPawn() {
        val state = fenParser.parseGameState("8/8/8/8/8/8/3p4/4K2k w - - 0 1")
        assertTrue(checkDetector.isInCheck(state, ChessColor.WHITE))
    }

    @Test
    fun testCheckByQueen() {
        val state = fenParser.parseGameState("4q3/8/8/8/8/8/8/4K2k w - - 0 1")
        assertTrue(checkDetector.isInCheck(state, ChessColor.WHITE))
    }

    @Test
    fun testCheckmateScholarsMate() {
        val state = fenParser.parseGameState("r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4")
        assertTrue(checkDetector.isCheckmate(state))
    }

    @Test
    fun testCheckmateBackRank() {
        val state = fenParser.parseGameState("R5k1/5ppp/8/8/8/8/8/4K3 b - - 0 1")
        assertTrue(checkDetector.isCheckmate(state))
    }

    @Test
    fun testNotCheckmateCanBlock() {
        val state = fenParser.parseGameState("4k3/8/8/8/8/8/4r3/R3K3 b - - 0 1")
        assertFalse(checkDetector.isCheckmate(state))
    }

    @Test
    fun testStalemateKingTrapped() {
        val state = fenParser.parseGameState("k7/8/1K6/8/8/8/8/8 b - - 0 1")
        assertFalse(checkDetector.isStalemate(state), "King has moves, not stalemate")
    }

    @Test
    fun testStalemateClassic() {
        val state = fenParser.parseGameState("k7/2Q5/1K6/8/8/8/8/8 b - - 0 1")
        assertTrue(checkDetector.isStalemate(state))
    }

    @Test
    fun testNotStalemateWhenInCheck() {
        val state = fenParser.parseGameState("k7/1Q6/1K6/8/8/8/8/8 b - - 0 1")
        assertFalse(checkDetector.isStalemate(state))
    }
}
