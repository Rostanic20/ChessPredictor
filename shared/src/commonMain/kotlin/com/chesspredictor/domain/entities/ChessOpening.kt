package com.chesspredictor.domain.entities

data class ChessOpening(
    val name: String,
    val eco: String, // Encyclopedia of Chess Openings code
    val moves: List<String>, // Move sequence in algebraic notation
    val description: String = "",
    val statistics: OpeningStatistics = OpeningStatistics(),
    val category: OpeningCategory = OpeningCategory.OTHER,
    val difficulty: OpeningDifficulty = OpeningDifficulty.INTERMEDIATE,
    val popularity: Float = 0.0f, // Percentage of games featuring this opening
    val transpositions: List<List<String>> = emptyList(), // Alternative move orders
    val themes: List<OpeningTheme> = emptyList(),
    val parentOpening: String? = null, // ECO code of parent opening
    val isMainLine: Boolean = true
)

data class OpeningStatistics(
    val whiteWinRate: Float = 0.0f,      // Percentage (0-100)
    val blackWinRate: Float = 0.0f,      // Percentage (0-100) 
    val drawRate: Float = 0.0f,          // Percentage (0-100)
    val totalGames: Int = 0,             // Number of games in database
    val averageRating: Int = 0,          // Average player rating
    val recentTrend: Float = 0.0f,       // Popularity change (+/- percentage)
    val topPlayers: List<String> = emptyList() // Notable players who play this
)

data class OpeningInfo(
    val opening: ChessOpening?,
    val variation: String? = null,
    val moveNumber: Int = 0,
    val transposition: Boolean = false,  // Was this reached via transposition?
    val alternativeOrders: List<List<String>> = emptyList(),
    val nextPopularMoves: List<PopularMove> = emptyList(),
    val positionEvaluation: Float = 0.0f
)

data class PopularMove(
    val move: String,
    val frequency: Float,        // Percentage of games continuing with this move
    val performance: Float,      // Win rate after this move
    val isTheoretical: Boolean = true
)

enum class OpeningCategory {
    KINGS_PAWN,      // 1.e4
    QUEENS_PAWN,     // 1.d4
    ENGLISH,         // 1.c4
    RETI_SYSTEM,     // 1.Nf3
    FLANK,           // Other first moves
    IRREGULAR,       // Unusual openings
    OTHER
}

enum class OpeningDifficulty {
    BEGINNER,        // Simple, easy to understand
    INTERMEDIATE,    // Standard complexity
    ADVANCED,        // Complex theory, sharp lines
    EXPERT          // Highly theoretical, expert level
}

enum class OpeningTheme {
    TACTICAL,        // Sharp, tactical positions
    POSITIONAL,      // Strategic, positional play
    ATTACKING,       // Quick attacks on the king
    SOLID,           // Safe, solid development
    GAMBIT,          // Material sacrifice for initiative
    HYPERMODERN,     // Hypermodern principles
    CLASSICAL,       // Classical development
    CONTROL_CENTER,  // Direct central control
    FIANCHETTO,      // Bishop fianchetto
    PAWN_STORM,      // Pawn advances
    PIECE_ACTIVITY,  // Focus on piece development
    KING_SAFETY      // Emphasis on king safety
}