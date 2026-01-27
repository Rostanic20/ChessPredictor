package com.chesspredictor.android.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chesspredictor.domain.entities.*

/**
 * Displays the engine's current emotional state with appropriate icon and color
 */
@Composable
fun EmotionalStateDisplay(
    emotionalState: EmotionalState,
    modifier: Modifier = Modifier
) {
    val (icon, color, description) = when (emotionalState) {
        EmotionalState.CONFIDENT -> Triple(Icons.Default.TrendingUp, Color(0xFF4CAF50), "Confident")
        EmotionalState.WORRIED -> Triple(Icons.Default.Warning, Color(0xFFFF9800), "Worried")
        EmotionalState.EXCITED -> Triple(Icons.Default.FlashOn, Color(0xFFE91E63), "Excited")
        EmotionalState.FRUSTRATED -> Triple(Icons.Default.Close, Color(0xFFF44336), "Frustrated")
        EmotionalState.SURPRISED -> Triple(Icons.Default.Help, Color(0xFF9C27B0), "Surprised")
        EmotionalState.FOCUSED -> Triple(Icons.Default.Visibility, Color(0xFF2196F3), "Focused")
        EmotionalState.PRESSURED -> Triple(Icons.Default.Timer, Color(0xFFFF5722), "Under Pressure")
        EmotionalState.SATISFIED -> Triple(Icons.Default.CheckCircle, Color(0xFF8BC34A), "Satisfied")
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Displays move commentary from the engine
 */
@Composable
fun MoveCommentaryDisplay(
    commentary: MoveCommentary?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = commentary != null,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        commentary?.let { comment ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = getCommentaryBackgroundColor(comment.type)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = getCommentaryIcon(comment.type),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = getCommentaryTypeLabel(comment.type),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        
                        // Confidence indicator
                        ConfidenceIndicator(
                            confidence = comment.confidence,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    
                    Text(
                        text = comment.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Shows when the engine is "thinking" with animated indicator
 */
@Composable
fun ThinkingProcessIndicator(
    isShowingThoughts: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isShowingThoughts,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Animated thinking dots
                ThinkingDots()
                
                Text(
                    text = "Thinking...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/**
 * Animated dots to show thinking process
 */
@Composable
private fun ThinkingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = alpha)
                    )
            )
        }
    }
}

/**
 * Shows confidence level as a visual indicator
 */
@Composable
private fun ConfidenceIndicator(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val color = when {
        confidence >= 0.8f -> Color(0xFF4CAF50)
        confidence >= 0.6f -> Color(0xFFFF9800)
        confidence >= 0.4f -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color)
    )
}

/**
 * Combined panel showing all human behavior features
 */
@Composable
fun HumanBehaviorPanel(
    emotionalState: EmotionalState,
    commentary: MoveCommentary?,
    isShowingThoughts: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Engine Behavior",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        EmotionalStateDisplay(emotionalState = emotionalState)
        
        ThinkingProcessIndicator(isShowingThoughts = isShowingThoughts)
        
        MoveCommentaryDisplay(commentary = commentary)
    }
}

// Helper functions

@Composable
private fun getCommentaryBackgroundColor(type: CommentaryType): Color {
    return when (type) {
        CommentaryType.TEACHING_MOMENT -> MaterialTheme.colorScheme.primaryContainer
        CommentaryType.EMOTIONAL_REACTION -> MaterialTheme.colorScheme.secondaryContainer
        CommentaryType.MISTAKE_ACKNOWLEDGMENT -> MaterialTheme.colorScheme.errorContainer
        CommentaryType.TACTICAL_OBSERVATION -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

private fun getCommentaryIcon(type: CommentaryType): ImageVector {
    return when (type) {
        CommentaryType.MOVE_EXPLANATION -> Icons.Default.Info
        CommentaryType.POSITION_ASSESSMENT -> Icons.Default.Analytics
        CommentaryType.EMOTIONAL_REACTION -> Icons.Default.SentimentSatisfied
        CommentaryType.TEACHING_MOMENT -> Icons.Default.School
        CommentaryType.TACTICAL_OBSERVATION -> Icons.Default.Visibility
        CommentaryType.STRATEGIC_PLAN -> Icons.Default.TrendingUp
        CommentaryType.TIME_PRESSURE_COMMENT -> Icons.Default.Timer
        CommentaryType.MISTAKE_ACKNOWLEDGMENT -> Icons.Default.Error
    }
}

private fun getCommentaryTypeLabel(type: CommentaryType): String {
    return when (type) {
        CommentaryType.MOVE_EXPLANATION -> "Move"
        CommentaryType.POSITION_ASSESSMENT -> "Position"
        CommentaryType.EMOTIONAL_REACTION -> "Reaction"
        CommentaryType.TEACHING_MOMENT -> "Teaching"
        CommentaryType.TACTICAL_OBSERVATION -> "Tactics"
        CommentaryType.STRATEGIC_PLAN -> "Strategy"
        CommentaryType.TIME_PRESSURE_COMMENT -> "Time"
        CommentaryType.MISTAKE_ACKNOWLEDGMENT -> "Mistake"
    }
}