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
)