package com.chesspredictor.domain.entities

data class ChessOpening(
    val name: String,
    val eco: String,
    val moves: List<String>,
    val description: String = "",
    val statistics: OpeningStatistics = OpeningStatistics(),
    val category: OpeningCategory = OpeningCategory.OTHER,
    val difficulty: OpeningDifficulty = OpeningDifficulty.INTERMEDIATE,
    val popularity: Float = 0.0f,
    val transpositions: List<List<String>> = emptyList(),
    val themes: List<OpeningTheme> = emptyList(),
    val parentOpening: String? = null,
    val isMainLine: Boolean = true
)

data class OpeningStatistics(
    val whiteWinRate: Float = 0.0f,
    val blackWinRate: Float = 0.0f,
    val drawRate: Float = 0.0f,
    val totalGames: Int = 0,
    val averageRating: Int = 0,
    val recentTrend: Float = 0.0f,
    val topPlayers: List<String> = emptyList()
)

data class OpeningInfo(
    val opening: ChessOpening?,
    val variation: String? = null,
    val moveNumber: Int = 0,
    val transposition: Boolean = false,
    val alternativeOrders: List<List<String>> = emptyList(),
    val nextPopularMoves: List<PopularMove> = emptyList(),
    val positionEvaluation: Float = 0.0f
)

data class PopularMove(
    val move: String,
    val frequency: Float,
    val performance: Float,
    val isTheoretical: Boolean = true
)

enum class OpeningCategory {
    KINGS_PAWN,
    QUEENS_PAWN,
    ENGLISH,
    RETI_SYSTEM,
    FLANK,
    IRREGULAR,
    OTHER
}

enum class OpeningDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

enum class OpeningTheme {
    TACTICAL,
    POSITIONAL,
    ATTACKING,
    SOLID,
    GAMBIT,
    HYPERMODERN,
    CLASSICAL,
    CONTROL_CENTER,
    FIANCHETTO,
    PAWN_STORM,
    PIECE_ACTIVITY,
    KING_SAFETY
}