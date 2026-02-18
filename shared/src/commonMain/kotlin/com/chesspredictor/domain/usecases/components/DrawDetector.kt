package com.chesspredictor.domain.usecases.components

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.Square

class DrawDetector {

    companion object {
        private const val THREEFOLD_REPETITION_THRESHOLD = 2
        private const val FIFTY_MOVE_HALF_CLOCK = 100
    }

    fun isDraw(gameState: GameState): Boolean {
        return isThreefoldRepetition(gameState) || 
               isFiftyMoveRule(gameState) || 
               isInsufficientMaterial(gameState)
    }
    
    fun isThreefoldRepetition(gameState: GameState): Boolean {
        val currentPosition = gameState.toFen().split(" ").take(4).joinToString(" ")
        val occurrences = gameState.positionHistory.count { position ->
            position.split(" ").take(4).joinToString(" ") == currentPosition
        }
        return occurrences >= THREEFOLD_REPETITION_THRESHOLD
    }
    
    fun isFiftyMoveRule(gameState: GameState): Boolean {
        return gameState.halfMoveClock >= FIFTY_MOVE_HALF_CLOCK
    }
    
    fun isInsufficientMaterial(gameState: GameState): Boolean {
        val piecesBySquare = gameState.board
        val whitePieces = piecesBySquare.filter { it.value.color == ChessColor.WHITE }
        val blackPieces = piecesBySquare.filter { it.value.color == ChessColor.BLACK }

        return hasInsufficientMaterial(whitePieces) && hasInsufficientMaterial(blackPieces)
    }

    private fun hasInsufficientMaterial(pieces: Map<Square, ChessPiece>): Boolean {
        val nonKingPieces = pieces.filter { it.value !is ChessPiece.King }

        return when (nonKingPieces.size) {
            0 -> true
            1 -> {
                val piece = nonKingPieces.values.first()
                piece is ChessPiece.Bishop || piece is ChessPiece.Knight
            }
            2 -> {
                val bishops = nonKingPieces.filter { it.value is ChessPiece.Bishop }
                if (bishops.size == 2) {
                    val squares = bishops.keys.toList()
                    isLightSquare(squares[0]) == isLightSquare(squares[1])
                } else {
                    false
                }
            }
            else -> false
        }
    }

    private fun isLightSquare(square: Square): Boolean {
        return (square.file.code + square.rank) % 2 != 0
    }
}