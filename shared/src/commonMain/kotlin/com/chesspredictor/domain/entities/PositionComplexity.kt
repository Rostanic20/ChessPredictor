package com.chesspredictor.domain.entities

data class PositionComplexity(
    val sharpnessScore: Float,           // 0-100: How critical each move is
    val tacticalComplexity: Float,       // 0-100: Number of tactical possibilities
    val strategicComplexity: Float,      // 0-100: Long-term planning requirements
    val materialImbalance: Float,        // 0-100: Unusual material distribution
    val kingDanger: KingDangerLevel,     // Safety assessment for both kings
    val criticalFactors: List<CriticalFactor>,
    val timeImportance: TimeImportance,  // How important time is in this position
    val overallComplexity: ComplexityLevel
) {
    companion object {
        fun calculateOverallScore(
            sharpness: Float,
            tactical: Float,
            strategic: Float,
            imbalance: Float
        ): Float {
            // Weighted average with sharpness having highest weight
            return (sharpness * 0.4f + tactical * 0.3f + strategic * 0.2f + imbalance * 0.1f)
        }
        
        fun getComplexityLevel(score: Float): ComplexityLevel {
            return when {
                score >= 80 -> ComplexityLevel.EXTREME
                score >= 60 -> ComplexityLevel.HIGH
                score >= 40 -> ComplexityLevel.MODERATE
                score >= 20 -> ComplexityLevel.LOW
                else -> ComplexityLevel.SIMPLE
            }
        }
    }
}

enum class ComplexityLevel {
    SIMPLE,      // Straightforward positions, obvious moves
    LOW,         // Some calculation needed but manageable
    MODERATE,    // Multiple reasonable options, requires thought
    HIGH,        // Complex tactics or deep calculation needed
    EXTREME      // Critical positions, one mistake loses
}

data class KingDangerLevel(
    val whiteKingSafety: Float,  // 0-100 (100 = completely safe)
    val blackKingSafety: Float   // 0-100 (100 = completely safe)
) {
    fun getMoreDangerousKing(): ChessColor? {
        return when {
            whiteKingSafety < blackKingSafety - 20 -> ChessColor.WHITE
            blackKingSafety < whiteKingSafety - 20 -> ChessColor.BLACK
            else -> null // Both relatively equal
        }
    }
}

enum class CriticalFactor {
    KING_EXPOSED,           // King safety is compromised
    TACTICAL_SHOTS,         // Multiple tactics available
    TIME_PRESSURE,          // Low on time in sharp position
    MATERIAL_IMBALANCE,     // Unusual material (Q vs RR, etc)
    PAWN_BREAKS,           // Critical pawn advances available
    PIECE_COORDINATION,     // Pieces working together for attack
    ENDGAME_TRANSITION,    // About to transition to endgame
    FORCED_SEQUENCE,       // Only moves or forced line
    COUNTER_PLAY,          // Both sides have threats
    POSITIONAL_SACRIFICE,  // Material sacrifice for compensation
    FORTRESS_POTENTIAL,    // Defensive setup possibility
    ZUGZWANG_POTENTIAL     // Move compulsion issues
}

enum class TimeImportance {
    CRITICAL,    // Every second counts, sharp tactical position
    HIGH,        // Time important but not immediately critical
    MODERATE,    // Normal time importance
    LOW          // Simple position, time less relevant
}

data class ComplexityFactors(
    val piecesAttackingKing: Int,
    val hangingPieces: Int,
    val pinnedPieces: Int,
    val possibleTactics: Int,
    val pawnBreaks: Int,
    val openFiles: Int,
    val pieceActivity: Float,
    val centerControl: Float,
    val materialBalance: MaterialBalance,
    val moveOptions: Int,         // Number of reasonable moves
    val forcedMoves: Int,        // Number of forced/only moves
    val evaluationVolatility: Float  // How much eval changes with different moves
)

data class MaterialBalance(
    val whiteValue: Int,
    val blackValue: Int,
    val imbalanceType: ImbalanceType,
    val compensation: Float  // 0-100: positional compensation for material deficit
)

enum class ImbalanceType {
    EQUAL,
    STANDARD,        // Normal material advantage
    MINOR_EXCHANGE,  // Rook for minor piece
    QUEEN_SACRIFICE, // Queen for other pieces
    UNUSUAL,         // Strange material distribution
    OVERWHELMING     // Huge material advantage
}