package com.chesspredictor.domain.entities

data class PositionComplexity(
    val sharpnessScore: Float,
    val tacticalComplexity: Float,
    val strategicComplexity: Float,
    val materialImbalance: Float,
    val kingDanger: KingDangerLevel,
    val criticalFactors: List<CriticalFactor>,
    val timeImportance: TimeImportance,
    val overallComplexity: ComplexityLevel
) {
    companion object {
        fun calculateOverallScore(
            sharpness: Float,
            tactical: Float,
            strategic: Float,
            imbalance: Float
        ): Float {
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
    SIMPLE,
    LOW,
    MODERATE,
    HIGH,
    EXTREME
}

data class KingDangerLevel(
    val whiteKingSafety: Float,
    val blackKingSafety: Float
) {
    fun getMoreDangerousKing(): ChessColor? {
        return when {
            whiteKingSafety < blackKingSafety - 20 -> ChessColor.WHITE
            blackKingSafety < whiteKingSafety - 20 -> ChessColor.BLACK
            else -> null
        }
    }
}

enum class CriticalFactor {
    KING_EXPOSED,
    TACTICAL_SHOTS,
    TIME_PRESSURE,
    MATERIAL_IMBALANCE,
    PAWN_BREAKS,
    PIECE_COORDINATION,
    ENDGAME_TRANSITION,
    FORCED_SEQUENCE,
    COUNTER_PLAY,
    POSITIONAL_SACRIFICE,
    FORTRESS_POTENTIAL,
    ZUGZWANG_POTENTIAL
}

enum class TimeImportance {
    CRITICAL,
    HIGH,
    MODERATE,
    LOW
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
    val moveOptions: Int,
    val forcedMoves: Int,
    val evaluationVolatility: Float
)

data class MaterialBalance(
    val whiteValue: Int,
    val blackValue: Int,
    val imbalanceType: ImbalanceType,
    val compensation: Float
)

enum class ImbalanceType {
    EQUAL,
    STANDARD,
    MINOR_EXCHANGE,
    QUEEN_SACRIFICE,
    UNUSUAL,
    OVERWHELMING
}
