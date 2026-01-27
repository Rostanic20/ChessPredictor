package com.chesspredictor.domain.usecases.components

import com.chesspredictor.domain.entities.GameState

class TranspositionTable {
    private val table = mutableMapOf<String, String>()
    
    fun get(gameState: GameState): String? {
        val key = gameState.toFen()
        return table[key]
    }
    
    fun put(gameState: GameState, value: String) {
        val key = gameState.toFen()
        table[key] = value
    }
    
    fun clear() {
        table.clear()
    }
    
    fun size(): Int {
        return table.size
    }
}