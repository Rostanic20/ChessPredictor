package com.chesspredictor.domain.usecases.patterns

import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.TacticalPattern

interface PatternDetector {
    fun detect(gameState: GameState): List<TacticalPattern>

    fun supportedPatternTypes(): Set<com.chesspredictor.domain.entities.PatternType>
}
