package com.chesspredictor.domain.usecases

import com.chesspredictor.domain.entities.ExportFormat
import com.chesspredictor.domain.entities.OpeningInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpeningDetectorTest {
    private val detector = OpeningDetector()
    private val importer = ImportExportGameUseCase()

    private fun detect(moves: String): OpeningInfo {
        val game = importer.importGame(moves, ExportFormat.PGN).getOrThrow().gameState
        return detector.detectOpening(game.moveHistory)
    }

    @Test
    fun sicilianBeforeAnyVariationIsOnlyNamedAsTheFamily() {
        val info = detect("1. e4 c5")
        assertEquals("Sicilian", info.opening?.name)
        assertEquals("B20", info.opening?.eco)
    }

    @Test
    fun completedLineIsNamedInFull() {
        val info = detect("1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 a6")
        assertEquals("Sicilian: Najdorf Variation", info.opening?.name)
        assertFalse(info.transposition)
    }

    @Test
    fun lineStaysNamedAfterTheGameLeavesBook() {
        val info = detect("1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 a6 6. h3 e5")
        assertEquals("Sicilian: Najdorf Variation", info.opening?.name)
    }

    @Test
    fun ambiguousStartGetsNoName() {
        assertNull(detect("1. d4 Nf6").opening)
    }

    @Test
    fun deviationKeepsTheSharedFamily() {
        assertEquals("Ruy Lopez", detect("1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. O-O").opening?.name)
    }

    @Test
    fun knightShuffleIsNotTheEnglish() {
        assertEquals("Réti Opening", detect("1. Nf3 Nf6 2. Ng1 Ng8").opening?.name)
    }

    @Test
    fun transposedMoveOrderReachesTheSameOpening() {
        val info = detect("1. c4 e6 2. Nc3 Nf6 3. d4 Bb4 4. Qc2")
        assertEquals("Nimzo-Indian Defense: Classical Variation", info.opening?.name)
        assertTrue(info.transposition)
    }
}
