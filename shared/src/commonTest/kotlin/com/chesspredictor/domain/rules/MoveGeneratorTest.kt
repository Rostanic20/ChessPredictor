package com.chesspredictor.domain.rules

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.usecases.components.MoveGenerator
import com.chesspredictor.domain.utils.FenParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoveGeneratorTest {
    private val fenParser = FenParser()
    private val moveGenerator = MoveGenerator()

    private fun legalMovesFrom(fen: String, from: Square): List<ChessMove> {
        val state = fenParser.parseGameState(fen)
        return moveGenerator.generateLegalMoves(state).filter { it.from == from }
    }

    private fun legalMoveTargets(fen: String, from: Square): Set<Square> =
        legalMovesFrom(fen, from).map { it.to }.toSet()

    @Test
    fun testStartingPositionHas20Moves() {
        val state = fenParser.parseGameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        val moves = moveGenerator.generateLegalMoves(state)
        assertEquals(20, moves.size)
    }

    @Test
    fun testPawnSingleForward() {
        val targets = legalMoveTargets("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", Square('e', 2))
        assertTrue(Square('e', 3) in targets)
    }

    @Test
    fun testPawnDoubleForward() {
        val targets = legalMoveTargets("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", Square('e', 2))
        assertTrue(Square('e', 4) in targets)
    }

    @Test
    fun testPawnDoubleForwardBlocked() {
        val targets = legalMoveTargets("8/8/8/8/8/4p3/4P3/4K2k w - - 0 1", Square('e', 2))
        assertTrue(targets.isEmpty())
    }

    @Test
    fun testPawnCaptureDiagonal() {
        val targets = legalMoveTargets("8/8/8/3p4/4P3/8/8/4K2k w - - 0 1", Square('e', 4))
        assertTrue(Square('d', 5) in targets)
    }

    @Test
    fun testPawnCannotCaptureForward() {
        val targets = legalMoveTargets("8/8/8/4p3/4P3/8/8/4K2k w - - 0 1", Square('e', 4))
        assertFalse(Square('e', 5) in targets)
    }

    @Test
    fun testPawnEnPassant() {
        val targets = legalMoveTargets("rnbqkbnr/pppp1ppp/8/4pP2/8/8/PPPPP1PP/RNBQKBNR w KQkq e6 0 3", Square('f', 5))
        assertTrue(Square('e', 6) in targets, "En passant capture should be available")
    }

    @Test
    fun testPawnPromotion() {
        val moves = legalMovesFrom("8/4P3/8/8/8/8/8/4K2k w - - 0 1", Square('e', 7))
        val promotionMoves = moves.filter { it.to == Square('e', 8) && it.promotion != null }
        assertEquals(4, promotionMoves.size, "Should generate 4 promotion options (Q, R, B, N)")
        val promotionTypes = promotionMoves.mapNotNull { it.promotion }.map { it::class }.toSet()
        assertEquals(4, promotionTypes.size)
    }

    @Test
    fun testPawnPromotionCapture() {
        val moves = legalMovesFrom("3r4/4P3/8/8/8/8/8/4K2k w - - 0 1", Square('e', 7))
        val capturePromotions = moves.filter { it.to == Square('d', 8) && it.promotion != null }
        assertEquals(4, capturePromotions.size, "Should generate 4 promotion captures")
    }

    @Test
    fun testKnightFromCenter() {
        val targets = legalMoveTargets("8/8/8/8/3N4/8/8/4K2k w - - 0 1", Square('d', 4))
        val expected = setOf(
            Square('c', 2), Square('e', 2), Square('b', 3), Square('f', 3),
            Square('b', 5), Square('f', 5), Square('c', 6), Square('e', 6)
        )
        assertEquals(expected, targets)
    }

    @Test
    fun testKnightFromCorner() {
        val targets = legalMoveTargets("8/8/8/8/8/8/8/N3K2k w - - 0 1", Square('a', 1))
        assertEquals(setOf(Square('b', 3), Square('c', 2)), targets)
    }

    @Test
    fun testKnightJumpsOverPieces() {
        val targets = legalMoveTargets("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", Square('b', 1))
        assertEquals(setOf(Square('a', 3), Square('c', 3)), targets)
    }

    @Test
    fun testBishopOpenBoard() {
        val targets = legalMoveTargets("8/8/8/8/3B4/8/8/4K2k w - - 0 1", Square('d', 4))
        assertEquals(13, targets.size)
    }

    @Test
    fun testRookOpenBoard() {
        val targets = legalMoveTargets("8/8/8/8/3R4/8/8/4K2k w - - 0 1", Square('d', 4))
        assertEquals(14, targets.size)
    }

    @Test
    fun testQueenOpenBoard() {
        val targets = legalMoveTargets("8/8/8/8/3Q4/8/8/4K2k w - - 0 1", Square('d', 4))
        assertEquals(27, targets.size)
    }

    @Test
    fun testKingNormalMoves() {
        val targets = legalMoveTargets("8/8/8/8/3K4/8/8/7k w - - 0 1", Square('d', 4))
        assertEquals(8, targets.size)
    }

    @Test
    fun testKingCannotMoveIntoCheck() {
        val targets = legalMoveTargets("8/8/8/8/3K4/8/8/3r3k w - - 0 1", Square('d', 4))
        assertFalse(Square('d', 3) in targets, "King cannot move onto rook's file")
    }

    @Test
    fun testWhiteKingsideCastling() {
        val targets = legalMoveTargets("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1", Square('e', 1))
        assertTrue(Square('g', 1) in targets, "White kingside castling should be available")
    }

    @Test
    fun testWhiteQueensideCastling() {
        val targets = legalMoveTargets("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1", Square('e', 1))
        assertTrue(Square('c', 1) in targets, "White queenside castling should be available")
    }

    @Test
    fun testBlackKingsideCastling() {
        val targets = legalMoveTargets("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R b KQkq - 0 1", Square('e', 8))
        assertTrue(Square('g', 8) in targets, "Black kingside castling should be available")
    }

    @Test
    fun testBlackQueensideCastling() {
        val targets = legalMoveTargets("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R b KQkq - 0 1", Square('e', 8))
        assertTrue(Square('c', 8) in targets, "Black queenside castling should be available")
    }

    @Test
    fun testCastlingBlockedByPiece() {
        val targets = legalMoveTargets("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3KB1R w KQkq - 0 1", Square('e', 1))
        assertFalse(Square('g', 1) in targets, "Castling blocked by bishop on f1")
    }

    @Test
    fun testCastlingThroughCheck() {
        // Black rook on f8 attacks f1
        val targets = legalMoveTargets("5r2/8/8/8/8/8/8/R3K2R w KQ - 0 1", Square('e', 1))
        assertFalse(Square('g', 1) in targets, "Cannot castle through attacked square f1")
    }

    @Test
    fun testCastlingWhileInCheck() {
        // Black rook on e8 checks king on e1
        val targets = legalMoveTargets("4r3/8/8/8/8/8/8/R3K2R w KQ - 0 1", Square('e', 1))
        assertFalse(Square('g', 1) in targets, "Cannot castle while in check")
        assertFalse(Square('c', 1) in targets)
    }

    @Test
    fun testCastlingRightsLost() {
        val targets = legalMoveTargets("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w - - 0 1", Square('e', 1))
        assertFalse(Square('g', 1) in targets, "No castling rights")
        assertFalse(Square('c', 1) in targets)
    }

    @Test
    fun testPinnedPieceCannotMove() {
        val state = fenParser.parseGameState("4r3/8/8/8/8/8/4B3/4K2k w - - 0 1")
        val bishopMoves = moveGenerator.generateLegalMoves(state).filter { it.from == Square('e', 2) }
        assertTrue(bishopMoves.isEmpty(), "A bishop pinned on a file has no legal move, got $bishopMoves")
    }

    @Test
    fun testPinnedRookMovesOnlyAlongThePin() {
        val state = fenParser.parseGameState("4r3/8/8/8/8/8/4R3/4K2k w - - 0 1")
        val targets = moveGenerator.generateLegalMoves(state).filter { it.from == Square('e', 2) }.map { it.to }.toSet()
        assertEquals((3..8).map { Square('e', it) }.toSet(), targets)
    }
}
