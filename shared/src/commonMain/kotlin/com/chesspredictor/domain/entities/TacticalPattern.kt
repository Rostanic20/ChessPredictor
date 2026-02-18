package com.chesspredictor.domain.entities

data class TacticalPattern(
    val type: PatternType,
    val squares: List<Square>,
    val attackingPieces: List<Pair<Square, ChessPiece>>,
    val targetPieces: List<Pair<Square, ChessPiece>>,
    val valuablePiece: Pair<Square, ChessPiece>? = null,
    val side: ChessColor,
    val value: Int,
    val severity: PatternSeverity,
    val isOpportunity: Boolean,
    val description: String
)

enum class PatternType {
    PIN,
    ABSOLUTE_PIN,
    RELATIVE_PIN,

    FORK,
    KNIGHT_FORK,
    PAWN_FORK,
    ROYAL_FORK,
    DOUBLE_ATTACK,

    SKEWER,
    ABSOLUTE_SKEWER,
    RELATIVE_SKEWER,

    DISCOVERED_ATTACK,
    DISCOVERED_CHECK,
    DOUBLE_CHECK,

    HANGING_PIECE,
    TRAPPED_PIECE,
    OVERLOADED_PIECE,

    BACK_RANK_MATE,
    SMOTHERED_MATE,

    REMOVE_DEFENDER,
    DEFLECTION,
    DECOY,
    INTERFERENCE,
    ZWISCHENZUG,

    EN_PASSANT_CAPTURE,
    PAWN_BREAKTHROUGH,
    PASSED_PAWN,

    PERPETUAL_CHECK,
    FORTRESS,
    ZUGZWANG
}

enum class PatternSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class TacticalAnalysis(
    val patterns: List<TacticalPattern>,
    val tacticalComplexity: Float,
    val criticalPatterns: List<TacticalPattern>,
    val opportunities: List<TacticalPattern>,
    val threats: List<TacticalPattern>,
    val summary: String = generateTacticalSummary(patterns)
) {
    companion object {
        fun empty() = TacticalAnalysis(
            patterns = emptyList(),
            tacticalComplexity = 0f,
            criticalPatterns = emptyList(),
            opportunities = emptyList(),
            threats = emptyList(),
            summary = "No tactical patterns detected"
        )
    }
}

private fun generateTacticalSummary(patterns: List<TacticalPattern>): String {
    if (patterns.isEmpty()) return "Position is tactically quiet"

    val criticalCount = patterns.count { it.severity == PatternSeverity.CRITICAL }
    val opportunities = patterns.count { it.isOpportunity }
    val threats = patterns.count { !it.isOpportunity }

    val parts = mutableListOf<String>()

    if (criticalCount > 0) {
        parts.add("$criticalCount critical tactical pattern${if (criticalCount > 1) "s" else ""}")
    }

    if (opportunities > 0) {
        parts.add("$opportunities tactical opportunit${if (opportunities > 1) "ies" else "y"}")
    }

    if (threats > 0) {
        parts.add("$threats threat${if (threats > 1) "s" else ""}")
    }

    val mostImportant = patterns.maxByOrNull {
        it.severity.ordinal * 1000 + it.value
    }

    mostImportant?.let {
        parts.add("Key: ${it.description}")
    }

    return parts.joinToString(". ")
}
