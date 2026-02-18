package com.chesspredictor.domain.rules

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.usecases.components.MoveExecutor
import com.chesspredictor.domain.utils.FenParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoveExecutorTest {
    private val fenParser = FenParser()
    private val moveExecutor = MoveExecutor()

    @Test
    fun testSimplePawnMove() {
        val state = fenParser.parseGameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        val move = ChessMove(Square('e', 2), Square('e', 4), ChessPiece.Pawn(ChessColor.WHITE))
        val newState = moveExecutor.executeMove(state, move)

        assertEquals(ChessPiece.Pawn(ChessColor.WHITE), newState.board[Square('e', 4)])
        assertNull(newState.board[Square('e', 2)])
        assertEquals(ChessColor.BLACK, newState.turn)
        assertEquals(Square('e', 3), newState.enPassantSquare)
        assertEquals(0, newState.halfMoveClock)
    }

    @Test
    fun testCapture() {
        val state = fenParser.parseGameState("8/8/8/3p4/4P3/8/8/4K2k w - - 5 10")
        val move = ChessMove(Square('e', 4), Square('d', 5), ChessPiece.Pawn(ChessColor.WHITE), ChessPiece.Pawn(ChessColor.BLACK))
        val newState = moveExecutor.executeMove(state, move)

        assertEquals(ChessPiece.Pawn(ChessColor.WHITE), newState.board[Square('d', 5)])
        assertNull(newState.board[Square('e', 4)])
        assertEquals(0, newState.halfMoveClock)
        assertTrue(newState.capturedPieces.contains(ChessPiece.Pawn(ChessColor.BLACK)))
    }

    @Test
    fun testEnPassantCapture() {
        val state = fenParser.parseGameState("rnbqkbnr/pppp1ppp/8/4pP2/8/8/PPPPP1PP/RNBQKBNR w KQkq e6 0 3")
        val capturedPawn = state.board[Square('e', 5)]
        val move = ChessMove(Square('f', 5), Square('e', 6), ChessPiece.Pawn(ChessColor.WHITE), capturedPawn)
        val newState = moveExecutor.executeMove(state, move)

        assertEquals(ChessPiece.Pawn(ChessColor.WHITE), newState.board[Square('e', 6)])
        assertNull(newState.board[Square('f', 5)])
        assertNull(newState.board[Square('e', 5)], "Captured pawn should be removed")
    }

    @Test
    fun testKingsideCastling() {
        val state = fenParser.parseGameState("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1")
        val move = ChessMove(Square('e', 1), Square('g', 1), ChessPiece.King(ChessColor.WHITE))
        val newState = moveExecutor.executeMove(state, move)

        assertEquals(ChessPiece.King(ChessColor.WHITE), newState.board[Square('g', 1)])
        assertEquals(ChessPiece.Rook(ChessColor.WHITE), newState.board[Square('f', 1)])
        assertNull(newState.board[Square('e', 1)])
        assertNull(newState.board[Square('h', 1)])
    }

    @Test
    fun testQueensideCastling() {
        val state = fenParser.parseGameState("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1")
        val move = ChessMove(Square('e', 1), Square('c', 1), ChessPiece.King(ChessColor.WHITE))
        val newState = moveExecutor.executeMove(state, move)

        assertEquals(ChessPiece.King(ChessColor.WHITE), newState.board[Square('c', 1)])
        assertEquals(ChessPiece.Rook(ChessColor.WHITE), newState.board[Square('d', 1)])
        assertNull(newState.board[Square('e', 1)])
        assertNull(newState.board[Square('a', 1)])
    }

    @Test
    fun testPromotion() {
        val state = fenParser.parseGameState("8/4P3/8/8/8/8/8/4K2k w - - 0 1")
        val move = ChessMove(
            Square('e', 7), Square('e', 8),
            ChessPiece.Pawn(ChessColor.WHITE),
            promotion = ChessPiece.Queen(ChessColor.WHITE)
        )
        val newState = moveExecutor.executeMove(state, move)

        assertEquals(ChessPiece.Queen(ChessColor.WHITE), newState.board[Square('e', 8)])
        assertNull(newState.board[Square('e', 7)])
    }

    @Test
    fun testCastlingRightsRemovedAfterKingMove() {
        val state = fenParser.parseGameState("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1")
        val move = ChessMove(Square('e', 1), Square('d', 1), ChessPiece.King(ChessColor.WHITE))
        val newState = moveExecutor.executeMove(state, move)

        assertEquals(false, newState.castlingRights.whiteKingside)
        assertEquals(false, newState.castlingRights.whiteQueenside)
        assertTrue(newState.castlingRights.blackKingside)
        assertTrue(newState.castlingRights.blackQueenside)
    }

    @Test
    fun testCastlingRightsRemovedAfterRookMove() {
        val state = fenParser.parseGameState("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1")
        val move = ChessMove(Square('h', 1), Square('g', 1), ChessPiece.Rook(ChessColor.WHITE))
        val newState = moveExecutor.executeMove(state, move)

        assertEquals(false, newState.castlingRights.whiteKingside)
        assertTrue(newState.castlingRights.whiteQueenside)
    }

    @Test
    fun testCastlingRightsRemovedAfterRookCapture() {
        val state = fenParser.parseGameState("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K1NR w KQkq - 0 1")
        val move = ChessMove(
            Square('g', 1), Square('h', 8),
            ChessPiece.Knight(ChessColor.WHITE), // This isn't realistic but tests the mechanic
            ChessPiece.Rook(ChessColor.BLACK)    // Actually let me use a more realistic scenario
        )
        // The updateCastlingRights checks move.to, so capturing at h8 should remove blackKingside
        val newState = moveExecutor.executeMove(state, move)
        assertEquals(false, newState.castlingRights.blackKingside)
    }

    @Test
    fun testHalfMoveClockIncrements() {
        val state = fenParser.parseGameState("8/8/8/8/8/8/8/R3K2k w - - 5 10")
        val move = ChessMove(Square('a', 1), Square('a', 2), ChessPiece.Rook(ChessColor.WHITE))
        val newState = moveExecutor.executeMove(state, move)
        assertEquals(6, newState.halfMoveClock)
    }

    @Test
    fun testHalfMoveClockResetsOnPawnMove() {
        val state = fenParser.parseGameState("8/8/8/8/8/8/4P3/4K2k w - - 5 10")
        val move = ChessMove(Square('e', 2), Square('e', 3), ChessPiece.Pawn(ChessColor.WHITE))
        val newState = moveExecutor.executeMove(state, move)
        assertEquals(0, newState.halfMoveClock)
    }

    @Test
    fun testFullMoveNumberIncrementsAfterBlack() {
        val state = fenParser.parseGameState("8/8/8/8/8/8/4p3/4K2k b - - 0 5")
        val move = ChessMove(Square('e', 2), Square('e', 1), ChessPiece.Pawn(ChessColor.BLACK), promotion = ChessPiece.Queen(ChessColor.BLACK))
        val newState = moveExecutor.executeMove(state, move)
        assertEquals(6, newState.fullMoveNumber)
    }

    @Test
    fun testMoveHistoryUpdated() {
        val state = fenParser.parseGameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        val move = ChessMove(Square('e', 2), Square('e', 4), ChessPiece.Pawn(ChessColor.WHITE))
        val newState = moveExecutor.executeMove(state, move)
        assertEquals(1, newState.moveHistory.size)
        assertEquals(move.from, newState.moveHistory[0].move.from)
        assertEquals(move.to, newState.moveHistory[0].move.to)
        assertTrue(newState.moveHistory[0].isWhiteMove)
    }

    @Test
    fun testPositionHistoryUpdated() {
        val state = fenParser.parseGameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        val move = ChessMove(Square('e', 2), Square('e', 4), ChessPiece.Pawn(ChessColor.WHITE))
        val newState = moveExecutor.executeMove(state, move)
        assertEquals(1, newState.positionHistory.size)
        assertTrue(newState.positionHistory[0].startsWith("rnbqkbnr/pppppppp"))
    }
}
