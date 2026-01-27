package com.chesspredictor.domain.utils

import com.chesspredictor.domain.entities.CastlingRights
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.utils.ChessUtils
import com.chesspredictor.domain.utils.ChessConstants

/**
 * Single source of truth for FEN string parsing and generation
 */
class FenParser {
    
    fun parseGameState(fen: String): GameState {
        val parts = fen.split(" ")
        require(parts.size >= 4) { "Invalid FEN string" }
        
        // Parse board
        val board = parseBoardPosition(parts[0])
        
        // Parse turn
        val turn = if (parts.getOrNull(1) == "b") ChessColor.BLACK else ChessColor.WHITE
        
        // Parse castling rights
        val castlingRights = parseCastlingRights(parts.getOrNull(2) ?: "-")
        
        // Parse en passant
        val enPassantSquare = parseEnPassantSquare(parts.getOrNull(3) ?: "-")
        
        // Parse clocks
        val halfMoveClock = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val fullMoveNumber = parts.getOrNull(5)?.toIntOrNull() ?: 1
        
        return GameState(
            board = board,
            turn = turn,
            castlingRights = castlingRights,
            enPassantSquare = enPassantSquare,
            halfMoveClock = halfMoveClock,
            fullMoveNumber = fullMoveNumber
        )
    }
    
    private fun parseBoardPosition(boardString: String): Map<Square, ChessPiece> {
        val board = mutableMapOf<Square, ChessPiece>()
        var rank = 8
        var file = 0
        
        for (char in boardString) {
            when (char) {
                '/' -> {
                    rank--
                    file = 0
                }
                in '1'..'8' -> {
                    file += char.digitToInt()
                }
                else -> {
                    val square = Square(('a' + file), rank)
                    val piece = ChessUtils.charToPiece(char)
                    if (piece != null) {
                        board[square] = piece
                    }
                    file++
                }
            }
        }
        
        return board
    }
    
    private fun parseCastlingRights(castlingString: String): CastlingRights {
        return CastlingRights(
            whiteKingside = 'K' in castlingString,
            whiteQueenside = 'Q' in castlingString,
            blackKingside = 'k' in castlingString,
            blackQueenside = 'q' in castlingString
        )
    }
    
    private fun parseEnPassantSquare(enPassantString: String): Square? {
        if (enPassantString == "-") return null
        if (enPassantString.length != 2) return null
        
        val file = enPassantString[0]
        val rank = enPassantString[1].digitToIntOrNull() ?: return null
        
        return if (ChessUtils.isValidSquare(file, rank)) {
            Square(file, rank)
        } else {
            null
        }
    }
}