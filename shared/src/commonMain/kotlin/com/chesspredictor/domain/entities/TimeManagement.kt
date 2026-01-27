package com.chesspredictor.domain.entities

/**
 * Represents time management analysis for a chess game.
 */
data class TimeManagementAnalysis(
    val totalGameTime: Long,
    val averageMoveTime: Long,
    val moveTimeAnalysis: List<MoveTimeAnalysis>,
    val timePressureIncidents: List<TimePressureIncident>,
    val timeUsagePattern: TimeUsagePattern,
    val recommendations: List<String>
)

/**
 * Analysis of time spent on a specific move.
 */
data class MoveTimeAnalysis(
    val moveNumber: Int,
    val move: ChessMove,
    val timeSpent: Long,
    val positionComplexity: ComplexityLevel,
    val wasOptimal: Boolean,
    val suggestedTime: Long,
    val analysis: TimeAnalysisType
)

/**
 * Represents a time pressure incident during the game.
 */
data class TimePressureIncident(
    val startMove: Int,
    val endMove: Int,
    val remainingTime: Long,
    val severity: TimePressureSeverity,
    val impactOnQuality: MoveQualityImpact
)

/**
 * Overall pattern of time usage throughout the game.
 */
data class TimeUsagePattern(
    val openingTimePercentage: Float,
    val middlegameTimePercentage: Float,
    val endgameTimePercentage: Float,
    val criticalMomentsTimePercentage: Float,
    val patternType: TimePatternType
)

/**
 * Types of time analysis for individual moves.
 */
enum class TimeAnalysisType {
    TOO_FAST,          // Spent too little time on a critical position
    TOO_SLOW,          // Spent too much time on a simple position
    APPROPRIATE,       // Good time allocation
    UNDER_PRESSURE,    // Made quickly due to time pressure
    CRITICAL_DECISION  // Important position requiring more time
}

/**
 * Severity levels of time pressure.
 */
enum class TimePressureSeverity {
    MILD,      // Still manageable
    MODERATE,  // Starting to affect decision quality
    SEVERE,    // Significantly impacting play
    CRITICAL   // Almost no time left
}

/**
 * Impact of time pressure on move quality.
 */
data class MoveQualityImpact(
    val blunders: Int,
    val mistakes: Int,
    val inaccuracies: Int,
    val missedOpportunities: Int
)

/**
 * Types of time usage patterns.
 */
enum class TimePatternType {
    BALANCED,           // Even time distribution
    FRONT_LOADED,      // Too much time in opening
    BACK_LOADED,       // Too much time in endgame
    ERRATIC,          // Inconsistent time usage
    RUSHED,           // Consistently too fast
    OVERTHINKING      // Consistently too slow
}

/**
 * Time control settings for a game.
 */
data class TimeControl(
    val initialTime: Long,        // Initial time in milliseconds
    val increment: Long,          // Increment per move in milliseconds
    val type: TimeControlType
)

/**
 * Types of time controls.
 */
enum class TimeControlType {
    CLASSICAL,    // 90+ minutes
    RAPID,        // 10-60 minutes
    BLITZ,        // 3-10 minutes
    BULLET,       // < 3 minutes
    UNLIMITED     // No time limit
}