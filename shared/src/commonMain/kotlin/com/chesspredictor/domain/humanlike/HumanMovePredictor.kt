package com.chesspredictor.domain.humanlike

import kotlin.math.exp

fun interface HumanMovePredictor {
    suspend fun logits(fens: List<String>, selfElo: Int, oppoElo: Int): FloatArray
}

object MaiaPolicy {
    fun distribution(logits: FloatArray, legalUci: List<String>, blackToMove: Boolean, temperature: Double = 1.0): Map<String, Double> {
        if (legalUci.isEmpty()) return emptyMap()
        val scores = legalUci.associateWith { logits[MaiaEncoding.moveIndex(it, blackToMove)] / temperature }
        val top = scores.values.max()
        val weights = scores.mapValues { exp(it.value - top) }
        val total = weights.values.sum()
        return weights.mapValues { it.value / total }
    }
}
