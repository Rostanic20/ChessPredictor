package com.chesspredictor.domain.utils

object FenValidator {

    fun problemWith(fen: String): String? {
        val fields = fen.trim().split(Regex("\\s+"))
        if (fields.size < 4) return "a FEN needs at least board, side, castling and en passant fields"

        val ranks = fields[0].split("/")
        if (ranks.size != 8) return "the board must have 8 ranks"
        for ((index, rank) in ranks.withIndex()) {
            var files = 0
            for (char in rank) {
                files += when {
                    char in '1'..'8' -> char.digitToInt()
                    char in "pnbrqkPNBRQK" -> 1
                    else -> return "unexpected '$char' on the board"
                }
            }
            if (files != 8) return "rank ${8 - index} does not have 8 squares"
        }

        val board = fields[0]
        if (board.count { it == 'K' } != 1 || board.count { it == 'k' } != 1) return "each side needs exactly one king"
        if (ranks.first().any { it in "Pp" } || ranks.last().any { it in "Pp" }) return "pawns cannot stand on the first or last rank"

        if (fields[1] != "w" && fields[1] != "b") return "side to move must be w or b"

        val castling = fields[2]
        if (castling != "-" && (castling.any { it !in "KQkq" } || castling.toSet().size != castling.length)) {
            return "castling rights must be '-' or letters from KQkq"
        }

        val enPassant = fields[3]
        if (enPassant != "-" && !Regex("[a-h][36]").matches(enPassant)) return "en passant square must be on rank 3 or 6"

        return null
    }
}
