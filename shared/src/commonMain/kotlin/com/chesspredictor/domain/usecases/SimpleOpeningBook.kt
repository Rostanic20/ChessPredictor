package com.chesspredictor.domain.usecases

import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessOpening

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
            val matched = SanMoveMatcher.match(san, legalMoves, state.turn) ?: break

            val positionKey = state.toPositionKey()
            val uci = matched.toUci()
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