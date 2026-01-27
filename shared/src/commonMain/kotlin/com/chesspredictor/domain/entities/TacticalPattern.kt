package com.chesspredictor.domain.entities

/**
 * Represents a tactical pattern found in a position
 */
data class TacticalPattern(
    val type: PatternType,
    val squares: List<Square>,
    val attackingPieces: List<Pair<Square, ChessPiece>>,
    val targetPieces: List<Pair<Square, ChessPiece>>,
    val valuablePiece: Pair<Square, ChessPiece>? = null,
    val side: ChessColor, // Which side can execute the tactic
    val value: Int, // Material value of the tactic
    val severity: PatternSeverity,
    val isOpportunity: Boolean, // true if it's our turn and we can execute it
    val description: String
)

/**
 * Types of tactical patterns
 */
enum class PatternType {
    // Pin patterns
    PIN,
    ABSOLUTE_PIN, // Pinned to king
    RELATIVE_PIN, // Pinned to valuable piece
    
    // Fork patterns
    FORK,
    KNIGHT_FORK,
    PAWN_FORK,
    ROYAL_FORK, // Fork involving king
    DOUBLE_ATTACK,
    
    // Skewer patterns
    SKEWER,
    ABSOLUTE_SKEWER, // King must move
    RELATIVE_SKEWER, // Valuable piece should move
    
    // Discovery patterns
    DISCOVERED_ATTACK,
    DISCOVERED_CHECK,
    DOUBLE_CHECK,
    
    // Piece vulnerability
    HANGING_PIECE,
    TRAPPED_PIECE,
    OVERLOADED_PIECE,
    
    // Mate patterns
    BACK_RANK_MATE,
    SMOTHERED_MATE,
    
    // Advanced tactics
    REMOVE_DEFENDER,
    DEFLECTION,
    DECOY,
    INTERFERENCE,
    ZWISCHENZUG, // In-between move
    
    // Pawn tactics
    EN_PASSANT_CAPTURE,
    PAWN_BREAKTHROUGH,
    PASSED_PAWN,
    
    // Other patterns
    PERPETUAL_CHECK,
    FORTRESS,
    ZUGZWANG
}

/**
 * Severity levels for tactical patterns
 */
enum class PatternSeverity {
    LOW,      // Minor tactical element
    MEDIUM,   // Significant but not game-changing
    HIGH,     // Major tactical opportunity/threat
    CRITICAL  // Game-deciding or involves king safety
}

/**
 * Complete tactical analysis of a position
 */
data class TacticalAnalysis(
    val patterns: List<TacticalPattern>,
    val tacticalComplexity: Float, // 0-100 score
    val criticalPatterns: List<TacticalPattern>,
    val opportunities: List<TacticalPattern>, // Tactics we can execute
    val threats: List<TacticalPattern>, // Tactics opponent can execute
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

/**
 * Generates a human-readable summary of tactical patterns
 */
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
    
    // Highlight most important pattern
    val mostImportant = patterns.maxByOrNull { 
        it.severity.ordinal * 1000 + it.value 
    }
    
    mostImportant?.let {
        parts.add("Key: ${it.description}")
    }
    
    return parts.joinToString(". ")
}