package com.chesspredictor.domain.entities

import kotlinx.serialization.Serializable

@Serializable
data class EngineSettings(
    val difficulty: EngineDifficulty = EngineDifficulty.CLUB_PLAYER,
    val analysisDepth: Int = 15,
    val timeLimit: Int = 1000,
    val threads: Int = 1,
    val hashSize: Int = 16,
    val skillLevel: Int = 10,
    val useBook: Boolean = true,
    val contempt: Int = 0,
    val multiPV: Int = 1,
    val humanStyle: Boolean = true,
    val behaviorProfile: HumanBehaviorProfile = HumanBehaviorProfile()
) {
    val blunderProbability: Float get() = difficulty.blunderRate
    val inaccuracyRate: Float get() = difficulty.inaccuracyRate
    val openingBookUsage: Float get() = difficulty.openingBookUsage
    val endgameAccuracy: Float get() = difficulty.endgameAccuracy
}

@Serializable
enum class EngineDifficulty(
    val displayName: String,
    val eloRating: Int,
    val skillLevel: Int,
    val depth: Int,
    val timeMs: Int,
    val blunderRate: Float,
    val inaccuracyRate: Float,
    val description: String,
    val openingBookUsage: Float = 0.8f,
    val endgameAccuracy: Float = 0.9f
) {
    NOVICE("Novice (800 ELO)", 800, 1, 6, 500, 0.18f, 0.35f, "Beginner level with frequent mistakes"),
    BEGINNER("Beginner (1000 ELO)", 1000, 3, 8, 700, 0.12f, 0.25f, "Learning player, makes obvious errors"), 
    CASUAL("Casual (1200 ELO)", 1200, 5, 10, 900, 0.08f, 0.19f, "Recreational player level"),
    INTERMEDIATE("Intermediate (1400 ELO)", 1400, 7, 12, 1200, 0.06f, 0.15f, "Club beginner strength"),
    CLUB_PLAYER("Club Player (1600 ELO)", 1600, 9, 14, 1500, 0.04f, 0.12f, "Average club player"),
    STRONG_CLUB("Strong Club (1800 ELO)", 1800, 11, 16, 1800, 0.03f, 0.09f, "Strong club player"),
    EXPERT("Expert (2000 ELO)", 2000, 13, 18, 2200, 0.02f, 0.07f, "Expert level player"),
    MASTER("Master (2200 ELO)", 2200, 15, 20, 2600, 0.01f, 0.04f, "Master strength"),
    MAXIMUM("Maximum (2400+ ELO)", 2400, 18, 22, 3000, 0.005f, 0.02f, "Near maximum engine strength"),
    CUSTOM("Custom", 1500, 10, 12, 1500, 0.05f, 0.15f, "Configure your own settings");
    
    fun getDefaultBehaviorProfile(): HumanBehaviorProfile {
        return when (this) {
            NOVICE, BEGINNER -> HumanBehaviorProfile.BEGINNER_ENTHUSIAST
            CASUAL, INTERMEDIATE -> HumanBehaviorProfile(
                thinkingSpeed = 0.6f,
                emotionalExpressiveness = 0.6f,
                chattiness = 0.0f,
                tacticalAggression = 0.4f
            )
            CLUB_PLAYER, STRONG_CLUB -> HumanBehaviorProfile.CALM_POSITIONAL
            EXPERT, MASTER -> HumanBehaviorProfile.EXPERIENCED_TEACHER
            MAXIMUM -> HumanBehaviorProfile(
                thinkingSpeed = 0.1f,
                timeConsistency = 0.95f,
                emotionalExpressiveness = 0.2f,
                chattiness = 0.0f
            )
            CUSTOM -> HumanBehaviorProfile()
        }
    }
}