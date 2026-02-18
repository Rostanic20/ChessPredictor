package com.chesspredictor.domain.entities

data class ChessBoard(
    val fen: String,
    val moveHistory: List<ChessMove> = emptyList(),
    val turn: ChessColor,
    val castlingRights: CastlingRights,
    val enPassantSquare: Square? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1
)

data class Square(
    val file: Char,
    val rank: Int
) {
    init {
        require(file in 'a'..'h') { "File must be between a and h" }
        require(rank in 1..8) { "Rank must be between 1 and 8" }
    }
    
    override fun toString() = "$file$rank"
    
    companion object {
        fun fromString(notation: String): Square {
            require(notation.length == 2) { "Square notation must be 2 characters" }
            val file = notation[0]
            val rank = notation[1].digitToIntOrNull()
                ?: throw IllegalArgumentException("Invalid rank in notation: $notation")
            return Square(file, rank)
        }
    }
}

enum class ChessColor {
    WHITE, BLACK
}

fun ChessColor.opposite(): ChessColor {
    return if (this == ChessColor.WHITE) ChessColor.BLACK else ChessColor.WHITE
}

data class CastlingRights(
    val whiteKingside: Boolean,
    val whiteQueenside: Boolean,
    val blackKingside: Boolean,
    val blackQueenside: Boolean
)