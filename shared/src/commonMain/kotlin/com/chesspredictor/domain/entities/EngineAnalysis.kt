package com.chesspredictor.domain.entities

data class EngineAnalysis(
    val bestMove: ChessMove,
    val evaluation: EngineEvaluation,
    val principalVariation: List<String> = emptyList(),
    val depth: Int = 0,
    val nodes: Long = 0,
    val time: Long = 0
)

data class EngineEvaluation(
    val score: Double, // Score in pawns (positive = white advantage, negative = black advantage)
    val mate: Int? = null, // Number of moves to mate (positive = white mates, negative = black mates)
    val isMateScore: Boolean = false
) {
    fun getDisplayScore(fromPerspective: ChessColor): String {
        return when {
            mate != null -> {
                val movesToMate = kotlin.math.abs(mate)
                if ((mate > 0 && fromPerspective == ChessColor.WHITE) || 
                    (mate < 0 && fromPerspective == ChessColor.BLACK)) {
                    "M$movesToMate"
                } else {
                    "-M$movesToMate"
                }
            }
            else -> {
                val adjustedScore = if (fromPerspective == ChessColor.BLACK) -score else score
                val rounded = kotlin.math.round(adjustedScore * 100) / 100.0
                rounded.toString()
            }
        }
    }
}