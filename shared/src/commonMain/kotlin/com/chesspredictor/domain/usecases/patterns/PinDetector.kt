package com.chesspredictor.domain.usecases.patterns

import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.PatternSeverity
import com.chesspredictor.domain.entities.PatternType
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.entities.TacticalPattern

/**
 * Detects pin patterns where a piece is pinned to a more valuable piece.
 * 
 * A pin occurs when:
 * - A sliding piece (bishop, rook, queen) attacks along a line
 * - An enemy piece is on that line
 * - A more valuable enemy piece is behind it on the same line
 */
class PinDetector : PatternDetector {
    
    override fun detect(gameState: GameState): List<TacticalPattern> {
        val pins = mutableListOf<TacticalPattern>()
        
        for ((square, piece) in gameState.board) {
            if (!BoardUtils.canPiecePin(piece)) continue
            
            val attackLines = BoardUtils.getAttackLines(square, piece, gameState)
            pins.addAll(findPinsInAttackLines(square, piece, attackLines, gameState))
        }
        
        return pins
    }
    
    override fun supportedPatternTypes(): Set<PatternType> {
        return setOf(PatternType.PIN, PatternType.ABSOLUTE_PIN)
    }
    
    private fun findPinsInAttackLines(
        attackerSquare: Square,
        attacker: ChessPiece,
        attackLines: List<List<Square>>,
        gameState: GameState
    ): List<TacticalPattern> {
        val pins = mutableListOf<TacticalPattern>()
        
        for (line in attackLines) {
            val pin = checkLineForPin(attackerSquare, attacker, line, gameState)
            if (pin != null) {
                pins.add(pin)
            }
        }
        
        return pins
    }
    
    private fun checkLineForPin(
        attackerSquare: Square,
        attacker: ChessPiece,
        line: List<Square>,
        gameState: GameState
    ): TacticalPattern? {
        val piecesInLine = line.mapNotNull { sq -> 
            gameState.board[sq]?.let { sq to it } 
        }
        
        // A pin requires exactly 2 pieces in the line
        if (piecesInLine.size != 2) return null
        
        val (pinnedSquare, pinnedPiece) = piecesInLine[0]
        val (valuableSquare, valuablePiece) = piecesInLine[1]
        
        // Check if it's a valid pin configuration
        if (!isValidPin(attacker, pinnedPiece, valuablePiece)) return null
        
        return createPinPattern(
            attackerSquare, attacker,
            pinnedSquare, pinnedPiece,
            valuableSquare, valuablePiece,
            gameState
        )
    }
    
    private fun isValidPin(
        attacker: ChessPiece,
        pinnedPiece: ChessPiece,
        valuablePiece: ChessPiece
    ): Boolean {
        return pinnedPiece.color != attacker.color && 
               valuablePiece.color != attacker.color && 
               BoardUtils.isValuablePiece(valuablePiece)
    }
    
    private fun createPinPattern(
        attackerSquare: Square,
        attacker: ChessPiece,
        pinnedSquare: Square,
        pinnedPiece: ChessPiece,
        valuableSquare: Square,
        valuablePiece: ChessPiece,
        gameState: GameState
    ): TacticalPattern {
        val isAbsolutePin = valuablePiece is ChessPiece.King
        
        return TacticalPattern(
            type = if (isAbsolutePin) PatternType.ABSOLUTE_PIN else PatternType.PIN,
            squares = listOf(attackerSquare, pinnedSquare, valuableSquare),
            attackingPieces = listOf(attackerSquare to attacker),
            targetPieces = listOf(pinnedSquare to pinnedPiece),
            valuablePiece = valuableSquare to valuablePiece,
            side = attacker.color,
            value = calculatePinValue(pinnedPiece, valuablePiece),
            severity = if (isAbsolutePin) PatternSeverity.CRITICAL else PatternSeverity.HIGH,
            isOpportunity = attacker.color == gameState.turn,
            description = buildPinDescription(
                attacker, attackerSquare,
                pinnedPiece, pinnedSquare,
                valuablePiece, valuableSquare
            )
        )
    }
    
    private fun calculatePinValue(pinnedPiece: ChessPiece, valuablePiece: ChessPiece): Int {
        return if (valuablePiece is ChessPiece.King) {
            BoardUtils.getPieceValue(pinnedPiece) // Can win the pinned piece
        } else {
            minOf(
                BoardUtils.getPieceValue(pinnedPiece), 
                BoardUtils.getPieceValue(valuablePiece) - BoardUtils.getPieceValue(pinnedPiece)
            )
        }
    }
    
    private fun buildPinDescription(
        attacker: ChessPiece, attackerSquare: Square,
        pinnedPiece: ChessPiece, pinnedSquare: Square,
        valuablePiece: ChessPiece, valuableSquare: Square
    ): String {
        return "${BoardUtils.getPieceName(attacker)} on $attackerSquare pins " +
               "${BoardUtils.getPieceName(pinnedPiece)} on $pinnedSquare to " +
               "${BoardUtils.getPieceName(valuablePiece)} on $valuableSquare"
    }
}