package com.chesspredictor.presentation

/**
 * Constants used throughout the chess application.
 */
object ChessConstants {
    
    /**
     * Standard starting position in FEN notation.
     * Represents the initial chess board setup.
     */
    const val STARTING_POSITION_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    
    /**
     * Animation duration in milliseconds for piece movements.
     */
    const val ANIMATION_DURATION_MS = 300L
    
    /**
     * Time to wait for UI state to be subscribed before stopping flow collection.
     */
    const val FLOW_SUBSCRIPTION_TIMEOUT_MS = 5000L
    
    /**
     * Initial position evaluation score (neutral position).
     */
    const val INITIAL_EVALUATION = 0.0f
    
    /**
     * Human-like reaction time delays for engine moves.
     */
    object EngineReactionTime {
        const val MIN_MS = 100
        const val MAX_MS = 300
    }

    /**
     * Engine initialization settings.
     */
    object EngineInit {
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 2000L
    }

    /**
     * Default fallback value when move timing is unknown.
     */
    const val DEFAULT_MOVE_TIME_MS = 5000L

    /**
     * Piece count thresholds for game phase detection.
     * Based on counting letter characters in FEN notation.
     */
    object PieceThresholds {
        /** Below this count, the position is considered an endgame */
        const val ENDGAME = 14
        /** Below this count, the position is considered middlegame (not opening) */
        const val MIDDLEGAME = 12
        /** Above this count, the position is still in the opening */
        const val OPENING = 28
        /** Above this count, tactical opportunities are more likely */
        const val TACTICAL = 16
    }
}