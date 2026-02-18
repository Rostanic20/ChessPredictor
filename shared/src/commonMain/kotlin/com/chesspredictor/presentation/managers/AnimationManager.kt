package com.chesspredictor.presentation.managers

import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.Square
import kotlin.math.abs

class AnimationManager {
    
    companion object {
        const val ANIMATION_DURATION_MS = 300L
    }
    
    fun createMoveAnimations(
        move: ChessMove,
        oldGameState: GameState,
        @Suppress("UNUSED_PARAMETER") newGameState: GameState
    ): List<PieceAnimation> {
        val animations = mutableListOf<PieceAnimation>()
        
        val moveType = analyzeMoveType(move, oldGameState)

        animations.add(createMainAnimation(move, moveType))
        when (moveType) {
            MoveType.CASTLING -> addCastlingAnimations(animations, move, oldGameState)
            MoveType.EN_PASSANT -> addEnPassantAnimations(animations, move, oldGameState)
            MoveType.CAPTURE -> addCaptureAnimation(animations, move)
            else -> { }
        }
        
        return animations
    }
    
    fun shouldSkipAnimation(@Suppress("UNUSED_PARAMETER") square: Square, @Suppress("UNUSED_PARAMETER") isFlipped: Boolean = false): Boolean {
        return false
    }
    
    fun getGlitchedSquares(@Suppress("UNUSED_PARAMETER") isFlipped: Boolean = false): Set<Square> {
        return emptySet()
    }
    
    private fun analyzeMoveType(move: ChessMove, gameState: GameState): MoveType {
        return when {
            isCastling(move) -> MoveType.CASTLING
            isEnPassant(move, gameState) -> MoveType.EN_PASSANT
            move.promotion != null -> MoveType.PROMOTION
            move.capturedPiece != null -> MoveType.CAPTURE
            else -> MoveType.STANDARD
        }
    }
    
    private fun isCastling(move: ChessMove): Boolean {
        return move.piece is ChessPiece.King && abs(move.from.file - move.to.file) == 2
    }
    
    private fun isEnPassant(move: ChessMove, gameState: GameState): Boolean {
        return move.piece is ChessPiece.Pawn &&
               move.to == gameState.enPassantSquare
    }
    
    private fun createMainAnimation(move: ChessMove, moveType: MoveType): PieceAnimation {
        val animationType = when (moveType) {
            MoveType.CASTLING -> AnimationType.CASTLING_KING
            MoveType.EN_PASSANT -> AnimationType.EN_PASSANT_MOVE
            MoveType.PROMOTION -> AnimationType.PROMOTION
            MoveType.CAPTURE -> AnimationType.CAPTURE_MOVE
            MoveType.STANDARD -> AnimationType.STANDARD_MOVE
        }
        
        return PieceAnimation(
            piece = move.piece,
            fromSquare = move.from,
            toSquare = move.to,
            animationType = animationType
        )
    }
    
    private fun addCastlingAnimations(
        animations: MutableList<PieceAnimation>,
        move: ChessMove,
        gameState: GameState
    ) {
        val isKingside = move.to.file > move.from.file
        val rookFromFile = if (isKingside) 'h' else 'a'
        val rookToFile = if (isKingside) 'f' else 'd'
        val rank = move.from.rank
        
        val rookFromSquare = Square(rookFromFile, rank)
        val rookToSquare = Square(rookToFile, rank)
        val rook = gameState.board[rookFromSquare]
        
        if (rook != null) {
            animations.add(
                PieceAnimation(
                    piece = rook,
                    fromSquare = rookFromSquare,
                    toSquare = rookToSquare,
                    animationType = AnimationType.CASTLING_ROOK
                )
            )
        }
    }
    
    private fun addEnPassantAnimations(
        animations: MutableList<PieceAnimation>,
        move: ChessMove,
        gameState: GameState
    ) {
        val capturedPawnSquare = Square(move.to.file, move.from.rank)
        val capturedPawn = gameState.board[capturedPawnSquare]
        
        if (capturedPawn != null) {
            animations.add(
                PieceAnimation(
                    piece = capturedPawn,
                    fromSquare = capturedPawnSquare,
                    toSquare = capturedPawnSquare,
                    animationType = AnimationType.EN_PASSANT_CAPTURE
                )
            )
        }
    }
    
    private fun addCaptureAnimation(
        animations: MutableList<PieceAnimation>,
        move: ChessMove
    ) {
        if (move.capturedPiece != null) {
            animations.add(
                PieceAnimation(
                    piece = move.capturedPiece,
                    fromSquare = move.to,
                    toSquare = move.to,
                    animationType = AnimationType.PIECE_FADE_OUT
                )
            )
        }
    }
    
    private enum class MoveType {
        STANDARD,
        CAPTURE,
        CASTLING,
        EN_PASSANT,
        PROMOTION
    }
}