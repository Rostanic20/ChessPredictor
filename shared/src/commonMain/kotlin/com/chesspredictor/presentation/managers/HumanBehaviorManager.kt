package com.chesspredictor.presentation.managers

import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.CommentaryType
import com.chesspredictor.domain.entities.EmotionalState
import com.chesspredictor.domain.entities.GameTimeControl
import com.chesspredictor.domain.entities.HumanBehaviorProfile
import com.chesspredictor.domain.entities.MoveCommentary
import com.chesspredictor.presentation.ChessConstants
import com.chesspredictor.utils.ChessLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class HumanBehaviorManager(
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "HumanBehaviorManager"
        private const val BASE_THINKING_TIME = 1000L
        private const val MAX_THINKING_TIME = 8000L
        private const val MIN_THINKING_TIME = 100L

        private const val BLITZ_BASE_TIME = 600L
        private const val BLITZ_MAX_TIME = 3000L
        private const val BULLET_BASE_TIME = 300L
        private const val BULLET_MAX_TIME = 1500L

        private const val BULLET_COMPLEXITY_THRESHOLD = 0.7f
        private const val BLITZ_COMPLEXITY_THRESHOLD = 0.8f
        private const val HIGH_CONSISTENCY_THRESHOLD = 0.7f

        private const val BULLET_HIGH_COMPLEXITY_MULTIPLIER = 1.8f
        private const val BULLET_LOW_COMPLEXITY_MULTIPLIER = 0.6f
        private const val BLITZ_HIGH_COMPLEXITY_MULTIPLIER = 2.0f
        private const val BLITZ_LOW_COMPLEXITY_MULTIPLIER = 0.8f
    }
    
    private val _currentEmotionalState = MutableStateFlow(EmotionalState.FOCUSED)
    val currentEmotionalState: StateFlow<EmotionalState> = _currentEmotionalState.asStateFlow()
    
    private val _lastCommentary = MutableStateFlow<MoveCommentary?>(null)
    val lastCommentary: StateFlow<MoveCommentary?> = _lastCommentary.asStateFlow()
    
    private val _isShowingThoughts = MutableStateFlow(false)
    val isShowingThoughts: StateFlow<Boolean> = _isShowingThoughts.asStateFlow()
    
    private var gameHistory = mutableListOf<Float>()
    private var lastMoveTime = 0L
    private var consecutiveBadMoves = 0
    
    fun calculateDisplayThinkingTime(
        profile: HumanBehaviorProfile,
        position: ChessBoard,
        actualEngineTime: Long,
        timeControl: GameTimeControl = GameTimeControl.CLASSICAL
    ): Long {
        val complexity = estimatePositionComplexity(position)
        val currentEval = gameHistory.lastOrNull() ?: 0f

        val (baseTime, maxTime) = when (timeControl) {
            GameTimeControl.BULLET -> BULLET_BASE_TIME to BULLET_MAX_TIME
            GameTimeControl.BLITZ -> BLITZ_BASE_TIME to BLITZ_MAX_TIME
            GameTimeControl.RAPID -> BASE_THINKING_TIME * 2 to MAX_THINKING_TIME * 2
            GameTimeControl.CLASSICAL -> BASE_THINKING_TIME to MAX_THINKING_TIME
        }
        
        val basePersonalityTime = baseTime * (0.5f + profile.thinkingSpeed)

        val complexityMultiplier = 1f + (complexity * profile.complexityReaction)

        val pressureMultiplier = if (isPositionDeteriorating()) {
            1f + (1f - profile.pressureResponse) * 0.5f
        } else {
            1f
        }

        val timePressureMultiplier = when (timeControl) {
            GameTimeControl.BULLET -> {
                if (complexity > BULLET_COMPLEXITY_THRESHOLD) BULLET_HIGH_COMPLEXITY_MULTIPLIER else BULLET_LOW_COMPLEXITY_MULTIPLIER
            }
            GameTimeControl.BLITZ -> {
                if (complexity > BLITZ_COMPLEXITY_THRESHOLD) BLITZ_HIGH_COMPLEXITY_MULTIPLIER else BLITZ_LOW_COMPLEXITY_MULTIPLIER
            }
            else -> 1f
        }

        val consistencyVariation = if (profile.timeConsistency > HIGH_CONSISTENCY_THRESHOLD) {
            Random.nextFloat() * 0.3f + 0.85f
        } else {
            Random.nextFloat() * 0.8f + 0.6f
        }

        val finalTime = (basePersonalityTime * complexityMultiplier *
                        pressureMultiplier * timePressureMultiplier * consistencyVariation).toLong()

        return finalTime.coerceIn(MIN_THINKING_TIME, maxTime)
    }
    
    fun calculateBlitzThinkingTime(
        profile: HumanBehaviorProfile,
        position: ChessBoard,
        actualEngineTime: Long
    ): Long {
        return calculateDisplayThinkingTime(profile, position, actualEngineTime, GameTimeControl.BLITZ)
    }
    
    fun calculateBulletThinkingTime(
        profile: HumanBehaviorProfile,
        position: ChessBoard,
        actualEngineTime: Long
    ): Long {
        return calculateDisplayThinkingTime(profile, position, actualEngineTime, GameTimeControl.BULLET)
    }
    
    fun updateEmotionalState(
        profile: HumanBehaviorProfile,
        position: ChessBoard,
        lastMove: ChessMove?,
        evaluation: Float
    ) {
        gameHistory.add(evaluation)
        if (gameHistory.size > 10) gameHistory.removeAt(0)

        val newState = when {
            evaluation > 2.0f && profile.emotionalExpressiveness > 0.6f -> {
                EmotionalState.CONFIDENT
            }
            evaluation < -2.0f && profile.emotionalExpressiveness > 0.5f -> {
                EmotionalState.WORRIED
            }
            isTacticalPosition(position) && profile.tacticalAggression > 0.7f -> {
                EmotionalState.EXCITED
            }
            isPositionDeteriorating() && profile.pressureResponse < 0.4f -> {
                EmotionalState.FRUSTRATED
            }
            kotlin.math.abs(evaluation) < 0.5f -> {
                EmotionalState.FOCUSED
            }
            else -> EmotionalState.FOCUSED
        }
        
        _currentEmotionalState.value = newState
        ChessLogger.info(TAG, "Emotional state: $newState (eval: $evaluation)")
    }
    
    fun generateMoveCommentary(
        profile: HumanBehaviorProfile,
        position: ChessBoard,
        move: ChessMove,
        evaluation: Float
    ): MoveCommentary? {
        if (Random.nextFloat() > profile.chattiness) return null

        val commentary = when {
            profile.teachingMode && isTacticalPosition(position) -> {
                MoveCommentary(
                    text = generateTacticalExplanation(move, position),
                    type = CommentaryType.TEACHING_MOMENT,
                    emotionalState = _currentEmotionalState.value,
                    confidence = profile.confidenceLevel
                )
            }

            evaluation > (gameHistory.getOrNull(gameHistory.size - 2) ?: 0f) + 1f -> {
                MoveCommentary(
                    text = generatePositiveReaction(profile),
                    type = CommentaryType.EMOTIONAL_REACTION,
                    emotionalState = EmotionalState.SATISFIED,
                    confidence = profile.confidenceLevel + 0.2f
                )
            }

            evaluation < (gameHistory.getOrNull(gameHistory.size - 2) ?: 0f) - 1f &&
            profile.weaknessesAware -> {
                consecutiveBadMoves++
                MoveCommentary(
                    text = generateMistakeAcknowledgment(consecutiveBadMoves),
                    type = CommentaryType.MISTAKE_ACKNOWLEDGMENT,
                    emotionalState = EmotionalState.FRUSTRATED,
                    confidence = profile.confidenceLevel - 0.3f
                )
            }

            !isEndgamePosition(position) && Random.nextFloat() < 0.3f -> {
                MoveCommentary(
                    text = generateStrategicComment(move, position, profile),
                    type = CommentaryType.STRATEGIC_PLAN,
                    emotionalState = _currentEmotionalState.value,
                    confidence = profile.confidenceLevel
                )
            }
            
            else -> null
        }
        
        commentary?.let {
            _lastCommentary.value = it
            ChessLogger.info(TAG, "Commentary: ${it.text}")
        }
        
        return commentary
    }
    
    suspend fun simulateThinkingProcess(
        profile: HumanBehaviorProfile,
        thinkingTime: Long
    ) {
        if (!profile.showThoughts) return
        
        _isShowingThoughts.value = true

        if (thinkingTime > 3000L) {
            delay(thinkingTime / 3)
            delay(thinkingTime / 3)
        }
        
        _isShowingThoughts.value = false
    }

    private fun generateTacticalExplanation(move: ChessMove, position: ChessBoard): String {
        val explanations = listOf(
            "I see a tactical opportunity here with ${move.piece}",
            "This move creates some threats I want to explore",
            "There might be a combination starting with this move",
            "I'm attacking the ${move.capturedPiece ?: "position"} structure"
        )
        return explanations.random()
    }
    
    private fun generatePositiveReaction(profile: HumanBehaviorProfile): String {
        val reactions = if (profile.celebratesGoodMoves) {
            listOf("Nice! I like this position", "Good move!", "This feels right", "I'm happy with this")
        } else {
            listOf("Interesting position", "This looks reasonable", "Making progress")
        }
        return reactions.random()
    }
    
    private fun generateMistakeAcknowledgment(consecutiveBad: Int): String {
        return when (consecutiveBad) {
            1 -> listOf("Hmm, not sure about that", "Could be better", "Might have missed something").random()
            2 -> listOf("I'm not playing my best", "Need to focus more", "Making some errors").random()
            else -> listOf("Having a tough game", "Really struggling here", "Not my day").random()
        }
    }
    
    private fun generateStrategicComment(move: ChessMove, position: ChessBoard, profile: HumanBehaviorProfile): String {
        val style = if (profile.tacticalAggression > 0.6f) {
            listOf("Looking for active play", "Keeping the initiative", "Staying aggressive")
        } else {
            listOf("Building a solid position", "Improving piece coordination", "Playing it safe")
        }
        return style.random()
    }
    
    private fun estimatePositionComplexity(position: ChessBoard): Float {
        val pieceCount = position.fen.count { it.isLetter() }
        return when {
            pieceCount < ChessConstants.PieceThresholds.MIDDLEGAME -> 0.3f
            pieceCount > ChessConstants.PieceThresholds.OPENING -> 0.8f
            else -> 0.6f
        }
    }

    private fun isTacticalPosition(position: ChessBoard): Boolean {
        return position.fen.count { it.isLetter() } > ChessConstants.PieceThresholds.TACTICAL && Random.nextFloat() < 0.4f
    }

    private fun isEndgamePosition(position: ChessBoard): Boolean {
        return position.fen.count { it.isLetter() } < ChessConstants.PieceThresholds.ENDGAME
    }
    
    private fun isPositionDeteriorating(): Boolean {
        if (gameHistory.size < 3) return false
        val recent = gameHistory.takeLast(3)
        return recent[0] > recent[1] && recent[1] > recent[2]
    }
}