package com.chesspredictor.domain.rules

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.usecases.ChessRulesEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChessRulesEngineIntegrationTest {
    private val engine = ChessRulesEngine()

    private fun makeMove(fen: String, from: Square, to: Square, promotion: ChessPiece? = null): com.chesspredictor.domain.entities.GameState? {
        val state = engine.parseGameState(fen)
        return makeMoveFromState(state, from, to, promotion)
    }

    private fun makeMoveFromState(state: com.chesspredictor.domain.entities.GameState, from: Square, to: Square, promotion: ChessPiece? = null): com.chesspredictor.domain.entities.GameState? {
        val legalMoves = engine.getLegalMoves(state, from)
        val move = legalMoves.find { it.to == to && (promotion == null || it.promotion == promotion) } ?: return null
        return engine.makeMove(state, move)
    }

    @Test
    fun testScholarsMate() {
        var state = engine.parseGameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")

        state = makeMoveFromState(state, Square('e', 2), Square('e', 4))!!
        state = makeMoveFromState(state, Square('e', 7), Square('e', 5))!!
        state = makeMoveFromState(state, Square('f', 1), Square('c', 4))!!
        state = makeMoveFromState(state, Square('b', 8), Square('c', 6))!!
        state = makeMoveFromState(state, Square('d', 1), Square('h', 5))!!
        state = makeMoveFromState(state, Square('g', 8), Square('f', 6))!!
        state = makeMoveFromState(state, Square('h', 5), Square('f', 7))!!

        assertTrue(state.isCheckmate)
        assertEquals(ChessColor.BLACK, state.turn)
    }

    @Test
    fun testFoolsMate() {
        var state = engine.parseGameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")

        state = makeMoveFromState(state, Square('f', 2), Square('f', 3))!!
        state = makeMoveFromState(state, Square('e', 7), Square('e', 5))!!
        state = makeMoveFromState(state, Square('g', 2), Square('g', 4))!!
        state = makeMoveFromState(state, Square('d', 8), Square('h', 4))!!

        assertTrue(state.isCheckmate)
        assertEquals(ChessColor.WHITE, state.turn)
    }

    @Test
    fun testCastlingKingside() {
        val state = makeMove("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1", Square('e', 1), Square('g', 1))
        assertNotNull(state)
        assertEquals(ChessPiece.King(ChessColor.WHITE), state.board[Square('g', 1)])
        assertEquals(ChessPiece.Rook(ChessColor.WHITE), state.board[Square('f', 1)])
        assertNull(state.board[Square('e', 1)])
        assertNull(state.board[Square('h', 1)])
    }

    @Test
    fun testCastlingQueenside() {
        val state = makeMove("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1", Square('e', 1), Square('c', 1))
        assertNotNull(state)
        assertEquals(ChessPiece.King(ChessColor.WHITE), state.board[Square('c', 1)])
        assertEquals(ChessPiece.Rook(ChessColor.WHITE), state.board[Square('d', 1)])
    }

    @Test
    fun testEnPassantCapture() {
        var state = engine.parseGameState("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")
        state = makeMoveFromState(state, Square('d', 7), Square('d', 5))!!
        state = makeMoveFromState(state, Square('e', 4), Square('d', 5))!!

        assertEquals(ChessPiece.Pawn(ChessColor.WHITE), state.board[Square('d', 5)])
    }

    @Test
    fun testEnPassantFullSequence() {
        var state = engine.parseGameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")

        state = makeMoveFromState(state, Square('e', 2), Square('e', 4))!!
        state = makeMoveFromState(state, Square('a', 7), Square('a', 6))!!
        state = makeMoveFromState(state, Square('e', 4), Square('e', 5))!!
        state = makeMoveFromState(state, Square('d', 7), Square('d', 5))!!

        assertNotNull(state.enPassantSquare)
        assertEquals(Square('d', 6), state.enPassantSquare)

        state = makeMoveFromState(state, Square('e', 5), Square('d', 6))!!
        assertEquals(ChessPiece.Pawn(ChessColor.WHITE), state.board[Square('d', 6)])
        assertNull(state.board[Square('d', 5)])
        assertNull(state.board[Square('e', 5)])
    }

    @Test
    fun testPromotion() {
        val state = makeMove("8/4P3/8/8/8/8/8/4K2k w - - 0 1", Square('e', 7), Square('e', 8), ChessPiece.Queen(ChessColor.WHITE))
        assertNotNull(state)
        assertEquals(ChessPiece.Queen(ChessColor.WHITE), state.board[Square('e', 8)])
    }

    @Test
    fun testIllegalMoveReturnsNull() {
        val state = engine.parseGameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        val illegalMove = ChessMove(Square('e', 2), Square('e', 5), ChessPiece.Pawn(ChessColor.WHITE))
        val result = engine.makeMove(state, illegalMove)
        assertNull(result)
    }

    @Test
    fun testCannotMoveIntoCheck() {
        val state = engine.parseGameState("8/8/8/8/8/8/8/r3K2k w - - 0 1")
        val legalMoves = engine.getLegalMoves(state, Square('e', 1))
        val targets = legalMoves.map { it.to }.toSet()
        assertFalse(Square('d', 1) in targets)
        assertFalse(Square('f', 1) in targets)
    }

    @Test
    fun testMustEscapeCheck() {
        val state = engine.parseGameState("4k3/8/8/8/8/8/r7/4K3 w - - 0 1")
        val allMoves = engine.getLegalMoves(state)
        for (move in allMoves) {
            assertEquals(Square('e', 1), move.from, "Only king moves should be legal when in check with no blocks")
        }
    }

    @Test
    fun testStalemateDetection() {
        val state = engine.parseGameState("k7/2Q5/1K6/8/8/8/8/8 b - - 0 1")
        assertTrue(engine.isStalemate(state))
        assertFalse(engine.isCheckmate(state))
    }

    @Test
    fun testDrawByInsufficientMaterial() {
        val state = engine.parseGameState("8/8/8/3k4/8/8/8/4K3 w - - 0 1")
        assertTrue(engine.isDraw(state))
    }
}
