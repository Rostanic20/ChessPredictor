package com.chesspredictor.domain.rules

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.usecases.components.AttackDetector
import com.chesspredictor.domain.utils.FenParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AttackDetectorTest {
    private val fenParser = FenParser()

    @Test
    fun testWhitePawnAttacks() {
        val state = fenParser.parseGameState("8/8/8/8/4P3/8/8/4K2k w - - 0 1")
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('d', 5), ChessColor.WHITE))
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('f', 5), ChessColor.WHITE))
        assertFalse(AttackDetector.isSquareAttackedBy(state, Square('e', 5), ChessColor.WHITE))
    }

    @Test
    fun testBlackPawnAttacks() {
        val state = fenParser.parseGameState("4k3/8/8/4p3/8/8/8/4K3 w - - 0 1")
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('d', 4), ChessColor.BLACK))
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('f', 4), ChessColor.BLACK))
        assertFalse(AttackDetector.isSquareAttackedBy(state, Square('e', 4), ChessColor.BLACK))
    }

    @Test
    fun testKnightAttacks() {
        val state = fenParser.parseGameState("8/8/8/8/3N4/8/8/4K2k w - - 0 1")
        val knightTargets = listOf(
            Square('c', 2), Square('e', 2), Square('b', 3), Square('f', 3),
            Square('b', 5), Square('f', 5), Square('c', 6), Square('e', 6)
        )
        for (target in knightTargets) {
            assertTrue(AttackDetector.isSquareAttackedBy(state, target, ChessColor.WHITE),
                "Knight at d4 should attack $target")
        }
        assertFalse(AttackDetector.isSquareAttackedBy(state, Square('d', 5), ChessColor.WHITE))
    }

    @Test
    fun testBishopAttacksDiagonal() {
        val state = fenParser.parseGameState("8/8/8/8/3B4/8/8/4K2k w - - 0 1")
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('a', 1), ChessColor.WHITE))
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('g', 7), ChessColor.WHITE))
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('a', 7), ChessColor.WHITE))
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('f', 2), ChessColor.WHITE))
        assertFalse(AttackDetector.isSquareAttackedBy(state, Square('d', 5), ChessColor.WHITE))
    }

    @Test
    fun testBishopBlockedByPiece() {
        val state = fenParser.parseGameState("8/8/8/4N3/3B4/8/8/4K2k w - - 0 1")
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('e', 5), ChessColor.WHITE))
        assertFalse(AttackDetector.isSquareAttackedBy(state, Square('f', 6), ChessColor.WHITE))
    }

    @Test
    fun testRookAttacksRankAndFile() {
        val state = fenParser.parseGameState("8/8/8/8/3R4/8/8/4K2k w - - 0 1")
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('d', 8), ChessColor.WHITE))
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('d', 1), ChessColor.WHITE))
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('a', 4), ChessColor.WHITE))
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('h', 4), ChessColor.WHITE))
        assertFalse(AttackDetector.isSquareAttackedBy(state, Square('e', 5), ChessColor.WHITE))
    }

    @Test
    fun testRookBlockedByPiece() {
        val state = fenParser.parseGameState("8/8/8/3P4/3R4/8/8/4K2k w - - 0 1")
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('d', 5), ChessColor.WHITE))
        assertFalse(AttackDetector.isSquareAttackedBy(state, Square('d', 6), ChessColor.WHITE))
    }

    @Test
    fun testQueenAttacksAllDirections() {
        val state = fenParser.parseGameState("8/8/8/8/3Q4/8/8/4K2k w - - 0 1")
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('d', 8), ChessColor.WHITE))
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('a', 4), ChessColor.WHITE))
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('a', 7), ChessColor.WHITE))
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('g', 7), ChessColor.WHITE))
        assertTrue(AttackDetector.isSquareAttackedBy(state, Square('f', 2), ChessColor.WHITE))
    }

    @Test
    fun testKingAttacksAdjacent() {
        val state = fenParser.parseGameState("8/8/8/8/3K4/8/8/7k w - - 0 1")
        val adjacentSquares = listOf(
            Square('c', 3), Square('d', 3), Square('e', 3),
            Square('c', 4), Square('e', 4),
            Square('c', 5), Square('d', 5), Square('e', 5)
        )
        for (sq in adjacentSquares) {
            assertTrue(AttackDetector.isSquareAttackedBy(state, sq, ChessColor.WHITE),
                "King at d4 should attack $sq")
        }
        assertFalse(AttackDetector.isSquareAttackedBy(state, Square('d', 6), ChessColor.WHITE))
    }

    @Test
    fun testFindKingSquare() {
        val state = fenParser.parseGameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        assertEquals(Square('e', 1), AttackDetector.findKingSquare(state, ChessColor.WHITE))
        assertEquals(Square('e', 8), AttackDetector.findKingSquare(state, ChessColor.BLACK))
    }
}
