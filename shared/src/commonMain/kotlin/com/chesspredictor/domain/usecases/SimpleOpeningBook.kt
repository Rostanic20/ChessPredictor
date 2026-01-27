package com.chesspredictor.domain.usecases

data class WeightedMove(
    val move: String,
    val weight: Float,
    val name: String = ""
)

class SimpleOpeningBook {
    private val book = mapOf(
        com.chesspredictor.presentation.ChessConstants.STARTING_POSITION_FEN to listOf(
            WeightedMove("e2e4", 0.35f, "King's Pawn"),
            WeightedMove("d2d4", 0.30f, "Queen's Pawn"),
            WeightedMove("g1f3", 0.20f, "Réti Opening"),
            WeightedMove("c2c4", 0.10f, "English Opening"),
            WeightedMove("b1c3", 0.03f, "Nimzowitsch-Larsen"),
            WeightedMove("f2f4", 0.02f, "Bird's Opening")
        ),
        "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1" to listOf(
            WeightedMove("e7e5", 0.30f, "Open Game"),
            WeightedMove("c7c5", 0.25f, "Sicilian Defense"),
            WeightedMove("e7e6", 0.15f, "French Defense"),
            WeightedMove("c7c6", 0.10f, "Caro-Kann"),
            WeightedMove("d7d5", 0.10f, "Scandinavian"),
            WeightedMove("g7g6", 0.05f, "Modern Defense"),
            WeightedMove("d7d6", 0.05f, "Pirc Defense")
        ),
        "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1" to listOf(
            WeightedMove("d7d5", 0.30f, "Queen's Gambit"),
            WeightedMove("g8f6", 0.30f, "Indian Defense"),
            WeightedMove("e7e6", 0.15f, "Queen's Gambit Declined"),
            WeightedMove("f7f5", 0.10f, "Dutch Defense"),
            WeightedMove("g7g6", 0.10f, "King's Indian"),
            WeightedMove("c7c6", 0.05f, "Slav Defense")
        )
    )
    
    fun getBookMove(fen: String, skillLevel: Int): String? {
        val moves = book[fen] ?: return null
        
        if (skillLevel >= 18) {
            return selectWeightedRandom(moves)
        }
        
        val variety = 0.5f + (skillLevel / 20f) * 0.5f
        val threshold = (1.0f - variety) * 0.3f
        val filtered = moves.filter { it.weight >= threshold }
        
        return selectWeightedRandom(filtered.ifEmpty { moves })
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