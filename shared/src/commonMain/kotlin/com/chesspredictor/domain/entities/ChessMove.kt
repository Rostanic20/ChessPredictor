package com.chesspredictor.domain.entities

data class ChessMove(
    val from: Square,
    val to: Square,
    val piece: ChessPiece,
    val capturedPiece: ChessPiece? = null,
    val promotion: ChessPiece? = null,
    val evaluation: Float = 0f,
    val isCheckmate: Boolean = false,
    val isCheck: Boolean = false
) {
    fun toUci(): String = "$from$to" + when (promotion) {
        is ChessPiece.Queen -> "q"
        is ChessPiece.Rook -> "r"
        is ChessPiece.Bishop -> "b"
        is ChessPiece.Knight -> "n"
        else -> ""
    }
}
