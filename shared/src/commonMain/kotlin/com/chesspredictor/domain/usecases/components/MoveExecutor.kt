package com.chesspredictor.domain.usecases.components

import com.chesspredictor.domain.entities.CastlingRights
import com.chesspredictor.domain.entities.CastlingType
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.DetailedMove
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.entities.opposite
import kotlin.math.abs

class MoveExecutor {
    
    fun executeMove(gameState: GameState, move: ChessMove): GameState {
        val newBoard = gameState.board.toMutableMap()
        val newMoveHistory = gameState.moveHistory.toMutableList()
        val newCapturedPieces = gameState.capturedPieces.toMutableList()
        
        // Handle captured piece
        val capturedPiece = move.capturedPiece ?: gameState.board[move.to]
        if (capturedPiece != null) {
            newCapturedPieces.add(capturedPiece)
        }
        
        // Remove piece from source
        newBoard.remove(move.from)
        
        // Place piece on destination (or promoted piece)
        val finalPiece = move.promotion ?: move.piece
        newBoard[move.to] = finalPiece
        
        // Handle special moves
        var newEnPassantSquare: Square? = null
        var castlingType: CastlingType? = null
        
        when {
            // Castling - move the rook
            move.piece is ChessPiece.King && abs(move.from.file.code - move.to.file.code) == 2 -> {
                val isKingside = move.to.file > move.from.file
                val rookFromFile = if (isKingside) 'h' else 'a'
                val rookToFile = if (isKingside) 'f' else 'd'
                val rank = move.from.rank
                
                val rook = newBoard.remove(Square(rookFromFile, rank))
                if (rook != null) {
                    newBoard[Square(rookToFile, rank)] = rook
                }
                
                // Set castling type
                castlingType = when {
                    move.piece.color == ChessColor.WHITE && isKingside -> CastlingType.WHITE_KINGSIDE
                    move.piece.color == ChessColor.WHITE && !isKingside -> CastlingType.WHITE_QUEENSIDE
                    move.piece.color == ChessColor.BLACK && isKingside -> CastlingType.BLACK_KINGSIDE
                    else -> CastlingType.BLACK_QUEENSIDE
                }
            }
            
            // En passant capture - remove the captured pawn
            move.piece is ChessPiece.Pawn && move.to == gameState.enPassantSquare -> {
                val capturedPawnSquare = Square(move.to.file, move.from.rank)
                val enPassantPawn = newBoard.remove(capturedPawnSquare)
                if (enPassantPawn != null && capturedPiece == null) {
                    newCapturedPieces.add(enPassantPawn)
                }
            }
            
            // Pawn double move - set en passant square
            move.piece is ChessPiece.Pawn && abs(move.from.rank - move.to.rank) == 2 -> {
                newEnPassantSquare = Square(move.from.file, (move.from.rank + move.to.rank) / 2)
            }
        }
        
        // Update castling rights
        val newCastlingRights = updateCastlingRights(gameState.castlingRights, move)
        
        // Calculate halfmove clock
        val newHalfmoveClock = if (move.piece is ChessPiece.Pawn || capturedPiece != null) {
            0
        } else {
            gameState.halfMoveClock + 1
        }
        
        // Calculate fullmove number
        val newFullmoveNumber = if (gameState.turn == ChessColor.BLACK) {
            gameState.fullMoveNumber + 1
        } else {
            gameState.fullMoveNumber
        }
        
        // Create detailed move for history
        val moveNumber = if (gameState.turn == ChessColor.WHITE) {
            gameState.fullMoveNumber
        } else {
            gameState.fullMoveNumber
        }
        
        val detailedMove = DetailedMove(
            move = move,
            moveNumber = moveNumber,
            isWhiteMove = gameState.turn == ChessColor.WHITE,
            isCapture = capturedPiece != null,
            isCastling = castlingType,
            isEnPassant = move.piece is ChessPiece.Pawn && move.to == gameState.enPassantSquare,
            isPromotion = move.promotion != null,
            isCheck = false, // Will be updated by caller
            isCheckmate = false, // Will be updated by caller
            san = generateSimpleSAN(move),
            previousCastlingRights = gameState.castlingRights,
            previousEnPassantSquare = gameState.enPassantSquare,
            promotion = move.promotion
        )
        newMoveHistory.add(detailedMove)
        
        return GameState(
            board = newBoard,
            turn = gameState.turn.opposite(),
            castlingRights = newCastlingRights,
            enPassantSquare = newEnPassantSquare,
            halfMoveClock = newHalfmoveClock,
            fullMoveNumber = newFullmoveNumber,
            moveHistory = newMoveHistory,
            capturedPieces = newCapturedPieces,
            positionHistory = gameState.positionHistory + gameState.toFen()
        )
    }
    
    private fun updateCastlingRights(currentRights: CastlingRights, move: ChessMove): CastlingRights {
        var newRights = currentRights
        
        // King moves remove all castling rights for that color
        if (move.piece is ChessPiece.King) {
            newRights = if (move.piece.color == ChessColor.WHITE) {
                newRights.copy(whiteKingside = false, whiteQueenside = false)
            } else {
                newRights.copy(blackKingside = false, blackQueenside = false)
            }
        }
        
        // Rook moves remove castling rights for that side
        if (move.piece is ChessPiece.Rook) {
            when {
                move.from == Square('a', 1) -> newRights = newRights.copy(whiteQueenside = false)
                move.from == Square('h', 1) -> newRights = newRights.copy(whiteKingside = false)
                move.from == Square('a', 8) -> newRights = newRights.copy(blackQueenside = false)
                move.from == Square('h', 8) -> newRights = newRights.copy(blackKingside = false)
            }
        }
        
        // Rook captures remove castling rights
        when (move.to) {
            Square('a', 1) -> newRights = newRights.copy(whiteQueenside = false)
            Square('h', 1) -> newRights = newRights.copy(whiteKingside = false)
            Square('a', 8) -> newRights = newRights.copy(blackQueenside = false)
            Square('h', 8) -> newRights = newRights.copy(blackKingside = false)
        }
        
        return newRights
    }
    
    private fun generateSimpleSAN(move: ChessMove): String {
        // Basic SAN generation - can be improved later
        val piece = when (move.piece) {
            is ChessPiece.King -> "K"
            is ChessPiece.Queen -> "Q"
            is ChessPiece.Rook -> "R"
            is ChessPiece.Bishop -> "B"
            is ChessPiece.Knight -> "N"
            is ChessPiece.Pawn -> ""
        }
        
        val capture = if (move.capturedPiece != null) "x" else ""
        val destination = "${move.to.file}${move.to.rank}"
        
        return "$piece$capture$destination"
    }
}