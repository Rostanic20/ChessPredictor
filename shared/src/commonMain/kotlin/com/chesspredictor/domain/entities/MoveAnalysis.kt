package com.chesspredictor.domain.entities

enum class MoveQuality {
    BRILLIANT,      // ♦ (Brilliant sacrifice/tactical shot)
    GREAT,          // ! (Great move, significantly better than alternatives)
    GOOD,           // No symbol (Solid, reasonable move)
    INACCURACY,     // ?! (Slight mistake, small evaluation loss)
    MISTAKE,        // ?? (Clear mistake, significant evaluation loss)
    BLUNDER         // ??? (Major blunder, game-changing mistake)
}

data class MoveAnalysis(
    val move: ChessMove,
    val quality: MoveQuality,
    val evaluationBefore: Float,  // Position eval before the move
    val evaluationAfter: Float,   // Position eval after the move
    val centipawnLoss: Int,       // How much eval was lost/gained
    val bestMove: String? = null, // What the engine thought was best
    val alternativeMoves: List<String> = emptyList(),
    val tacticalThemes: List<String> = emptyList(), // "Pin", "Fork", etc.
    val comment: String = ""      // Human-readable explanation
)

data class GameAnalysis(
    val moves: List<MoveAnalysis> = emptyList(),
    val accuracy: PlayerAccuracy = PlayerAccuracy(),
    val keyMoments: List<KeyMoment> = emptyList(),
    val gamePhase: GamePhase = GamePhase.OPENING,
    val result: GameResult? = null,
    val positionComplexities: List<PositionComplexity> = emptyList(),
    val averageComplexity: ComplexityLevel = ComplexityLevel.MODERATE
)

data class PlayerAccuracy(
    val white: Float = 0f,  // Accuracy percentage (0-100)
    val black: Float = 0f
)

data class KeyMoment(
    val moveNumber: Int,
    val description: String,
    val evaluationSwing: Float,
    val type: KeyMomentType
)

enum class KeyMomentType {
    BLUNDER,
    MISSED_WIN,
    BRILLIANT_MOVE,
    TURNING_POINT,
    GAME_DECIDING
}

enum class GamePhase {
    OPENING,     // First 10-15 moves
    MIDDLEGAME,  // Main battle
    ENDGAME      // Few pieces left
}

enum class GameResult {
    WHITE_WINS,
    BLACK_WINS,
    DRAW
}