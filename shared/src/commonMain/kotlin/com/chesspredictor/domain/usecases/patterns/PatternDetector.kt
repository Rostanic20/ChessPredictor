package com.chesspredictor.domain.usecases.patterns

import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.TacticalPattern

/**
 * Base interface for tactical pattern detectors.
 * Each implementation detects a specific type of tactical pattern.
 */
interface PatternDetector {
    /**
     * Detects patterns of this detector's type in the given position.
     * 
     * @param gameState The current game state to analyze
     * @return List of detected patterns, empty if none found
     */
    fun detect(gameState: GameState): List<TacticalPattern>
    
    /**
     * Returns the pattern types this detector can identify.
     * Used for filtering and categorization.
     */
    fun supportedPatternTypes(): Set<com.chesspredictor.domain.entities.PatternType>
}