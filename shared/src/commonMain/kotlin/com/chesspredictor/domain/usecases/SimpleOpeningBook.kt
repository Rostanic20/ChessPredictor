package com.chesspredictor.domain.usecases

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessOpening
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.Square

data class WeightedMove(
    val move: String,
    val weight: Float,
    val name: String = ""
)

class SimpleOpeningBook {
    private val positionBook: Map<String, List<WeightedMove>> by lazy { buildBook() }

    fun getBookMove(fen: String, skillLevel: Int): String? {
        val positionKey = fenToPositionKey(fen)
        val moves = positionBook[positionKey] ?: return null

        if (skillLevel >= 18) {
            return selectWeightedRandom(moves)
        }

        val variety = 0.5f + (skillLevel / 20f) * 0.5f
        val threshold = (1.0f - variety) * 0.3f
        val filtered = moves.filter { it.weight >= threshold }

        return selectWeightedRandom(filtered.ifEmpty { moves })
    }

    private fun buildBook(): Map<String, List<WeightedMove>> {
        val engine = ChessRulesEngine()
        val bookMap = mutableMapOf<String, MutableList<WeightedMove>>()

        for (opening in AdvancedOpeningDatabase.openingDatabase) {
            val weight = calculateWeight(opening)
            replayOpening(engine, opening.moves, weight, opening.name, bookMap)
            for (transposition in opening.transpositions) {
                replayOpening(engine, transposition, weight, opening.name, bookMap)
            }
        }

        return bookMap.mapValues { (_, moves) ->
            moves.groupBy { it.move }
                .map { (uci, group) ->
                    WeightedMove(uci, group.sumOf { it.weight.toDouble() }.toFloat(), group.first().name)
                }
                .sortedByDescending { it.weight }
        }
    }

    private fun replayOpening(
        engine: ChessRulesEngine,
        sanMoves: List<String>,
        weight: Float,
        name: String,
        bookMap: MutableMap<String, MutableList<WeightedMove>>
    ) {
        val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        var state = engine.parseGameState(startFen)

        for (san in sanMoves) {
            val legalMoves = engine.getLegalMoves(state)
            val matched = matchSanToMove(san, legalMoves, state.turn) ?: break

            val positionKey = state.toPositionKey()
            val uci = toUci(matched)
            bookMap.getOrPut(positionKey) { mutableListOf() }.add(WeightedMove(uci, weight, name))

            state = engine.makeMove(state, matched) ?: break
        }
    }

    private fun calculateWeight(opening: ChessOpening): Float {
        val popularityWeight = opening.popularity.coerceAtLeast(0.1f)
        val mainLineBonus = if (opening.isMainLine) 1.2f else 1.0f
        val gamesBonus = (opening.statistics.totalGames / 50000f).coerceIn(0.5f, 2.0f)
        return popularityWeight * mainLineBonus * gamesBonus
    }

    private fun matchSanToMove(san: String, legalMoves: List<ChessMove>, turn: ChessColor): ChessMove? {
        val cleaned = san.replace("+", "").replace("#", "").replace("!", "").replace("?", "")

        if (cleaned == "O-O" || cleaned == "0-0") {
            val rank = if (turn == ChessColor.WHITE) 1 else 8
            return legalMoves.find { it.from == Square('e', rank) && it.to == Square('g', rank) }
        }
        if (cleaned == "O-O-O" || cleaned == "0-0-0") {
            val rank = if (turn == ChessColor.WHITE) 1 else 8
            return legalMoves.find { it.from == Square('e', rank) && it.to == Square('c', rank) }
        }

        var work = cleaned
        var promotionChar: Char? = null
        if (work.contains("=")) {
            val eqIndex = work.indexOf("=")
            promotionChar = work[eqIndex + 1]
            work = work.substring(0, eqIndex)
        }

        work = work.replace("x", "")

        val targetFile = work[work.length - 2]
        val targetRank = work[work.length - 1].digitToInt()
        val target = Square(targetFile, targetRank)

        val prefix = work.substring(0, work.length - 2)

        val isPawn = prefix.isEmpty() || (prefix.length == 1 && prefix[0].isLowerCase())

        val candidates = if (isPawn) {
            var pawns = legalMoves.filter { it.piece is ChessPiece.Pawn && it.to == target }
            if (prefix.isNotEmpty()) {
                val fileDisambig = prefix[0]
                pawns = pawns.filter { it.from.file == fileDisambig }
            }
            if (promotionChar != null) {
                pawns = pawns.filter { it.promotion != null && pieceMatchesChar(it.promotion, promotionChar) }
            } else {
                pawns = pawns.filter { it.promotion == null }
            }
            pawns
        } else {
            val pieceChar = prefix[0]
            val disambig = prefix.substring(1)
            var pieces = legalMoves.filter { it.to == target && pieceMatchesChar(it.piece, pieceChar) }
            if (disambig.isNotEmpty()) {
                for (c in disambig) {
                    pieces = if (c.isLetter()) {
                        pieces.filter { it.from.file == c }
                    } else {
                        pieces.filter { it.from.rank == c.digitToInt() }
                    }
                }
            }
            pieces
        }

        return candidates.firstOrNull()
    }

    private fun pieceMatchesChar(piece: ChessPiece, char: Char): Boolean {
        return when (char) {
            'K' -> piece is ChessPiece.King
            'Q' -> piece is ChessPiece.Queen
            'R' -> piece is ChessPiece.Rook
            'B' -> piece is ChessPiece.Bishop
            'N' -> piece is ChessPiece.Knight
            else -> false
        }
    }

    private fun toUci(move: ChessMove): String {
        val base = "${move.from.file}${move.from.rank}${move.to.file}${move.to.rank}"
        val promo = when (move.promotion) {
            is ChessPiece.Queen -> "q"
            is ChessPiece.Rook -> "r"
            is ChessPiece.Bishop -> "b"
            is ChessPiece.Knight -> "n"
            else -> ""
        }
        return base + promo
    }

    private fun fenToPositionKey(fen: String): String {
        val parts = fen.split(" ")
        return "${parts[0]} ${parts[1]} ${parts[2]} ${parts[3]}"
    }

    private fun selectWeightedRandom(moves: List<WeightedMove>): String {
        val totalWeight = moves.sumOf { it.weight.toDouble() }.toFloat()
        var random = kotlin.random.Random.nextFloat() * totalWeight

        for (move in moves) {
            random -= move.weight
            if (random <= 0) {
                return move.move
            }
        }

        return moves.last().move
    }
}