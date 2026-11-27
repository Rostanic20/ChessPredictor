package com.chesspredictor.data.datasources

import com.chesspredictor.domain.entities.EngineDifficulty
import com.chesspredictor.domain.entities.EngineSettings

data class EngineStrength(
    val limitStrength: Boolean,
    val elo: Int?,
    val skillLevel: Int,
    val depth: Int
) {
    fun setupCommands(multiPV: Int): List<String> = listOfNotNull(
        "setoption name UCI_LimitStrength value $limitStrength",
        elo?.let { "setoption name UCI_Elo value $it" },
        "setoption name Skill Level value $skillLevel",
        "setoption name MultiPV value $multiPV"
    )

    fun goCommand(timeLimitMs: Int): String = buildString {
        append("go depth $depth")
        if (timeLimitMs > 0) append(" movetime ${timeLimitMs.coerceIn(300, 5000)}")
        if (timeLimitMs < 100) append(" nodes 100000")
    }

    companion object {
        const val MIN_UCI_ELO = 1320
        const val MAX_UCI_ELO = 3190

        fun of(settings: EngineSettings): EngineStrength {
            val difficulty = settings.difficulty
            val handicap = (difficulty.skillLevel - settings.skillLevel).coerceAtLeast(0)
            val targetElo = difficulty.eloRating - handicap * 100
            return when {
                difficulty == EngineDifficulty.MAXIMUM && handicap == 0 ->
                    EngineStrength(limitStrength = false, elo = null, skillLevel = 20, depth = settings.analysisDepth)
                targetElo >= MIN_UCI_ELO ->
                    EngineStrength(
                        limitStrength = true,
                        elo = targetElo.coerceAtMost(MAX_UCI_ELO),
                        skillLevel = 20,
                        depth = settings.analysisDepth
                    )
                else ->
                    EngineStrength(
                        limitStrength = false,
                        elo = null,
                        skillLevel = 0,
                        depth = ((targetElo - 700) / 100).coerceIn(1, 5)
                    )
            }
        }
    }
}
