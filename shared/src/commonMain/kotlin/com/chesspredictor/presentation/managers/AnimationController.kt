package com.chesspredictor.presentation.managers

import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.GameState
import com.chesspredictor.domain.entities.Square
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AnimationController(
    private val animationManager: AnimationManager
) {
    private val _animations = MutableStateFlow<List<PieceAnimation>>(emptyList())
    val animations: StateFlow<List<PieceAnimation>> = _animations.asStateFlow()

    private val _isAnimating = MutableStateFlow(false)
    val isAnimatingState: StateFlow<Boolean> = _isAnimating.asStateFlow()

    private val _animatingPieces = MutableStateFlow<Set<Square>>(emptySet())
    val animatingPieces: StateFlow<Set<Square>> = _animatingPieces.asStateFlow()

    fun isAnimating(): Boolean = _isAnimating.value

    fun startMoveAnimation(move: ChessMove, oldState: GameState, newState: GameState, isFlipped: Boolean) {
        val anims = if (animationManager.shouldSkipAnimation(move.from, isFlipped)) {
            emptyList()
        } else {
            animationManager.createMoveAnimations(move, oldState, newState)
        }

        _animations.value = anims
        _isAnimating.value = anims.isNotEmpty()
        _animatingPieces.value = anims.map { it.fromSquare }.toSet()
    }

    fun clearAnimations() {
        _animations.value = emptyList()
        _isAnimating.value = false
        _animatingPieces.value = emptySet()
    }

    fun getAnimationDuration(): Long = AnimationManager.ANIMATION_DURATION_MS
}

data class PieceAnimation(
    val piece: ChessPiece,
    val fromSquare: Square,
    val toSquare: Square,
    val animationType: AnimationType,
    val isActive: Boolean = true,
    val animationId: String = "${fromSquare}_${toSquare}_${kotlin.random.Random.nextLong()}"
)

enum class AnimationType {
    STANDARD_MOVE,
    CAPTURE_MOVE,
    CASTLING_KING,
    CASTLING_ROOK,
    EN_PASSANT_MOVE,
    EN_PASSANT_CAPTURE,
    PROMOTION,
    PIECE_FADE_OUT
}
