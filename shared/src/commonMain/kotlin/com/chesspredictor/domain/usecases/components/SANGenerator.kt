package com.chesspredictor.domain.usecases.components

import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.GameState
import kotlin.math.abs

class SANGenerator(
    private val moveGenerator: MoveGenerator,
    private val checkDetector: CheckDetector
) {
    
    fun generateSAN(gameState: GameState, move: ChessMove): String {
        return generateSAN(gameState, move, null)
    }
    
    fun generateSAN(gameState: GameState, move: ChessMove, newState: GameState?): String {
        val sb = StringBuilder()
        
        // Handle castling first
        if (move.piece is ChessPiece.King && abs(move.from.file.code - move.to.file.code) == 2) {
            return if (move.to.file > move.from.file) "O-O" else "O-O-O"
        }
        
        // Add piece symbol (except for pawns)
        if (move.piece !is ChessPiece.Pawn) {
            sb.append(getPieceSymbol(move.piece))
            
            // Add disambiguation if needed
            val disambiguation = getDisambiguation(gameState, move)
            sb.append(disambiguation)
        }
        
        // Add capture symbol
        if (move.capturedPiece != null || (move.piece is ChessPiece.Pawn && move.to == gameState.enPassantSquare)) {
            if (move.piece is ChessPiece.Pawn) {
                sb.append(move.from.file)
            }
            sb.append("x")
        }
        
        // Add destination square
        sb.append(move.to.file)
        sb.append(move.to.rank)
        
        // Add promotion
        if (move.promotion != null) {
            sb.append("=")
            sb.append(getPieceSymbol(move.promotion))
        }
        
        // Add check/checkmate notation
        if (newState != null) {
            when {
                checkDetector.isCheckmate(newState) -> sb.append("#")
                checkDetector.isKingInCheck(newState, newState.turn) -> sb.append("+")
            }
        }
        
        return sb.toString()
    }
    
    private fun getPieceSymbol(piece: ChessPiece): String {
        return when (piece) {
            is ChessPiece.King -> "K"
            is ChessPiece.Queen -> "Q"
            is ChessPiece.Rook -> "R"
            is ChessPiece.Bishop -> "B"
            is ChessPiece.Knight -> "N"
            is ChessPiece.Pawn -> ""
        }
    }
    
    private fun getDisambiguation(gameState: GameState, move: ChessMove): String {
        // Find all pieces of the same type that can move to the same square
        val sameTypeMoves = moveGenerator.generateLegalMoves(gameState).filter { otherMove ->
            otherMove.piece::class == move.piece::class &&
            otherMove.piece.color == move.piece.color &&
            otherMove.to == move.to &&
            otherMove.from != move.from
        }
        
        if (sameTypeMoves.isEmpty()) {
            return ""
        }
        
        // Check if file disambiguation is sufficient
        val sameFile = sameTypeMoves.any { it.from.file == move.from.file }
        if (!sameFile) {
            return move.from.file.toString()
        }
        
        // Check if rank disambiguation is sufficient
        val sameRank = sameTypeMoves.any { it.from.rank == move.from.rank }
        if (!sameRank) {
            return move.from.rank.toString()
        }
        
        // Use full square notation
        return "${move.from.file}${move.from.rank}"
    }
}