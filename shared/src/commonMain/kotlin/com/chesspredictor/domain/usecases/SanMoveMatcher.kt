package com.chesspredictor.domain.usecases

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.Square

object SanMoveMatcher {

    fun match(san: String, legalMoves: List<ChessMove>, turn: ChessColor): ChessMove? {
        val cleaned = san.replace("+", "").replace("#", "").replace("!", "").replace("?", "")

        if (cleaned == "O-O" || cleaned == "0-0") {
            val rank = if (turn == ChessColor.WHITE) 1 else 8
            return legalMoves.find { it.piece is ChessPiece.King && it.from == Square('e', rank) && it.to == Square('g', rank) }
        }
        if (cleaned == "O-O-O" || cleaned == "0-0-0") {
            val rank = if (turn == ChessColor.WHITE) 1 else 8
            return legalMoves.find { it.piece is ChessPiece.King && it.from == Square('e', rank) && it.to == Square('c', rank) }
        }

        var work = cleaned
        var promotionChar: Char? = null
        if (work.contains("=")) {
            val eqIndex = work.indexOf("=")
            promotionChar = work.getOrNull(eqIndex + 1) ?: return null
            work = work.substring(0, eqIndex)
        }

        work = work.replace("x", "")
        if (work.length < 2) return null

        val targetRank = work[work.length - 1].digitToIntOrNull() ?: return null
        val targetFile = work[work.length - 2]
        if (targetFile !in 'a'..'h' || targetRank !in 1..8) return null
        val target = Square(targetFile, targetRank)

        val prefix = work.substring(0, work.length - 2)
        val isPawn = prefix.isEmpty() || (prefix.length == 1 && prefix[0].isLowerCase())

        val candidates = if (isPawn) {
            var pawns = legalMoves.filter { it.piece is ChessPiece.Pawn && it.to == target }
            if (prefix.isNotEmpty()) {
                pawns = pawns.filter { it.from.file == prefix[0] }
            }
            pawns.filter { move ->
                if (promotionChar == null) move.promotion == null
                else move.promotion?.let { pieceMatchesChar(it, promotionChar) } == true
            }
        } else {
            var pieces = legalMoves.filter { it.to == target && pieceMatchesChar(it.piece, prefix[0]) }
            for (c in prefix.substring(1)) {
                pieces = if (c.isLetter()) {
                    pieces.filter { it.from.file == c }
                } else {
                    pieces.filter { it.from.rank == c.digitToIntOrNull() }
                }
            }
            pieces
        }

        return candidates.singleOrNull()
    }

    private fun pieceMatchesChar(piece: ChessPiece, char: Char): Boolean = when (char) {
        'K' -> piece is ChessPiece.King
        'Q' -> piece is ChessPiece.Queen
        'R' -> piece is ChessPiece.Rook
        'B' -> piece is ChessPiece.Bishop
        'N' -> piece is ChessPiece.Knight
        else -> false
    }
}
