package com.chesspredictor.domain.entities

import kotlinx.serialization.Serializable

/**
 * Human-like behavior profile separate from ELO strength
 * Based on 2024 research from Maia Chess and chess robot emotional systems
 */
@Serializable
data class HumanBehaviorProfile(
    // Thinking Time Personality (affects display timing, not analysis)
    val thinkingSpeed: Float = 0.5f, // 0.0 = very fast, 1.0 = very slow
    val complexityReaction: Float = 0.5f, // How much thinking time varies with position complexity
    val timeConsistency: Float = 0.7f, // How consistent timing is (0.0 = erratic, 1.0 = consistent)
    
    // Emotional Response System
    val emotionalExpressiveness: Float = 0.5f, // How much emotion is shown
    val confidenceLevel: Float = 0.6f, // Base confidence level
    val pressureResponse: Float = 0.5f, // How well handles pressure situations
    
    // Playing Style Recognition (Maia-style individual patterns)
    val tacticalAggression: Float = 0.5f, // Preference for tactical vs positional play
    val riskTolerance: Float = 0.5f, // Willingness to take risks
    val openingPreference: String = "BALANCED", // AGGRESSIVE, SOLID, BALANCED
    
    // Communication Style
    val chattiness: Float = 0.0f, // How often provides commentary (disabled by default)
    val teachingMode: Boolean = false, // Explains moves and concepts
    val showThoughts: Boolean = true, // Shows "thinking" process
    
    // Individual Quirks (makes each opponent unique)
    val favoriteOpenings: List<String> = listOf("e4", "d4", "Nf3"),
    val weaknessesAware: Boolean = true, // Admits to mistakes
    val celebratesGoodMoves: Boolean = true
) {
    companion object {
        // Predefined personality archetypes
        val BEGINNER_ENTHUSIAST = HumanBehaviorProfile(
            thinkingSpeed = 0.7f,
            emotionalExpressiveness = 0.8f,
            chattiness = 0.0f,
            teachingMode = false,
            openingPreference = "AGGRESSIVE"
        )
        
        val CALM_POSITIONAL = HumanBehaviorProfile(
            thinkingSpeed = 0.3f,
            timeConsistency = 0.9f,
            tacticalAggression = 0.2f,
            emotionalExpressiveness = 0.3f,
            openingPreference = "SOLID"
        )
        
        val TACTICAL_FIGHTER = HumanBehaviorProfile(
            thinkingSpeed = 0.4f,
            tacticalAggression = 0.9f,
            riskTolerance = 0.8f,
            emotionalExpressiveness = 0.7f,
            openingPreference = "AGGRESSIVE"
        )
        
        val EXPERIENCED_TEACHER = HumanBehaviorProfile(
            thinkingSpeed = 0.2f,
            timeConsistency = 0.8f,
            teachingMode = false,
            chattiness = 0.0f,
            showThoughts = true
        )
    }
}

/**
 * Emotional responses during gameplay
 */
@Serializable
enum class EmotionalState {
    CONFIDENT,
    WORRIED,
    EXCITED,
    FRUSTRATED,
    SURPRISED,
    FOCUSED,
    PRESSURED,
    SATISFIED
}

/**
 * Commentary types for human-like interaction
 */
@Serializable
enum class CommentaryType {
    MOVE_EXPLANATION,
    POSITION_ASSESSMENT,
    EMOTIONAL_REACTION,
    TEACHING_MOMENT,
    TACTICAL_OBSERVATION,
    STRATEGIC_PLAN,
    TIME_PRESSURE_COMMENT,
    MISTAKE_ACKNOWLEDGMENT
}

/**
 * Human-like move commentary
 */
@Serializable
data class MoveCommentary(
    val text: String,
    val type: CommentaryType,
    val emotionalState: EmotionalState,
    val confidence: Float // 0.0-1.0
)