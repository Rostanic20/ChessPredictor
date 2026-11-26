package com.chesspredictor.domain.entities

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun decodingOnlyADifficultyFillsTheRestFromIt() {
        val settings = Json.decodeFromString<EngineSettings>("""{"difficulty":"MASTER"}""")
        assertMatches(EngineDifficulty.MASTER, settings)
    }
}
