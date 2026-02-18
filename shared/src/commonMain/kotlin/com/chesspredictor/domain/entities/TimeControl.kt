package com.chesspredictor.domain.entities

import kotlinx.serialization.Serializable

@Serializable
enum class GameTimeControl(
    val displayName: String,
    val description: String,
    val typicalTimePerMove: String
) {
    BULLET("Bullet", "1+0, 2+1 games", "0.5-2 seconds"),
    BLITZ("Blitz", "3+0, 5+0, 3+2, 5+3 games", "1-8 seconds"),
    RAPID("Rapid", "10+0, 15+10, 30+0 games", "5-30 seconds"),
    CLASSICAL("Classical", "60+0, 90+30 games", "30-300 seconds");

    companion object {
        fun fromMoveTime(moveTimeMs: Int): GameTimeControl {
            return when {
                moveTimeMs <= 1000 -> BULLET
                moveTimeMs <= 3000 -> BLITZ
                moveTimeMs <= 15000 -> RAPID
                else -> CLASSICAL
            }
        }

        fun recommendedForElo(eloRating: Int): GameTimeControl {
            return when {
                eloRating < 1200 -> RAPID
                eloRating < 1600 -> BLITZ
                eloRating < 2000 -> BLITZ
                else -> BULLET
            }
        }
    }
}
