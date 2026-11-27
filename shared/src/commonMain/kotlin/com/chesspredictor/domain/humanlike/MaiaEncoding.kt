package com.chesspredictor.domain.humanlike

object MaiaEncoding {
    const val HISTORY = 8
    const val SQUARES = 64
    const val FEATURES = 12 * HISTORY + 1
    const val MOVE_COUNT = 64 * 64 + 8 * 8 * 4

    private const val PIECES = "pnbrqk"
    private const val PROMOTIONS = "qrbn"

    fun tokens(fens: List<String>): FloatArray {
        require(fens.isNotEmpty()) { "at least the current position is needed" }
        val recent = fens.takeLast(HISTORY)
        val padded = List(HISTORY - recent.size) { recent.first() } + recent
        val out = FloatArray(SQUARES * FEATURES)
        padded.forEachIndexed { slot, fen -> writePlanes(fen, out, slot * 12) }
        return out
    }

    fun blackToMove(fen: String): Boolean = fen.split(' ').getOrNull(1) == "b"

    fun moveIndex(uci: String, blackToMove: Boolean): Int {
        val move = if (blackToMove) mirror(uci) else uci
        if (move.length == 5) {
            val fromFile = move[0] - 'a'
            val toFile = move[2] - 'a'
            return 64 * 64 + (fromFile * 8 + toFile) * 4 + PROMOTIONS.indexOf(move[4])
        }
        return square(move, 0) * 64 + square(move, 2)
    }

    fun uciOf(index: Int, blackToMove: Boolean): String {
        val move = if (index >= 64 * 64) {
            val promotion = index - 64 * 64
            "${'a' + promotion / 32}7${'a' + promotion / 4 % 8}8${PROMOTIONS[promotion % 4]}"
        } else {
            squareName(index / 64) + squareName(index % 64)
        }
        return if (blackToMove) mirror(move) else move
    }

    private fun writePlanes(fen: String, out: FloatArray, offset: Int) {
        val fields = fen.split(' ')
        val mirrored = fields.getOrNull(1) == "b"
        var rank = 7
        var file = 0
        for (symbol in fields[0]) {
            when {
                symbol == '/' -> {
                    rank--
                    file = 0
                }
                symbol.isDigit() -> file += symbol.digitToInt()
                else -> {
                    val type = PIECES.indexOf(symbol.lowercaseChar())
                    val whiteAfterMirror = symbol.isUpperCase() != mirrored
                    val squareIndex = (if (mirrored) 7 - rank else rank) * 8 + file
                    out[squareIndex * FEATURES + offset + type + if (whiteAfterMirror) 0 else 6] = 1f
                    file++
                }
            }
        }
    }

    private fun mirror(uci: String): String =
        "${uci[0]}${'1' + ('8' - uci[1])}${uci[2]}${'1' + ('8' - uci[3])}${uci.drop(4)}"

    private fun square(uci: String, at: Int): Int = (uci[at + 1] - '1') * 8 + (uci[at] - 'a')

    private fun squareName(index: Int): String = "${'a' + index % 8}${'1' + index / 8}"
}
