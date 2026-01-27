package com.chesspredictor.domain.usecases.components

import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.GameState

class MoveValidator(
    private val moveGenerator: MoveGenerator,
    @Suppress("UNUSED_PARAMETER") checkDetector: CheckDetector
) {

    fun isMoveLegal(gameState: GameState, move: ChessMove): Boolean {
        // Check if it's the correct player's turn
        if (move.piece.color != gameState.turn) return false
        
        // Check if the piece is actually on the from square
        if (gameState.board[move.from] != move.piece) return false
        
        // Check if the move is in the list of legal moves
        val legalMoves = moveGenerator.generateLegalMoves(gameState)
        return legalMoves.any { legalMove ->
            legalMove.piece == move.piece &&
            legalMove.from == move.from &&
            legalMove.to == move.to &&
            legalMove.promotion == move.promotion
        }
    }
}