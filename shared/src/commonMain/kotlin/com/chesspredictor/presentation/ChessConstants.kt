package com.chesspredictor.presentation

object ChessConstants {

    const val STARTING_POSITION_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    const val ANIMATION_DURATION_MS = 300L

    const val FLOW_SUBSCRIPTION_TIMEOUT_MS = 5000L

    const val INITIAL_EVALUATION = 0.0f

    object EngineReactionTime {
        const val MIN_MS = 100
        const val MAX_MS = 300
    }

    object EngineInit {
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 2000L
    }

    const val DEFAULT_MOVE_TIME_MS = 5000L

    object PieceThresholds {
        const val ENDGAME = 14
        const val MIDDLEGAME = 12
        const val OPENING = 28
        const val TACTICAL = 16
    }
}
