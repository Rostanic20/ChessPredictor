package com.chesspredictor.data.datasources

import com.chesspredictor.domain.entities.EngineDifficulty
import com.chesspredictor.domain.entities.EngineSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EngineStrengthTest {

    @Test
    fun calibratedLevelsUseUciElo() {
        listOf(EngineDifficulty.INTERMEDIATE, EngineDifficulty.CLUB_PLAYER, EngineDifficulty.MASTER).forEach { difficulty ->
            val strength = EngineStrength.of(EngineSettings(difficulty = difficulty))
            assertTrue(strength.limitStrength, difficulty.name)
            assertEquals(difficulty.eloRating, strength.elo, difficulty.name)
        }
    }

    @Test
    fun maximumPlaysAtFullStrength() {
        val strength = EngineStrength.of(EngineSettings(difficulty = EngineDifficulty.MAXIMUM))
        assertFalse(strength.limitStrength)
        assertEquals(20, strength.skillLevel)
    }

    @Test
    fun levelsBelowStockfishsFloorGetShallowerSearches() {
        val depths = listOf(EngineDifficulty.NOVICE, EngineDifficulty.BEGINNER, EngineDifficulty.CASUAL).map {
            val strength = EngineStrength.of(EngineSettings(difficulty = it))
            assertFalse(strength.limitStrength, it.name)
            assertEquals(0, strength.skillLevel, it.name)
            strength.depth
        }
        assertEquals(listOf(1, 3, 5), depths)
    }

    @Test
    fun uciCommandsMatchTheStrength() {
        val limited = EngineStrength(limitStrength = true, elo = 1600, skillLevel = 20, depth = 14)
        assertEquals(
            listOf(
                "setoption name UCI_LimitStrength value true",
                "setoption name UCI_Elo value 1600",
                "setoption name Skill Level value 20",
                "setoption name MultiPV value 1"
            ),
            limited.setupCommands(multiPV = 1)
        )
        val unlimited = EngineStrength(limitStrength = false, elo = null, skillLevel = 0, depth = 3)
        assertFalse(unlimited.setupCommands(multiPV = 1).any { "UCI_Elo" in it })

        assertEquals("go depth 14 movetime 300", limited.goCommand(150))
        assertEquals("go depth 14 movetime 5000", limited.goCommand(9_000))
        assertEquals("go depth 14 movetime 300 nodes 100000", limited.goCommand(50))
        assertEquals("go depth 14", limited.goCommand(0).substringBefore(" nodes"))
    }

    @Test
    fun weakerRequestsLowerTheTargetElo() {
        val settings = EngineSettings(difficulty = EngineDifficulty.EXPERT)
        val weaker = EngineStrength.of(settings.copy(skillLevel = settings.skillLevel - 3))
        assertEquals(EngineDifficulty.EXPERT.eloRating - 300, weaker.elo)

        val muchWeaker = EngineStrength.of(settings.copy(skillLevel = settings.skillLevel - 8))
        assertFalse(muchWeaker.limitStrength)
        assertEquals(5, muchWeaker.depth)
    }
}
