package com.chesspredictor.data.repositories

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.DetailedMove
import com.chesspredictor.domain.entities.Square

object SavedGameCodec {

    fun encodeMoves(moves: List<DetailedMove>): String = moves.joinToString(";") { detailed ->
        val move = detailed.move
        listOf(
            move.from.toString(),
            move.to.toString(),
            detailed.san,
            encodePiece(move.piece),
            detailed.moveNumber.toString(),
            detailed.isWhiteMove.toString(),
            move.promotion?.let(::encodePiece) ?: ""
        ).joinToString("|")
    }

    fun decodeMoves(serialized: String): List<DetailedMove> {
        if (serialized.isEmpty()) return emptyList()
        return serialized.split(";").mapIndexedNotNull { index, encoded ->
            runCatching {
                val parts = encoded.split("|")
                if (parts.size < 3) return@mapIndexedNotNull null
                val san = parts[2]
                val isWhiteMove = parts.getOrNull(5)?.toBooleanStrictOrNull() ?: (index % 2 == 0)
                val piece = parts.getOrNull(3)?.let(::decodePiece) ?: pieceFromSan(san, isWhiteMove)
                DetailedMove(
                    move = ChessMove(
                        from = Square.fromString(parts[0]),
                        to = Square.fromString(parts[1]),
                        piece = piece,
                        promotion = parts.getOrNull(6)?.takeIf { it.isNotEmpty() }?.let(::decodePiece)
                    ),
                    moveNumber = parts.getOrNull(4)?.toIntOrNull() ?: (index / 2 + 1),
                    isWhiteMove = isWhiteMove,
                    san = san
                )
            }.getOrNull()
        }
    }

    fun encodePieces(pieces: List<ChessPiece>): String = pieces.joinToString(",", transform = ::encodePiece)

    fun decodePieces(serialized: String): List<ChessPiece> {
        if (serialized.isEmpty()) return emptyList()
        return serialized.split(",").mapNotNull(::decodePiece)
    }

    private fun encodePiece(piece: ChessPiece): String {
        val type = when (piece) {
            is ChessPiece.Pawn -> 'P'
            is ChessPiece.Knight -> 'N'
            is ChessPiece.Bishop -> 'B'
            is ChessPiece.Rook -> 'R'
            is ChessPiece.Queen -> 'Q'
            is ChessPiece.King -> 'K'
        }
        return "$type${if (piece.color == ChessColor.WHITE) 'W' else 'B'}"
    }

    private fun decodePiece(code: String): ChessPiece? {
        if (code.length < 2) return null
        val color = if (code[1] == 'W') ChessColor.WHITE else ChessColor.BLACK
        return when (code[0]) {
            'P' -> ChessPiece.Pawn(color)
            'N' -> ChessPiece.Knight(color)
            'B' -> ChessPiece.Bishop(color)
            'R' -> ChessPiece.Rook(color)
            'Q' -> ChessPiece.Queen(color)
            'K' -> ChessPiece.King(color)
            else -> null
        }
    }

    private fun pieceFromSan(san: String, isWhiteMove: Boolean): ChessPiece {
        val color = if (isWhiteMove) ChessColor.WHITE else ChessColor.BLACK
        return when (san.firstOrNull()) {
            'O', 'K' -> ChessPiece.King(color)
            'Q' -> ChessPiece.Queen(color)
            'R' -> ChessPiece.Rook(color)
            'B' -> ChessPiece.Bishop(color)
            'N' -> ChessPiece.Knight(color)
            else -> ChessPiece.Pawn(color)
        }
    }
}
