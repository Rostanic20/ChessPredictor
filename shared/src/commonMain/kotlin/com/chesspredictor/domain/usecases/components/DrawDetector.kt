package com.chesspredictor.domain.usecases.components

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.GameState

class DrawDetector {
    
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
        return occurrences >= 2 // Current position + 2 previous = 3 total
    }
    
    fun isFiftyMoveRule(gameState: GameState): Boolean {
        return gameState.halfMoveClock >= 100 // 50 moves by each player = 100 half-moves
    }
    
    fun isInsufficientMaterial(gameState: GameState): Boolean {
        val pieces = gameState.board.values.groupBy { it.color }
        val whitePieces = pieces[ChessColor.WHITE] ?: emptyList()
        val blackPieces = pieces[ChessColor.BLACK] ?: emptyList()
        
        // Both sides must have insufficient material
        return hasInsufficientMaterial(whitePieces) && hasInsufficientMaterial(blackPieces)
    }
    
    private fun hasInsufficientMaterial(pieces: List<ChessPiece>): Boolean {
        // Remove kings from consideration
        val nonKingPieces = pieces.filterNot { it is ChessPiece.King }
        
        return when (nonKingPieces.size) {
            0 -> true // King only
            1 -> {
                val piece = nonKingPieces.first()
                piece is ChessPiece.Bishop || piece is ChessPiece.Knight
            }
            2 -> {
                // Two bishops of same color
                val bishops = nonKingPieces.filterIsInstance<ChessPiece.Bishop>()
                if (bishops.size == 2) {
                    // This is a simplification - in reality we'd need to check square colors
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }
}