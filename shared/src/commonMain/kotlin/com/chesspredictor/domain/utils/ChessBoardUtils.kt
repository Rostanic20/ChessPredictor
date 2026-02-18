package com.chesspredictor.domain.utils

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.entities.opposite

object ChessBoardUtils {
    
    fun findKingSquare(board: Map<Square, ChessPiece>, color: ChessColor): Square? {
        return board.entries.firstOrNull { (_, piece) ->
            piece is ChessPiece.King && piece.color == color
        }?.key
    }
    
    fun findKings(board: Map<Square, ChessPiece>): Pair<Square?, Square?> {
        var whiteKing: Square? = null
        var blackKing: Square? = null
        
        for ((square, piece) in board) {
            if (piece is ChessPiece.King) {
                when (piece.color) {
                    ChessColor.WHITE -> whiteKing = square
                    ChessColor.BLACK -> blackKing = square
                }
                if (whiteKing != null && blackKing != null) break
            }
        }
        
        return Pair(whiteKing, blackKing)
    }
    
    inline fun <reified T : ChessPiece> getPiecesOfType(
        board: Map<Square, ChessPiece>,
        color: ChessColor? = null
    ): List<Pair<Square, T>> {
        return board.entries.mapNotNull { (square, piece) ->
            if (piece is T && (color == null || piece.color == color)) {
                square to piece
            } else null
        }
    }
    
    fun countPieces(board: Map<Square, ChessPiece>): PieceCount {
        val counts = mutableMapOf<ChessColor, MutableMap<String, Int>>(
            ChessColor.WHITE to mutableMapOf(),
            ChessColor.BLACK to mutableMapOf()
        )

        board.forEach { (_, piece) ->
            val pieceType = when (piece) {
                is ChessPiece.Pawn -> "pawn"
                is ChessPiece.Knight -> "knight"
                is ChessPiece.Bishop -> "bishop"
                is ChessPiece.Rook -> "rook"
                is ChessPiece.Queen -> "queen"
                is ChessPiece.King -> "king"
            }
            val colorCounts = counts.getValue(piece.color)
            colorCounts[pieceType] = (colorCounts[pieceType] ?: 0) + 1
        }

        return PieceCount(counts)
    }
    
    fun getMaterialBalance(board: Map<Square, ChessPiece>): MaterialInfo {
        var whiteValue = 0
        var blackValue = 0
        
        board.forEach { (_, piece) ->
            val value = com.chesspredictor.domain.constants.PieceValues.getValue(piece)
            if (piece !is ChessPiece.King) {
                when (piece.color) {
                    ChessColor.WHITE -> whiteValue += value
                    ChessColor.BLACK -> blackValue += value
                }
            }
        }
        
        return MaterialInfo(
            whiteMaterial = whiteValue,
            blackMaterial = blackValue,
            difference = whiteValue - blackValue
        )
    }
    
    fun isEdgeSquare(square: Square): Boolean {
        return square.file == 'a' || square.file == 'h' || 
               square.rank == 1 || square.rank == 8
    }
    
    fun isCenterSquare(square: Square): Boolean {
        return square.file in 'd'..'e' && square.rank in 4..5
    }
    
    fun isExtendedCenterSquare(square: Square): Boolean {
        return square.file in 'c'..'f' && square.rank in 3..6
    }
    
    fun getBackRank(color: ChessColor): Int {
        return if (color == ChessColor.WHITE) 1 else 8
    }
    
    fun getPawnStartRank(color: ChessColor): Int {
        return if (color == ChessColor.WHITE) 2 else 7
    }
    
    fun getPawnDirection(color: ChessColor): Int {
        return if (color == ChessColor.WHITE) 1 else -1
    }
    
    fun isPassedPawn(pawnSquare: Square, pawnColor: ChessColor, board: Map<Square, ChessPiece>): Boolean {
        val enemyColor = pawnColor.opposite()
        
        val filesToCheck = listOf(
            pawnSquare.file,
            (pawnSquare.file - 1).toChar(),
            (pawnSquare.file + 1).toChar()
        ).filter { it in 'a'..'h' }
        
        val ranksToCheck = if (pawnColor == ChessColor.WHITE) {
            (pawnSquare.rank + 1)..8
        } else {
            1 until pawnSquare.rank
        }
        
        for (file in filesToCheck) {
            for (rank in ranksToCheck) {
                val square = Square(file, rank)
                val piece = board[square]
                if (piece is ChessPiece.Pawn && piece.color == enemyColor) {
                    return false
                }
            }
        }
        
        return true
    }
}

data class PieceCount(
    val counts: Map<ChessColor, Map<String, Int>>
) {
    fun getCount(color: ChessColor, pieceType: String): Int {
        return counts[color]?.get(pieceType) ?: 0
    }
}

data class MaterialInfo(
    val whiteMaterial: Int,
    val blackMaterial: Int,
    val difference: Int
) {
    val isBalanced: Boolean = kotlin.math.abs(difference) < 100
    val advantage: ChessColor? = when {
        difference > 0 -> ChessColor.WHITE
        difference < 0 -> ChessColor.BLACK
        else -> null
    }
}

