package com.chesspredictor.domain.entities

sealed class ChessPiece(open val color: ChessColor) {
    data class Pawn(override val color: ChessColor) : ChessPiece(color)
    data class Knight(override val color: ChessColor) : ChessPiece(color)
    data class Bishop(override val color: ChessColor) : ChessPiece(color)
    data class Rook(override val color: ChessColor) : ChessPiece(color)
    data class Queen(override val color: ChessColor) : ChessPiece(color)
    data class King(override val color: ChessColor) : ChessPiece(color)
}