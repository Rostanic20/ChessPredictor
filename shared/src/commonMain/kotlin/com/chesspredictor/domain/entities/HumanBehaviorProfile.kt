package com.chesspredictor.domain.entities

import kotlinx.serialization.Serializable

@Serializable
data class HumanBehaviorProfile(
    val thinkingSpeed: Float = 0.5f,
    val complexityReaction: Float = 0.5f,
    val timeConsistency: Float = 0.7f,

    val emotionalExpressiveness: Float = 0.5f,
    val confidenceLevel: Float = 0.6f,
    val pressureResponse: Float = 0.5f,

    val tacticalAggression: Float = 0.5f,
    val riskTolerance: Float = 0.5f,
    val openingPreference: String = "BALANCED",

    val chattiness: Float = 0.0f,
    val teachingMode: Boolean = false,
    val showThoughts: Boolean = true,

    val favoriteOpenings: List<String> = listOf("e4", "d4", "Nf3"),
    val weaknessesAware: Boolean = true,
    val celebratesGoodMoves: Boolean = true
) {
    companion object {
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

@Serializable
data class MoveCommentary(
    val text: String,
    val type: CommentaryType,
    val emotionalState: EmotionalState,
    val confidence: Float
)
