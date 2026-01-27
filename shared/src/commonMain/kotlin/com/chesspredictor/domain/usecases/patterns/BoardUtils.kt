package com.chesspredictor.domain.usecases.patterns

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.Square

/**
 * Utility class for chess board operations used in pattern detection.
 * Provides common functionality for move generation, attack detection, and piece evaluation.
 */
class BoardUtils {
    
    companion object {
        /**
         * Gets all squares attacked by a piece from a given position.
         * Does not consider move legality, only attack patterns.
         */
        fun getSquaresAttackedBy(from: Square, piece: ChessPiece, gameState: GameState): List<Square> {
            return when (piece) {
                is ChessPiece.Pawn -> getPawnAttacks(from, piece.color)
                is ChessPiece.Knight -> getKnightMoves(from)
                is ChessPiece.Bishop -> getBishopMoves(from, gameState)
                is ChessPiece.Rook -> getRookMoves(from, gameState)
                is ChessPiece.Queen -> getQueenMoves(from, gameState)
                is ChessPiece.King -> getKingMoves(from)
            }.filter { it.isValid() }
        }
        
        /**
         * Gets all possible moves for a piece from a given position.
         * Filters out moves to squares occupied by friendly pieces.
         */
        fun getPossibleMoves(from: Square, piece: ChessPiece, gameState: GameState): List<Square> {
            return getSquaresAttackedBy(from, piece, gameState).filter { target ->
                gameState.board[target]?.color != piece.color
            }
        }
        
        /**
         * Finds all pieces of a given color that attack a specific square.
         */
        fun getAttackers(target: Square, byColor: ChessColor, gameState: GameState): List<Pair<Square, ChessPiece>> {
            return gameState.board.filter { (sq, piece) ->
                piece.color == byColor && 
                getSquaresAttackedBy(sq, piece, gameState).contains(target)
            }.map { it.key to it.value }
        }
        
        /**
         * Checks if a piece can pin other pieces (bishops, rooks, queens).
         */
        fun canPiecePin(piece: ChessPiece): Boolean {
            return piece is ChessPiece.Bishop || piece is ChessPiece.Rook || piece is ChessPiece.Queen
        }
        
        /**
         * Checks if a piece can skewer other pieces (bishops, rooks, queens).
         */
        fun canPieceSkewer(piece: ChessPiece): Boolean = canPiecePin(piece)
        
        /**
         * Determines if a piece is considered valuable (king, queen, rook).
         */
        fun isValuablePiece(piece: ChessPiece): Boolean {
            return piece is ChessPiece.King || piece is ChessPiece.Queen || piece is ChessPiece.Rook
        }
        
        /**
         * Gets the standard material value of a piece.
         */
        fun getPieceValue(piece: ChessPiece): Int {
            return com.chesspredictor.domain.constants.PieceValues.getValue(piece)
        }
        
        /**
         * Gets a human-readable name for a piece.
         */
        fun getPieceName(piece: ChessPiece): String {
            return when (piece) {
                is ChessPiece.Pawn -> "Pawn"
                is ChessPiece.Knight -> "Knight"
                is ChessPiece.Bishop -> "Bishop"
                is ChessPiece.Rook -> "Rook"
                is ChessPiece.Queen -> "Queen"
                is ChessPiece.King -> "King"
            }
        }
        
        /**
         * Gets all attack lines for sliding pieces (bishop, rook, queen).
         * Returns lists of squares in each direction until blocked.
         */
        fun getAttackLines(from: Square, piece: ChessPiece, gameState: GameState): List<List<Square>> {
            val lines = mutableListOf<List<Square>>()
            val directions = when (piece) {
                is ChessPiece.Bishop -> BISHOP_DIRECTIONS
                is ChessPiece.Rook -> ROOK_DIRECTIONS
                is ChessPiece.Queen -> QUEEN_DIRECTIONS
                else -> return emptyList()
            }
            
            for (direction in directions) {
                val line = mutableListOf<Square>()
                var current = from.move(direction.first, direction.second)
                
                while (current != null && current.isValid()) {
                    line.add(current)
                    if (gameState.board.containsKey(current)) {
                        // Add one more square if there's another piece behind
                        val next = current.move(direction.first, direction.second)
                        if (next?.isValid() == true && gameState.board.containsKey(next)) {
                            line.add(next)
                        }
                        break
                    }
                    current = current.move(direction.first, direction.second)
                }
                
                if (line.isNotEmpty()) lines.add(line)
            }
            
            return lines
        }
        
        // Private helper functions
        
        private fun getPawnAttacks(from: Square, color: ChessColor): List<Square> {
            val direction = if (color == ChessColor.WHITE) 1 else -1
            return listOfNotNull(
                from.move(1, direction),
                from.move(-1, direction)
            )
        }
        
        private fun getKnightMoves(from: Square): List<Square> {
            return KNIGHT_MOVES.mapNotNull { (dx, dy) -> from.move(dx, dy) }
        }
        
        private fun getKingMoves(from: Square): List<Square> {
            return KING_MOVES.mapNotNull { (dx, dy) -> from.move(dx, dy) }
        }
        
        private fun getBishopMoves(from: Square, gameState: GameState): List<Square> {
            return getSlidingMoves(from, gameState, BISHOP_DIRECTIONS)
        }
        
        private fun getRookMoves(from: Square, gameState: GameState): List<Square> {
            return getSlidingMoves(from, gameState, ROOK_DIRECTIONS)
        }
        
        private fun getQueenMoves(from: Square, gameState: GameState): List<Square> {
            return getBishopMoves(from, gameState) + getRookMoves(from, gameState)
        }
        
        private fun getSlidingMoves(
            from: Square, 
            gameState: GameState, 
            directions: List<Pair<Int, Int>>
        ): List<Square> {
            val moves = mutableListOf<Square>()
            
            for ((dx, dy) in directions) {
                var current = from.move(dx, dy)
                while (current?.isValid() == true) {
                    moves.add(current)
                    if (gameState.board.containsKey(current)) break
                    current = current.move(dx, dy)
                }
            }
            
            return moves
        }
        
        // Direction constants
        private val BISHOP_DIRECTIONS = listOf(
            Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
        )
        
        private val ROOK_DIRECTIONS = listOf(
            Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0)
        )
        
        private val QUEEN_DIRECTIONS = BISHOP_DIRECTIONS + ROOK_DIRECTIONS
        
        private val KNIGHT_MOVES = listOf(
            Pair(2, 1), Pair(2, -1), Pair(-2, 1), Pair(-2, -1),
            Pair(1, 2), Pair(1, -2), Pair(-1, 2), Pair(-1, -2)
        )
        
        private val KING_MOVES = listOf(
            Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0),
            Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
        )
    }
}

// Extension functions for Square
fun Square.move(fileOffset: Int, rankOffset: Int): Square? {
    val newFile = (file + fileOffset).toChar()
    val newRank = rank + rankOffset
    
    return if (newFile in 'a'..'h' && newRank in 1..8) {
        Square(newFile, newRank)
    } else null
}

fun Square.isValid(): Boolean {
    return file in 'a'..'h' && rank in 1..8
}