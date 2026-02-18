package com.chesspredictor.data.utils

import com.chesspredictor.data.repositories.MoveParser
import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.utils.FenParser

class MoveParserImpl : MoveParser {

    private val fenParser = FenParser()

    override fun parseUciMove(uciMove: String, board: ChessBoard): ChessMove? {
        if (uciMove.length < 4) return null

        try {
            val fromFile = uciMove[0]
            val fromRank = uciMove[1].digitToInt()
            val toFile = uciMove[2]
            val toRank = uciMove[3].digitToInt()

            val from = Square(fromFile, fromRank)
            val to = Square(toFile, toRank)

            val gameState = fenParser.parseGameState(board.fen)

            val piece = gameState.board[from] ?: return null

            if (piece.color != gameState.turn) {
                return null
            }

            val promotion = if (uciMove.length == 5) {
                when (uciMove[4]) {
                    'q' -> ChessPiece.Queen(piece.color)
                    'r' -> ChessPiece.Rook(piece.color)
                    'b' -> ChessPiece.Bishop(piece.color)
                    'n' -> ChessPiece.Knight(piece.color)
                    else -> null
                }
            } else null

            val capturedPiece = gameState.board[to]

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
}