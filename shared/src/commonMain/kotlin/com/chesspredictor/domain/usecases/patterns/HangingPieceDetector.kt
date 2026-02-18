package com.chesspredictor.domain.usecases.patterns

import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.PatternSeverity
import com.chesspredictor.domain.entities.PatternType
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.entities.TacticalPattern
import com.chesspredictor.domain.entities.opposite

class HangingPieceDetector : PatternDetector {

    companion object {
        private const val CRITICAL_VALUE_THRESHOLD = 500
        private const val HIGH_VALUE_THRESHOLD = 300
    }

    override fun detect(gameState: GameState): List<TacticalPattern> {
        val hangingPieces = mutableListOf<TacticalPattern>()
        
        for ((square, piece) in gameState.board) {
            if (piece is ChessPiece.King) continue
            
            val pattern = checkIfPieceIsHanging(square, piece, gameState)
            if (pattern != null) {
                hangingPieces.add(pattern)
            }
        }
        
        return hangingPieces
    }
    
    override fun supportedPatternTypes(): Set<PatternType> {
        return setOf(PatternType.HANGING_PIECE)
    }
    
    private fun checkIfPieceIsHanging(
        square: Square,
        piece: ChessPiece,
        gameState: GameState
    ): TacticalPattern? {
        val attackers = BoardUtils.getAttackers(square, piece.color.opposite(), gameState)
        if (attackers.isEmpty()) return null
        
        val defenders = BoardUtils.getAttackers(square, piece.color, gameState)
        if (defenders.isNotEmpty()) return null
        
        return createHangingPiecePattern(square, piece, attackers, gameState)
    }
    
    private fun createHangingPiecePattern(
        targetSquare: Square,
        targetPiece: ChessPiece,
        attackers: List<Pair<Square, ChessPiece>>,
        gameState: GameState
    ): TacticalPattern {
        val pieceValue = BoardUtils.getPieceValue(targetPiece)
        
        return TacticalPattern(
            type = PatternType.HANGING_PIECE,
            squares = listOf(targetSquare) + attackers.map { it.first },
            attackingPieces = attackers,
            targetPieces = listOf(targetSquare to targetPiece),
            valuablePiece = targetSquare to targetPiece,
            side = targetPiece.color.opposite(),
            value = pieceValue,
            severity = calculateSeverity(pieceValue),
            isOpportunity = targetPiece.color != gameState.turn,
            description = "Hanging ${BoardUtils.getPieceName(targetPiece)} on $targetSquare"
        )
    }
    
    private fun calculateSeverity(pieceValue: Int): PatternSeverity {
        return when {
            pieceValue >= CRITICAL_VALUE_THRESHOLD -> PatternSeverity.CRITICAL
            pieceValue >= HIGH_VALUE_THRESHOLD -> PatternSeverity.HIGH
            else -> PatternSeverity.MEDIUM
        }
    }
}