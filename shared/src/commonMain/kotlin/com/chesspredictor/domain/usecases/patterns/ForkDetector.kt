package com.chesspredictor.domain.usecases.patterns

import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.PatternSeverity
import com.chesspredictor.domain.entities.PatternType
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.entities.TacticalPattern

class ForkDetector : PatternDetector {

    companion object {
        private const val HIGH_VALUE_THRESHOLD = 500
    }

    override fun detect(gameState: GameState): List<TacticalPattern> {
        val forks = mutableListOf<TacticalPattern>()
        
        for ((square, piece) in gameState.board) {
            val fork = detectForkFromSquare(square, piece, gameState)
            if (fork != null) {
                forks.add(fork)
            }
        }
        
        return forks
    }
    
    override fun supportedPatternTypes(): Set<PatternType> {
        return setOf(
            PatternType.KNIGHT_FORK,
            PatternType.PAWN_FORK,
            PatternType.ROYAL_FORK,
            PatternType.DOUBLE_ATTACK
        )
    }
    
    private fun detectForkFromSquare(
        square: Square,
        piece: ChessPiece,
        gameState: GameState
    ): TacticalPattern? {
        val enemyPiecesAttacked = findAttackedEnemyPieces(square, piece, gameState)
        
        if (enemyPiecesAttacked.size < 2) return null
        if (piece is ChessPiece.Pawn && enemyPiecesAttacked.size != 2) return null
        
        return createForkPattern(square, piece, enemyPiecesAttacked, gameState)
    }
    
    private fun findAttackedEnemyPieces(
        square: Square,
        piece: ChessPiece,
        gameState: GameState
    ): List<Pair<Square, ChessPiece>> {
        val attacks = BoardUtils.getSquaresAttackedBy(square, piece, gameState)
        
        return attacks.mapNotNull { attackedSquare ->
            gameState.board[attackedSquare]?.let { targetPiece ->
                if (targetPiece.color != piece.color) {
                    attackedSquare to targetPiece
                } else null
            }
        }
    }
    
    private fun createForkPattern(
        attackerSquare: Square,
        attacker: ChessPiece,
        targets: List<Pair<Square, ChessPiece>>,
        gameState: GameState
    ): TacticalPattern? {
        if (targets.isEmpty()) return null
        val forkType = determineForkType(attacker, targets)
        val mostValuableTarget = targets.maxByOrNull { BoardUtils.getPieceValue(it.second) }
            ?: return null
        val totalValue = calculateForkValue(attacker, targets)
        
        return TacticalPattern(
            type = forkType,
            squares = listOf(attackerSquare) + targets.map { it.first },
            attackingPieces = listOf(attackerSquare to attacker),
            targetPieces = targets,
            valuablePiece = mostValuableTarget,
            side = attacker.color,
            value = totalValue,
            severity = calculateForkSeverity(forkType, totalValue),
            isOpportunity = attacker.color == gameState.turn,
            description = buildForkDescription(attacker, attackerSquare, targets)
        )
    }
    
    private fun determineForkType(
        attacker: ChessPiece,
        targets: List<Pair<Square, ChessPiece>>
    ): PatternType {
        return when {
            attacker is ChessPiece.Knight -> PatternType.KNIGHT_FORK
            attacker is ChessPiece.Pawn && targets.size == 2 -> PatternType.PAWN_FORK
            targets.any { it.second is ChessPiece.King } -> PatternType.ROYAL_FORK
            else -> PatternType.DOUBLE_ATTACK
        }
    }
    
    private fun calculateForkValue(
        attacker: ChessPiece,
        targets: List<Pair<Square, ChessPiece>>
    ): Int {
        val targetValues = targets.sumOf { BoardUtils.getPieceValue(it.second) }
        val attackerValue = BoardUtils.getPieceValue(attacker)
        
        return targetValues - attackerValue
    }
    
    private fun calculateForkSeverity(forkType: PatternType, value: Int): PatternSeverity {
        return when {
            forkType == PatternType.ROYAL_FORK -> PatternSeverity.CRITICAL
            value > HIGH_VALUE_THRESHOLD -> PatternSeverity.HIGH
            else -> PatternSeverity.MEDIUM
        }
    }
    
    private fun buildForkDescription(
        attacker: ChessPiece,
        attackerSquare: Square,
        targets: List<Pair<Square, ChessPiece>>
    ): String {
        return "${BoardUtils.getPieceName(attacker)} on $attackerSquare forks ${targets.size} pieces"
    }
}