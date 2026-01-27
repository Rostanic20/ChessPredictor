package com.chesspredictor.domain.entities

import com.chesspredictor.domain.utils.ChessUtils

data class GameState(
    val board: Map<Square, ChessPiece>,
    val turn: ChessColor,
    val castlingRights: CastlingRights,
    val enPassantSquare: Square?,
    val halfMoveClock: Int,
    val fullMoveNumber: Int,
    val moveHistory: List<DetailedMove> = emptyList(),
    val capturedPieces: List<ChessPiece> = emptyList(),
    val positionHistory: List<String> = emptyList(),
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val isStalemate: Boolean = false,
    val isDraw: Boolean = false
) {
    fun toFen(): String {
        val fenBuilder = StringBuilder()

        for (rank in 8 downTo 1) {
            var emptyCount = 0
            for (file in 'a'..'h') {
                val square = Square(file, rank)
                val piece = board[square]
                
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        fenBuilder.append(emptyCount)
                        emptyCount = 0
                    }
                    fenBuilder.append(ChessUtils.pieceToChar(piece))
                }
            }
            if (emptyCount > 0) {
                fenBuilder.append(emptyCount)
            }
            if (rank > 1) fenBuilder.append('/')
        }
        
        fenBuilder.append(" ")
        fenBuilder.append(if (turn == ChessColor.WHITE) 'w' else 'b')
        fenBuilder.append(" ")
        
        val castling = buildString {
            if (castlingRights.whiteKingside) append('K')
            if (castlingRights.whiteQueenside) append('Q')
            if (castlingRights.blackKingside) append('k')
            if (castlingRights.blackQueenside) append('q')
        }
        fenBuilder.append(if (castling.isEmpty()) "-" else castling)
        fenBuilder.append(" ")
        
        fenBuilder.append(enPassantSquare?.toString() ?: "-")
        fenBuilder.append(" ")
        fenBuilder.append(halfMoveClock)
        fenBuilder.append(" ")
        fenBuilder.append(fullMoveNumber)
        
        return fenBuilder.toString()
    }
    
    fun toPositionKey(): String {
        val fenParts = toFen().split(" ")
        return "${fenParts[0]} ${fenParts[1]} ${fenParts[2]} ${fenParts[3]}"
    }
}

data class DetailedMove(
    val move: ChessMove,
    val moveNumber: Int,
    val isWhiteMove: Boolean,
    val isCapture: Boolean = false,
    val isCastling: CastlingType? = null,
    val isEnPassant: Boolean = false,
    val isPromotion: Boolean = false,
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val san: String = "",
    val previousCastlingRights: CastlingRights = CastlingRights(true, true, true, true),
    val previousEnPassantSquare: Square? = null,
    val timeSpent: Long? = null,
    val remainingTime: Long? = null,
    val evaluation: MoveAnalysis? = null,
    val promotion: ChessPiece? = null
)

enum class CastlingType {
    WHITE_KINGSIDE,
    WHITE_QUEENSIDE,
    BLACK_KINGSIDE,
    BLACK_QUEENSIDE
}