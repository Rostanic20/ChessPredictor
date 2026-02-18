package com.chesspredictor.domain.rules

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.utils.FenParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFails

class FenParserTest {
    private val fenParser = FenParser()

    @Test
    fun testParseStartingPosition() {
        val state = fenParser.parseGameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        assertEquals(32, state.board.size)
        assertEquals(ChessColor.WHITE, state.turn)
        assertTrue(state.castlingRights.whiteKingside)
        assertTrue(state.castlingRights.whiteQueenside)
        assertTrue(state.castlingRights.blackKingside)
        assertTrue(state.castlingRights.blackQueenside)
        assertNull(state.enPassantSquare)
        assertEquals(0, state.halfMoveClock)
        assertEquals(1, state.fullMoveNumber)
    }

    @Test
    fun testParseStartingPositionPieces() {
        val state = fenParser.parseGameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        assertEquals(ChessPiece.King(ChessColor.WHITE), state.board[Square('e', 1)])
        assertEquals(ChessPiece.Queen(ChessColor.WHITE), state.board[Square('d', 1)])
        assertEquals(ChessPiece.Rook(ChessColor.WHITE), state.board[Square('a', 1)])
        assertEquals(ChessPiece.Rook(ChessColor.WHITE), state.board[Square('h', 1)])
        assertEquals(ChessPiece.Knight(ChessColor.WHITE), state.board[Square('b', 1)])
        assertEquals(ChessPiece.Bishop(ChessColor.WHITE), state.board[Square('c', 1)])
        assertEquals(ChessPiece.Pawn(ChessColor.WHITE), state.board[Square('e', 2)])
        assertEquals(ChessPiece.King(ChessColor.BLACK), state.board[Square('e', 8)])
        assertEquals(ChessPiece.Queen(ChessColor.BLACK), state.board[Square('d', 8)])
        assertEquals(ChessPiece.Pawn(ChessColor.BLACK), state.board[Square('a', 7)])
    }

    @Test
    fun testParseAfterE4() {
        val state = fenParser.parseGameState("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")
        assertEquals(ChessColor.BLACK, state.turn)
        assertEquals(Square('e', 3), state.enPassantSquare)
        assertEquals(32, state.board.size)
        assertEquals(ChessPiece.Pawn(ChessColor.WHITE), state.board[Square('e', 4)])
        assertNull(state.board[Square('e', 2)])
    }

    @Test
    fun testParseMidgamePosition() {
        val state = fenParser.parseGameState("r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4")
        assertEquals(ChessColor.WHITE, state.turn)
        assertEquals(4, state.halfMoveClock)
        assertEquals(4, state.fullMoveNumber)
        assertTrue(state.castlingRights.whiteKingside)
        assertTrue(state.castlingRights.blackKingside)
        assertEquals(ChessPiece.Bishop(ChessColor.WHITE), state.board[Square('c', 4)])
        assertEquals(ChessPiece.Knight(ChessColor.BLACK), state.board[Square('c', 6)])
    }

    @Test
    fun testParseNoCastlingRights() {
        val state = fenParser.parseGameState("8/8/8/3k4/3K4/8/8/8 w - - 0 1")
        assertEquals(2, state.board.size)
        assertEquals(false, state.castlingRights.whiteKingside)
        assertEquals(false, state.castlingRights.whiteQueenside)
        assertEquals(false, state.castlingRights.blackKingside)
        assertEquals(false, state.castlingRights.blackQueenside)
    }

    @Test
    fun testParsePartialCastlingRights() {
        val state = fenParser.parseGameState("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w Kq - 0 1")
        assertTrue(state.castlingRights.whiteKingside)
        assertEquals(false, state.castlingRights.whiteQueenside)
        assertEquals(false, state.castlingRights.blackKingside)
        assertTrue(state.castlingRights.blackQueenside)
    }

    @Test
    fun testRoundTripStartingPosition() {
        val originalFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val state = fenParser.parseGameState(originalFen)
        val regeneratedFen = state.toFen()
        assertEquals(originalFen, regeneratedFen)
    }

    @Test
    fun testRoundTripCustomPosition() {
        val originalFen = "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
        val state = fenParser.parseGameState(originalFen)
        val regeneratedFen = state.toFen()
        assertEquals(originalFen, regeneratedFen)
    }

    @Test
    fun testRoundTripWithEnPassant() {
        val originalFen = "rnbqkbnr/pppp1ppp/8/4pP2/8/8/PPPPP1PP/RNBQKBNR w KQkq e6 0 3"
        val state = fenParser.parseGameState(originalFen)
        val regeneratedFen = state.toFen()
        assertEquals(originalFen, regeneratedFen)
    }
}
