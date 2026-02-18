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
        
        if (move.piece is ChessPiece.King && abs(move.from.file.code - move.to.file.code) == 2) {
            return if (move.to.file > move.from.file) "O-O" else "O-O-O"
        }
        
        if (move.piece !is ChessPiece.Pawn) {
            sb.append(getPieceSymbol(move.piece))
            
            val disambiguation = getDisambiguation(gameState, move)
            sb.append(disambiguation)
        }
        
        if (move.capturedPiece != null || (move.piece is ChessPiece.Pawn && move.to == gameState.enPassantSquare)) {
            if (move.piece is ChessPiece.Pawn) {
                sb.append(move.from.file)
            }
            sb.append("x")
        }
        
        sb.append(move.to.file)
        sb.append(move.to.rank)
        
        if (move.promotion != null) {
            sb.append("=")
            sb.append(getPieceSymbol(move.promotion))
        }
        
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
        val sameTypeMoves = moveGenerator.generateLegalMoves(gameState).filter { otherMove ->
            otherMove.piece::class == move.piece::class &&
            otherMove.piece.color == move.piece.color &&
            otherMove.to == move.to &&
            otherMove.from != move.from
        }
        
        if (sameTypeMoves.isEmpty()) {
            return ""
        }
        
        val sameFile = sameTypeMoves.any { it.from.file == move.from.file }
        if (!sameFile) {
            return move.from.file.toString()
        }
        
        val sameRank = sameTypeMoves.any { it.from.rank == move.from.rank }
        if (!sameRank) {
            return move.from.rank.toString()
        }
        
        return "${move.from.file}${move.from.rank}"
    }
}