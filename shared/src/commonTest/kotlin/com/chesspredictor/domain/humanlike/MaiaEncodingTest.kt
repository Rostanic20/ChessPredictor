package com.chesspredictor.domain.humanlike

import kotlin.test.Test
import kotlin.test.assertEquals

class MaiaEncodingTest {
    private val start = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    private val afterE4 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"

    private fun plane(tokens: FloatArray, square: String, slot: Int, plane: Int): Float {
        val index = (square[1] - '1') * 8 + (square[0] - 'a')
        return tokens[index * MaiaEncoding.FEATURES + slot * 12 + plane]
    }

    @Test
    fun whiteToMoveKeepsTheBoardAsItIs() {
        val tokens = MaiaEncoding.tokens(listOf(start))
        assertEquals(1f, plane(tokens, "e1", 7, 5))
        assertEquals(1f, plane(tokens, "e8", 7, 11))
        assertEquals(1f, plane(tokens, "a2", 7, 0))
        assertEquals(32f, tokens.sum() / MaiaEncoding.HISTORY)
    }

    @Test
    fun blackToMoveMirrorsRanksAndSwapsColours() {
        val tokens = MaiaEncoding.tokens(listOf(start, afterE4))
        assertEquals(1f, plane(tokens, "e1", 7, 5), "black king seen as own king on e1")
        assertEquals(1f, plane(tokens, "e5", 7, 6), "white e4 pawn seen as an enemy pawn on e5")
        assertEquals(1f, plane(tokens, "e1", 6, 5), "the previous position keeps its own perspective")
    }

    @Test
    fun shortHistoryIsPaddedWithTheEarliestPosition() {
        val tokens = MaiaEncoding.tokens(listOf(start, afterE4))
        (0..6).forEach { slot -> assertEquals(1f, plane(tokens, "e2", slot, 0), "slot $slot") }
        assertEquals(0f, tokens[MaiaEncoding.FEATURES - 1])
    }

    @Test
    fun movesRoundTripThroughIndicesForBothSides() {
        listOf("e2e4", "g1f3", "e1g1", "a7a8q", "h7g8n").forEach {
            assertEquals(it, MaiaEncoding.uciOf(MaiaEncoding.moveIndex(it, blackToMove = false), blackToMove = false))
        }
        listOf("e7e5", "e8g8", "b2b1r").forEach {
            assertEquals(it, MaiaEncoding.uciOf(MaiaEncoding.moveIndex(it, blackToMove = true), blackToMove = true))
        }
        assertEquals(MaiaEncoding.moveIndex("e2e4", false), MaiaEncoding.moveIndex("e7e5", true))
        assertEquals(12 * 64 + 28, MaiaEncoding.moveIndex("e2e4", false))
        assertEquals(4096 + (0 * 8 + 0) * 4 + 0, MaiaEncoding.moveIndex("a7a8q", false))
    }
}
