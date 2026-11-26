package com.chesspredictor.domain.usecases.components

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.Square

class DrawDetector {

    companion object {
        private const val THREEFOLD_REPETITION_THRESHOLD = 3
        private const val FIFTY_MOVE_HALF_CLOCK = 100
    }

    fun isDraw(gameState: GameState): Boolean {
        return isThreefoldRepetition(gameState) || 
               isFiftyMoveRule(gameState) || 
               isInsufficientMaterial(gameState)
    }
    
    fun isThreefoldRepetition(gameState: GameState): Boolean {
        val currentPosition = gameState.toFen().split(" ").take(4).joinToString(" ")
        val earlierOccurrences = gameState.positionHistory.count { position ->
            position.split(" ").take(4).joinToString(" ") == currentPosition
        }
        return earlierOccurrences + 1 >= THREEFOLD_REPETITION_THRESHOLD
    }
    
    fun isFiftyMoveRule(gameState: GameState): Boolean {
        return gameState.halfMoveClock >= FIFTY_MOVE_HALF_CLOCK
    }
    
    fun isInsufficientMaterial(gameState: GameState): Boolean {
        val minors = gameState.board.filterValues { it !is ChessPiece.King }
        if (minors.values.any { it !is ChessPiece.Bishop && it !is ChessPiece.Knight }) return false
        if (minors.size <= 1) return true
        if (minors.values.any { it is ChessPiece.Knight }) return false
        return minors.keys.map(::isLightSquare).distinct().size == 1
    }

    fun hasMatingMaterial(gameState: GameState, color: ChessColor): Boolean {
        return gameState.board.values.any { it.color == color && it !is ChessPiece.King }
    }

    private fun isLightSquare(square: Square): Boolean {
        return (square.file.code + square.rank) % 2 != 0
    }
}