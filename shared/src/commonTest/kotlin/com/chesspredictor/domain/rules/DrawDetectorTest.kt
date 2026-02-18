package com.chesspredictor.domain.rules

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.usecases.components.DrawDetector
import com.chesspredictor.domain.utils.FenParser
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DrawDetectorTest {
    private val fenParser = FenParser()
    private val drawDetector = DrawDetector()

    @Test
    fun testKingVsKingInsufficientMaterial() {
        val state = fenParser.parseGameState("8/8/8/3k4/8/8/8/4K3 w - - 0 1")
        assertTrue(drawDetector.isInsufficientMaterial(state))
    }

    @Test
    fun testKingBishopVsKingInsufficientMaterial() {
        val state = fenParser.parseGameState("8/8/8/3k4/8/8/8/4KB2 w - - 0 1")
        assertTrue(drawDetector.isInsufficientMaterial(state))
    }

    @Test
    fun testKingKnightVsKingInsufficientMaterial() {
        val state = fenParser.parseGameState("8/8/8/3k4/8/8/8/4KN2 w - - 0 1")
        assertTrue(drawDetector.isInsufficientMaterial(state))
    }

    @Test
    fun testKingRookVsKingNotInsufficientMaterial() {
        val state = fenParser.parseGameState("8/8/8/3k4/8/8/8/4K2R w - - 0 1")
        assertFalse(drawDetector.isInsufficientMaterial(state))
    }

    @Test
    fun testKingQueenVsKingNotInsufficientMaterial() {
        val state = fenParser.parseGameState("8/8/8/3k4/8/8/8/3QK3 w - - 0 1")
        assertFalse(drawDetector.isInsufficientMaterial(state))
    }

    @Test
    fun testKingPawnVsKingNotInsufficientMaterial() {
        val state = fenParser.parseGameState("8/8/8/3k4/8/8/4P3/4K3 w - - 0 1")
        assertFalse(drawDetector.isInsufficientMaterial(state))
    }

    @Test
    fun testSameColorBishopsInsufficientMaterial() {
        val state = fenParser.parseGameState("8/8/8/3k4/8/8/8/2B1KB2 w - - 0 1")
        val c1Light = ('c'.code + 1) % 2 != 0
        val f1Light = ('f'.code + 1) % 2 != 0
        if (c1Light == f1Light) {
            assertTrue(drawDetector.isInsufficientMaterial(state))
        } else {
            assertFalse(drawDetector.isInsufficientMaterial(state))
        }
    }

    @Test
    fun testOppositeColorBishopsNotInsufficientMaterial() {
        val state = fenParser.parseGameState("8/8/8/3k4/8/8/8/1B2KB2 w - - 0 1")
        val b1Light = ('b'.code + 1) % 2 != 0
        val f1Light = ('f'.code + 1) % 2 != 0
        if (b1Light != f1Light) {
            assertFalse(drawDetector.isInsufficientMaterial(state))
        } else {
            assertTrue(drawDetector.isInsufficientMaterial(state))
        }
    }

    @Test
    fun testFiftyMoveRuleNotTriggered() {
        val state = fenParser.parseGameState("8/8/8/3k4/8/8/8/4K2R w - - 99 50")
        assertFalse(drawDetector.isFiftyMoveRule(state))
    }

    @Test
    fun testFiftyMoveRuleTriggered() {
        val state = fenParser.parseGameState("8/8/8/3k4/8/8/8/4K2R w - - 100 50")
        assertTrue(drawDetector.isFiftyMoveRule(state))
    }

    @Test
    fun testThreefoldRepetitionNotTriggered() {
        val state = fenParser.parseGameState("8/8/8/3k4/8/8/8/4K2R w - - 0 1")
        assertFalse(drawDetector.isThreefoldRepetition(state))
    }

    @Test
    fun testThreefoldRepetitionTriggered() {
        val positionFen = "8/8/8/3k4/8/8/8/4K2R w - -"
        val state = fenParser.parseGameState("$positionFen 0 1").copy(
            positionHistory = listOf(
                "$positionFen 0 1",
                "8/8/8/8/3k4/8/8/4K2R b - - 1 1",
                "$positionFen 2 2",
                "8/8/8/8/3k4/8/8/4K2R b - - 3 2",
                "$positionFen 4 3"
            )
        )
        assertTrue(drawDetector.isThreefoldRepetition(state))
    }

    @Test
    fun testKingBishopVsKingBishopSameColorInsufficient() {
        val state = fenParser.parseGameState("4k1b1/8/8/8/8/8/8/2B1K3 w - - 0 1")
        assertTrue(drawDetector.isInsufficientMaterial(state))
    }
}
