package com.chesspredictor.domain.entities

enum class MoveQuality {
    BRILLIANT,
    GREAT,
    GOOD,
    INACCURACY,
    MISTAKE,
    BLUNDER
}

data class MoveAnalysis(
    val move: ChessMove,
    val quality: MoveQuality,
    val evaluationBefore: Float,
    val evaluationAfter: Float,
    val centipawnLoss: Int,
    val bestMove: String? = null,
    val alternativeMoves: List<String> = emptyList(),
    val tacticalThemes: List<String> = emptyList(),
    val comment: String = ""
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
    val white: Float = 0f,
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
    OPENING,
    MIDDLEGAME,
    ENDGAME
}

enum class GameResult {
    WHITE_WINS,
    BLACK_WINS,
    DRAW
}
