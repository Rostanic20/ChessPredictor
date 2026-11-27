package com.chesspredictor.domain.humanlike

import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.usecases.ChessRulesEngine
import com.chesspredictor.utils.ChessLogger
import kotlinx.coroutines.CancellationException
import kotlin.random.Random

class MaiaMoveChooser(
    private val predictor: HumanMovePredictor,
    private val rules: ChessRulesEngine = ChessRulesEngine(),
    private val random: Random = Random.Default
) {
    suspend fun choose(history: List<String>, selfElo: Int, oppoElo: Int, judge: MoveJudge? = null): ChessMove? {
        val fen = history.last()
        val state = rules.parseGameState(fen)
        val legal = rules.getLegalMoves(state).associateBy { it.toUci() }
        if (legal.isEmpty()) return null
        val model = MaiaPolicy.distribution(predictor.logits(history, selfElo, oppoElo), legal.keys.toList(), MaiaEncoding.blackToMove(fen))
        var candidates = model

        repeat(MAX_VETOES) {
            val uci = sample(candidates)
            if (judge == null || model.getValue(uci) >= SUSPICIOUS_PROBABILITY) return legal.getValue(uci)
            if (!isBlunder(judge, state, legal.getValue(uci))) return legal.getValue(uci)
            val rest = candidates - uci
            if (rest.isEmpty()) return legal.getValue(uci)
            val total = rest.values.sum()
            candidates = rest.mapValues { it.value / total }
        }
        return legal.getValue(candidates.maxBy { it.value }.key)
    }

    private suspend fun isBlunder(judge: MoveJudge, state: GameState, move: ChessMove): Boolean {
        val after = rules.makeMove(state, move) ?: return false
        return try {
            judge.winChanceLoss(state, after) > BLUNDER_LOSS
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ChessLogger.warning(TAG, "Move judge failed, keeping the move: ${e.message}")
            false
        }
    }

    private fun sample(distribution: Map<String, Double>): String {
        var remaining = random.nextDouble()
        for ((uci, probability) in distribution) {
            remaining -= probability
            if (remaining <= 0) return uci
        }
        return distribution.maxBy { it.value }.key
    }

    private companion object {
        const val TAG = "MaiaMoveChooser"
        const val SUSPICIOUS_PROBABILITY = 0.02
        const val BLUNDER_LOSS = 0.15
        const val MAX_VETOES = 4
    }
}
