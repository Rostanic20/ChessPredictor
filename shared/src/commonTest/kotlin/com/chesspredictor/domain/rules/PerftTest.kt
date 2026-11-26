package com.chesspredictor.domain.rules

import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.usecases.ChessRulesEngine
import kotlin.test.Test
import kotlin.test.assertEquals

class PerftTest {
    private val engine = ChessRulesEngine()

    private fun perft(state: GameState, depth: Int): Long {
        val moves = engine.getLegalMoves(state)
        if (depth == 1) return moves.size.toLong()
        return moves.sumOf { move -> perft(requireNotNull(engine.makeMove(state, move)) { "rejected $move" }, depth - 1) }
    }

    private fun assertPerft(fen: String, vararg expected: Long) {
        val state = engine.parseGameState(fen)
        expected.forEachIndexed { index, nodes -> assertEquals(nodes, perft(state, index + 1), "perft(${index + 1}) of $fen") }
    }

    @Test
    fun startingPosition() = assertPerft("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 20, 400, 8902)

    @Test
    fun kiwipeteCastlingAndEnPassant() =
        assertPerft("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1", 48, 2039, 97862)

    @Test
    fun endgameWithDiscoveredChecksAndPinnedEnPassant() =
        assertPerft("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1", 14, 191, 2812, 43238)

    @Test
    fun promotionsAndCastlingRightsAfterCaptures() =
        assertPerft("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1", 6, 264, 9467)

    @Test
    fun promotionWithCheckAndCapture() =
        assertPerft("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 44, 1486, 62379)
}
