package com.chesspredictor.domain.entities

import kotlinx.serialization.Serializable

/**
 * Chess time control types for human-like behavior timing
 */
@Serializable
enum class GameTimeControl(
    val displayName: String,
    val description: String,
    val typicalTimePerMove: String // Human expectation
) {
    BULLET("Bullet", "1+0, 2+1 games", "0.5-2 seconds"),
    BLITZ("Blitz", "3+0, 5+0, 3+2, 5+3 games", "1-8 seconds"),
    RAPID("Rapid", "10+0, 15+10, 30+0 games", "5-30 seconds"),
    CLASSICAL("Classical", "60+0, 90+30 games", "30-300 seconds");
    
    companion object {
        /**
         * Determine time control based on time limit per move
         */
        fun fromMoveTime(moveTimeMs: Int): GameTimeControl {
            return when {
                moveTimeMs <= 1000 -> BULLET
                moveTimeMs <= 3000 -> BLITZ  
                moveTimeMs <= 15000 -> RAPID
                else -> CLASSICAL
            }
        }
        
        /**
         * Get recommended time control for ELO level
         * Lower rated players often prefer longer time controls
         */
        fun recommendedForElo(eloRating: Int): GameTimeControl {
            return when {
                eloRating < 1200 -> RAPID    // Beginners need time to think
                eloRating < 1600 -> BLITZ    // Club players comfortable with blitz
                eloRating < 2000 -> BLITZ    // Strong players play all formats
                else -> BULLET               // Masters can play bullet effectively
            }
        }
    }
}