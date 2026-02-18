package com.chesspredictor.domain.usecases.components

import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.GameState

class MoveValidator(
    private val moveGenerator: MoveGenerator
) {

    fun isMoveLegal(gameState: GameState, move: ChessMove): Boolean {
        if (move.piece.color != gameState.turn) return false

        if (gameState.board[move.from] != move.piece) return false
        val legalMoves = moveGenerator.generateLegalMoves(gameState)
        return legalMoves.any { legalMove ->
            legalMove.piece == move.piece &&
            legalMove.from == move.from &&
            legalMove.to == move.to &&
            legalMove.promotion == move.promotion
        }
    }
}