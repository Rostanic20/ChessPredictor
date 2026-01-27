package com.chesspredictor.domain.usecases.components

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.entities.opposite

class CheckDetector(
    private val moveGenerator: MoveGenerator,
    @Suppress("UNUSED_PARAMETER") private val moveExecutor: MoveExecutor
) {

    fun isInCheck(gameState: GameState, color: ChessColor): Boolean {
        return isKingInCheck(gameState, color)
    }

    fun isKingInCheck(gameState: GameState, color: ChessColor): Boolean {
        val kingSquare = AttackDetector.findKingSquare(gameState, color) ?: return false
        return AttackDetector.isSquareAttackedBy(gameState, kingSquare, color.opposite())
    }

    fun isCheckmate(gameState: GameState): Boolean {
        if (!isKingInCheck(gameState, gameState.turn)) return false
        return moveGenerator.generateLegalMoves(gameState).isEmpty()
    }

    fun isStalemate(gameState: GameState): Boolean {
        if (isKingInCheck(gameState, gameState.turn)) return false
        return moveGenerator.generateLegalMoves(gameState).isEmpty()
    }

    fun isSquareAttacked(gameState: GameState, square: Square, byColor: ChessColor): Boolean {
        return AttackDetector.isSquareAttackedBy(gameState, square, byColor)
    }
}