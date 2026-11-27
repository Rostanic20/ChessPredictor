package com.chesspredictor.domain.humanlike

import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.usecases.ChessRulesEngine
import kotlin.random.Random

class MaiaMoveChooser(
    private val predictor: HumanMovePredictor,
    private val rules: ChessRulesEngine = ChessRulesEngine(),
    private val random: Random = Random.Default
) {
    suspend fun choose(history: List<String>, selfElo: Int, oppoElo: Int, temperature: Double = 1.0): ChessMove? {
        val fen = history.last()
        val legal = rules.getLegalMoves(rules.parseGameState(fen)).associateBy { it.toUci() }
        if (legal.isEmpty()) return null
        val distribution = MaiaPolicy.distribution(
            predictor.logits(history, selfElo, oppoElo),
            legal.keys.toList(),
            MaiaEncoding.blackToMove(fen),
            temperature
        )
        var remaining = random.nextDouble()
        for ((uci, probability) in distribution) {
            remaining -= probability
            if (remaining <= 0) return legal.getValue(uci)
        }
        return legal.getValue(distribution.maxBy { it.value }.key)
    }
}
