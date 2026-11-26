package com.chesspredictor.domain.entities

enum class GameEnding {
    DRAW_AGREED,
    WHITE_OUT_OF_TIME,
    BLACK_OUT_OF_TIME;

    companion object {
        fun outOfTime(color: ChessColor): GameEnding =
            if (color == ChessColor.WHITE) WHITE_OUT_OF_TIME else BLACK_OUT_OF_TIME
    }
}
