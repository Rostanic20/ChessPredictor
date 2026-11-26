package com.chesspredictor.domain.entities

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EngineSettingsTest {

    private fun assertMatches(difficulty: EngineDifficulty, settings: EngineSettings) {
        assertEquals(difficulty, settings.difficulty)
        assertEquals(difficulty.depth, settings.analysisDepth)
        assertEquals(difficulty.timeMs, settings.timeLimit)
        assertEquals(difficulty.skillLevel, settings.skillLevel)
        assertEquals(difficulty.getDefaultBehaviorProfile(), settings.behaviorProfile)
    }

    @Test
    fun defaultSettingsMatchTheirDifficulty() {
        assertMatches(EngineDifficulty.CLUB_PLAYER, EngineSettings())
    }

    @Test
    fun withDifficultyReplacesEveryDifficultyDerivedValue() {
        val settings = EngineSettings().withDifficulty(EngineDifficulty.NOVICE)
        assertMatches(EngineDifficulty.NOVICE, settings)
    }

    @Test
    fun strongerPresetsSearchMoreAndErrLess() {
        val presets = EngineDifficulty.entries.filter { it != EngineDifficulty.CUSTOM }.sortedBy { it.eloRating }
        presets.zipWithNext().forEach { (weaker, stronger) ->
            assertTrue(stronger.skillLevel > weaker.skillLevel, "$stronger skill")
            assertTrue(stronger.depth >= weaker.depth, "$stronger depth")
            assertTrue(stronger.timeMs >= weaker.timeMs, "$stronger time")
            assertTrue(stronger.blunderRate < weaker.blunderRate, "$stronger blunders")
            assertTrue(stronger.inaccuracyRate < weaker.inaccuracyRate, "$stronger inaccuracies")
        }
    }

    @Test
    fun decodingOnlyADifficultyFillsTheRestFromIt() {
        val settings = Json.decodeFromString<EngineSettings>("""{"difficulty":"MASTER"}""")
        assertMatches(EngineDifficulty.MASTER, settings)
    }
}
