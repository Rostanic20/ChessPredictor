package com.chesspredictor.domain.usecases.components

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.Square
import kotlin.math.abs

object AttackDetector {

    fun isSquareAttackedBy(gameState: GameState, square: Square, byColor: ChessColor): Boolean {
        for ((pieceSquare, piece) in gameState.board) {
            if (piece.color == byColor && canPieceAttackSquare(gameState, pieceSquare, piece, square)) {
                return true
            }
        }
        return false
    }

    fun findKingSquare(gameState: GameState, color: ChessColor): Square? {
        return gameState.board.entries.find {
            it.value is ChessPiece.King && it.value.color == color
        }?.key
    }

    fun canPieceAttackSquare(gameState: GameState, from: Square, piece: ChessPiece, target: Square): Boolean {
        return when (piece) {
            is ChessPiece.Pawn -> canPawnAttackSquare(from, piece, target)
            is ChessPiece.Rook -> canSlidingPieceAttack(gameState, from, target, ROOK_DIRECTIONS)
            is ChessPiece.Knight -> canKnightAttackSquare(from, target)
            is ChessPiece.Bishop -> canSlidingPieceAttack(gameState, from, target, BISHOP_DIRECTIONS)
            is ChessPiece.Queen -> canSlidingPieceAttack(gameState, from, target, QUEEN_DIRECTIONS)
            is ChessPiece.King -> canKingAttackSquare(from, target)
        }
    }

    private fun canPawnAttackSquare(from: Square, pawn: ChessPiece.Pawn, target: Square): Boolean {
        val direction = if (pawn.color == ChessColor.WHITE) 1 else -1
        val attackRank = from.rank + direction
        val fileDiff = abs(from.file.code - target.file.code)
        return target.rank == attackRank && fileDiff == 1
    }

    private fun canKnightAttackSquare(from: Square, target: Square): Boolean {
        val fileDiff = abs(from.file.code - target.file.code)
        val rankDiff = abs(from.rank - target.rank)
        return (fileDiff == 2 && rankDiff == 1) || (fileDiff == 1 && rankDiff == 2)
    }

    private fun canKingAttackSquare(from: Square, target: Square): Boolean {
        val fileDiff = abs(from.file.code - target.file.code)
        val rankDiff = abs(from.rank - target.rank)
        return fileDiff <= 1 && rankDiff <= 1 && (fileDiff != 0 || rankDiff != 0)
    }

    private fun canSlidingPieceAttack(
        gameState: GameState,
        from: Square,
        target: Square,
        directions: List<Pair<Int, Int>>
    ): Boolean {
        val fileDiff = target.file.code - from.file.code
        val rankDiff = target.rank - from.rank

        val direction = directions.find { (df, dr) ->
            when {
                df == 0 && dr == 0 -> false
                df == 0 -> fileDiff == 0 && (rankDiff > 0 == dr > 0)
                dr == 0 -> rankDiff == 0 && (fileDiff > 0 == df > 0)
                else -> {
                    val absFileDiff = abs(fileDiff)
                    val absRankDiff = abs(rankDiff)
                    absFileDiff == absRankDiff &&
                        (fileDiff > 0 == df > 0) &&
                        (rankDiff > 0 == dr > 0)
                }
            }
        } ?: return false

        return isPathClear(gameState, from, target, direction)
    }

    private fun isPathClear(
        gameState: GameState,
        from: Square,
        target: Square,
        direction: Pair<Int, Int>
    ): Boolean {
        var currentFile = from.file.code + direction.first
        var currentRank = from.rank + direction.second

        while (currentFile.toChar() != target.file || currentRank != target.rank) {
            if (gameState.board[Square(currentFile.toChar(), currentRank)] != null) {
                return false
            }
            currentFile += direction.first
            currentRank += direction.second
        }
        return true
    }

    private val ROOK_DIRECTIONS = listOf(
        Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0)
    )

    private val BISHOP_DIRECTIONS = listOf(
        Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
    )

    private val QUEEN_DIRECTIONS = ROOK_DIRECTIONS + BISHOP_DIRECTIONS
}
