package com.chesspredictor.domain.constants

import com.chesspredictor.domain.entities.ChessPiece

/**
 * Standardized piece values used throughout the application.
 * 
 * Two scales are provided:
 * - Standard scale (100-based): Used for evaluation and analysis
 * - Simple scale (1-based): Used for quick calculations
 * 
 * Always prefer the standard scale for consistency.
 */
object PieceValues {
    // Standard centipawn values (100 = 1 pawn)
    const val PAWN_VALUE = 100
    const val KNIGHT_VALUE = 300
    const val BISHOP_VALUE = 300
    const val ROOK_VALUE = 500
    const val QUEEN_VALUE = 900
    const val KING_VALUE = 10000 // Arbitrarily high for checkmate scenarios
    
    // Simple scale for board analysis (1 = 1 pawn)
    const val PAWN_SIMPLE = 1
    const val KNIGHT_SIMPLE = 3
    const val BISHOP_SIMPLE = 3
    const val ROOK_SIMPLE = 5
    const val QUEEN_SIMPLE = 9
    const val KING_SIMPLE = 100 // High value but not used in material count
    
    /**
     * Get the standard value of a piece (100-based scale).
     */
    fun getValue(piece: ChessPiece): Int {
        return when (piece) {
            is ChessPiece.Pawn -> PAWN_VALUE
            is ChessPiece.Knight -> KNIGHT_VALUE
            is ChessPiece.Bishop -> BISHOP_VALUE
            is ChessPiece.Rook -> ROOK_VALUE
            is ChessPiece.Queen -> QUEEN_VALUE
            is ChessPiece.King -> KING_VALUE
        }
    }
    
    /**
     * Get the simple value of a piece (1-based scale).
     * Used for quick material balance calculations.
     */
    fun getSimpleValue(piece: ChessPiece): Int {
        return when (piece) {
            is ChessPiece.Pawn -> PAWN_SIMPLE
            is ChessPiece.Knight -> KNIGHT_SIMPLE
            is ChessPiece.Bishop -> BISHOP_SIMPLE
            is ChessPiece.Rook -> ROOK_SIMPLE
            is ChessPiece.Queen -> QUEEN_SIMPLE
            is ChessPiece.King -> KING_SIMPLE
        }
    }
    
    /**
     * Convert simple scale to standard scale.
     */
    fun simpleToStandard(simpleValue: Int): Int = simpleValue * 100
    
    /**
     * Convert standard scale to simple scale.
     */
    fun standardToSimple(standardValue: Int): Int = standardValue / 100
}