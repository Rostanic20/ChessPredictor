package com.chesspredictor.domain.utils

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.Square

object ChessConstants {
    val FILES = 'a'..'h'
    val RANKS = 1..8
    const val BOARD_SIZE = 8
    const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
}

object ChessUtils {
    fun pieceToChar(piece: ChessPiece): Char = when (piece) {
        is ChessPiece.Pawn -> if (piece.color == ChessColor.WHITE) 'P' else 'p'
        is ChessPiece.Knight -> if (piece.color == ChessColor.WHITE) 'N' else 'n'
        is ChessPiece.Bishop -> if (piece.color == ChessColor.WHITE) 'B' else 'b'
        is ChessPiece.Rook -> if (piece.color == ChessColor.WHITE) 'R' else 'r'
        is ChessPiece.Queen -> if (piece.color == ChessColor.WHITE) 'Q' else 'q'
        is ChessPiece.King -> if (piece.color == ChessColor.WHITE) 'K' else 'k'
    }
    
    fun charToPiece(char: Char): ChessPiece? = when (char.lowercaseChar()) {
        'p' -> ChessPiece.Pawn(if (char.isUpperCase()) ChessColor.WHITE else ChessColor.BLACK)
        'n' -> ChessPiece.Knight(if (char.isUpperCase()) ChessColor.WHITE else ChessColor.BLACK)
        'b' -> ChessPiece.Bishop(if (char.isUpperCase()) ChessColor.WHITE else ChessColor.BLACK)
        'r' -> ChessPiece.Rook(if (char.isUpperCase()) ChessColor.WHITE else ChessColor.BLACK)
        'q' -> ChessPiece.Queen(if (char.isUpperCase()) ChessColor.WHITE else ChessColor.BLACK)
        'k' -> ChessPiece.King(if (char.isUpperCase()) ChessColor.WHITE else ChessColor.BLACK)
        else -> null
    }
    
    fun pieceToUnicode(piece: ChessPiece): String = when (piece) {
        is ChessPiece.Pawn -> if (piece.color == ChessColor.WHITE) "♙" else "♟"
        is ChessPiece.Knight -> if (piece.color == ChessColor.WHITE) "♘" else "♞"
        is ChessPiece.Bishop -> if (piece.color == ChessColor.WHITE) "♗" else "♝"
        is ChessPiece.Rook -> if (piece.color == ChessColor.WHITE) "♖" else "♜"
        is ChessPiece.Queen -> if (piece.color == ChessColor.WHITE) "♕" else "♛"
        is ChessPiece.King -> if (piece.color == ChessColor.WHITE) "♔" else "♚"
    }
    
    fun isValidSquare(file: Char, rank: Int): Boolean {
        return file in ChessConstants.FILES && rank in ChessConstants.RANKS
    }
    
    fun algebraicToSquare(algebraic: String): Square? {
        if (algebraic.length != 2) return null
        val file = algebraic[0]
        val rank = algebraic[1].digitToIntOrNull() ?: return null
        return if (isValidSquare(file, rank)) Square(file, rank) else null
    }
    
    fun squareToAlgebraic(square: Square): String = "${square.file}${square.rank}"
}