package com.chesspredictor.data.utils

import com.chesspredictor.data.repositories.MoveParser
import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.utils.FenParser

class MoveParserImpl : MoveParser {
    override fun parseUciMove(uciMove: String, board: ChessBoard): ChessMove? {
        if (uciMove.length < 4) return null
        
        try {
            val fromFile = uciMove[0]
            val fromRank = uciMove[1].digitToInt()
            val toFile = uciMove[2]
            val toRank = uciMove[3].digitToInt()
            
            val from = Square(fromFile, fromRank)
            val to = Square(toFile, toRank)
            
            // Get piece at from square (simplified - would need actual board state)
            val piece = getPieceAt(board, from) ?: return null
            
            // Validate that the piece color matches whose turn it is
            val gameState = FenParser().parseGameState(board.fen)
            if (piece.color != gameState.turn) {
                return null
            }
            
            // Check for promotion
            val promotion = if (uciMove.length == 5) {
                when (uciMove[4]) {
                    'q' -> ChessPiece.Queen(piece.color)
                    'r' -> ChessPiece.Rook(piece.color)
                    'b' -> ChessPiece.Bishop(piece.color)
                    'n' -> ChessPiece.Knight(piece.color)
                    else -> null
                }
            } else null
            
            // Get captured piece if any
            val capturedPiece = getPieceAt(board, to)
            
            return ChessMove(
                from = from,
                to = to,
                piece = piece,
                capturedPiece = capturedPiece,
                promotion = promotion
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    override fun toUciMove(move: ChessMove): String {
        val base = "${move.from}${move.to}"
        return when (move.promotion) {
            is ChessPiece.Queen -> "${base}q"
            is ChessPiece.Rook -> "${base}r"
            is ChessPiece.Bishop -> "${base}b"
            is ChessPiece.Knight -> "${base}n"
            else -> base
        }
    }
    
    private fun getPieceAt(board: ChessBoard, square: Square): ChessPiece? {
        val fenParser = FenParser()
        val gameState = fenParser.parseGameState(board.fen)
        return gameState.board[square]
    }
}