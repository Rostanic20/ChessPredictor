package com.chesspredictor.domain.entities

data class TimeManagementAnalysis(
    val totalGameTime: Long,
    val averageMoveTime: Long,
    val moveTimeAnalysis: List<MoveTimeAnalysis>,
    val timePressureIncidents: List<TimePressureIncident>,
    val timeUsagePattern: TimeUsagePattern,
    val recommendations: List<String>
)

data class MoveTimeAnalysis(
    val moveNumber: Int,
    val move: ChessMove,
    val timeSpent: Long,
    val positionComplexity: ComplexityLevel,
    val wasOptimal: Boolean,
    val suggestedTime: Long,
    val analysis: TimeAnalysisType
)

data class TimePressureIncident(
    val startMove: Int,
    val endMove: Int,
    val remainingTime: Long,
    val severity: TimePressureSeverity,
    val impactOnQuality: MoveQualityImpact
)

data class TimeUsagePattern(
    val openingTimePercentage: Float,
    val middlegameTimePercentage: Float,
    val endgameTimePercentage: Float,
    val criticalMomentsTimePercentage: Float,
    val patternType: TimePatternType
)

enum class TimeAnalysisType {
    TOO_FAST,
    TOO_SLOW,
    APPROPRIATE,
    UNDER_PRESSURE,
    CRITICAL_DECISION
}

enum class TimePressureSeverity {
    MILD,
    MODERATE,
    SEVERE,
    CRITICAL
}

data class MoveQualityImpact(
    val blunders: Int,
    val mistakes: Int,
    val inaccuracies: Int,
    val missedOpportunities: Int
)

enum class TimePatternType {
    BALANCED,
    FRONT_LOADED,
    BACK_LOADED,
    ERRATIC,
    RUSHED,
    OVERTHINKING
}

data class TimeControl(
    val initialTime: Long,
    val increment: Long,
    val type: TimeControlType
) {
    val label: String get() = "${initialTime / 60_000}+${increment / 1000}"

    companion object {
        fun of(initialMs: Long, incrementMs: Long): TimeControl {
            val estimatedMs = initialMs + 40 * incrementMs
            val type = when {
                estimatedMs < 180_000 -> TimeControlType.BULLET
                estimatedMs < 480_000 -> TimeControlType.BLITZ
                estimatedMs < 1_500_000 -> TimeControlType.RAPID
                else -> TimeControlType.CLASSICAL
            }
            return TimeControl(initialMs, incrementMs, type)
        }

        val PRESETS: List<TimeControl> = listOf(
            of(180_000, 2_000),
            of(300_000, 0),
            of(600_000, 0),
            of(900_000, 10_000),
            of(1_800_000, 0)
        )
    }
}

enum class TimeControlType {
    CLASSICAL,
    RAPID,
    BLITZ,
    BULLET,
    UNLIMITED
}
