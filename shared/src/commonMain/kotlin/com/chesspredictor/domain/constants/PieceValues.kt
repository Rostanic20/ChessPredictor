package com.chesspredictor.domain.constants

import com.chesspredictor.domain.entities.ChessPiece

object PieceValues {
    const val PAWN_VALUE = 100
    const val KNIGHT_VALUE = 300
    const val BISHOP_VALUE = 300
    const val ROOK_VALUE = 500
    const val QUEEN_VALUE = 900
    const val KING_VALUE = 10000

    const val PAWN_SIMPLE = 1
    const val KNIGHT_SIMPLE = 3
    const val BISHOP_SIMPLE = 3
    const val ROOK_SIMPLE = 5
    const val QUEEN_SIMPLE = 9
    const val KING_SIMPLE = 100

    fun getValue(piece: ChessPiece): Int {
        return when (piece) {
            is ChessPiece.Pawn -> PAWN_VALUE
            is ChessPiece.Knight -> KNIGHT_VALUE
            is ChessPiece.Bishop -> BISHOP_VALUE
            is ChessPiece.Rook -> ROOK_VALUE
            is ChessPiece.Queen -> QUEEN_VALUE
            is ChessPiece.King -> KING_VALUE
        }
    }

    fun getSimpleValue(piece: ChessPiece): Int {
        return when (piece) {
            is ChessPiece.Pawn -> PAWN_SIMPLE
            is ChessPiece.Knight -> KNIGHT_SIMPLE
            is ChessPiece.Bishop -> BISHOP_SIMPLE
            is ChessPiece.Rook -> ROOK_SIMPLE
            is ChessPiece.Queen -> QUEEN_SIMPLE
            is ChessPiece.King -> KING_SIMPLE
        }
    }

    fun simpleToStandard(simpleValue: Int): Int = simpleValue * 100

    fun standardToSimple(standardValue: Int): Int = standardValue / 100
}
