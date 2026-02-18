package com.chesspredictor.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chesspredictor.domain.entities.*

@Composable
fun PositionComplexityCard(
    complexity: PositionComplexity,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") expanded: Boolean = false,
    @Suppress("UNUSED_PARAMETER") onExpandToggle: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Position Complexity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                ComplexityBadge(complexity.overallComplexity)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Complexity bars
            ComplexityMetrics(complexity)
            
            if (complexity.criticalFactors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                CriticalFactorsDisplay(complexity.criticalFactors)
            }
            
            // King safety indicator
            if (complexity.kingDanger.whiteKingSafety < 50f || 
                complexity.kingDanger.blackKingSafety < 50f) {
                Spacer(modifier = Modifier.height(12.dp))
                KingSafetyIndicator(complexity.kingDanger)
            }
            
            // Time importance
            if (complexity.timeImportance != TimeImportance.MODERATE) {
                Spacer(modifier = Modifier.height(8.dp))
                TimeImportanceIndicator(complexity.timeImportance)
            }
        }
    }
}

@Composable
private fun ComplexityBadge(level: ComplexityLevel) {
    val (backgroundColor, textColor) = when (level) {
        ComplexityLevel.SIMPLE -> Color(0xFF4CAF50) to Color.White
        ComplexityLevel.LOW -> Color(0xFF8BC34A) to Color.White
        ComplexityLevel.MODERATE -> Color(0xFFFFC107) to Color.Black
        ComplexityLevel.HIGH -> Color(0xFFFF9800) to Color.White
        ComplexityLevel.EXTREME -> Color(0xFFF44336) to Color.White
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = level.name,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ComplexityMetrics(complexity: PositionComplexity) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ComplexityBar(
            label = "Sharpness",
            value = complexity.sharpnessScore,
            icon = Icons.Default.Warning
        )
        ComplexityBar(
            label = "Tactical",
            value = complexity.tacticalComplexity,
            icon = Icons.Default.Star
        )
        ComplexityBar(
            label = "Strategic",
            value = complexity.strategicComplexity,
            icon = Icons.Default.List
        )
        if (complexity.materialImbalance > 20f) {
            ComplexityBar(
                label = "Imbalance",
                value = complexity.materialImbalance,
                icon = Icons.Default.Info
            )
        }
    }
}

@Composable
private fun ComplexityBar(
    label: String,
    value: Float,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(60.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val animatedValue by animateFloatAsState(targetValue = value / 100f)
            val color by animateColorAsState(
                targetValue = when {
                    value > 70 -> Color(0xFFF44336)
                    value > 40 -> Color(0xFFFFC107)
                    else -> Color(0xFF4CAF50)
                }
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedValue)
                    .fillMaxHeight()
                    .background(color)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "${value.toInt()}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CriticalFactorsDisplay(factors: List<CriticalFactor>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Critical Factors",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            factors.chunked(2).forEach { rowFactors ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowFactors.forEach { factor ->
                        CriticalFactorChip(factor)
                    }
                }
            }
        }
    }
}

@Composable
private fun CriticalFactorChip(factor: CriticalFactor) {
    val (icon, color) = when (factor) {
        CriticalFactor.KING_EXPOSED -> Icons.Default.Warning to Color(0xFFF44336)
        CriticalFactor.TACTICAL_SHOTS -> Icons.Default.Star to Color(0xFFFF9800)
        CriticalFactor.TIME_PRESSURE -> Icons.Default.Refresh to Color(0xFFF44336)
        CriticalFactor.MATERIAL_IMBALANCE -> Icons.Default.Add to Color(0xFFFFC107)
        CriticalFactor.PAWN_BREAKS -> Icons.Default.KeyboardArrowUp to Color(0xFF2196F3)
        CriticalFactor.FORCED_SEQUENCE -> Icons.Default.PlayArrow to Color(0xFF9C27B0)
        else -> Icons.Default.Info to MaterialTheme.colorScheme.primary
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        contentColor = color
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = factor.name.replace('_', ' ').lowercase()
                    .replaceFirstChar { it.uppercase() },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun KingSafetyIndicator(kingDanger: KingDangerLevel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        KingSafetyDisplay(
            color = ChessColor.WHITE,
            safety = kingDanger.whiteKingSafety
        )
        
        KingSafetyDisplay(
            color = ChessColor.BLACK,
            safety = kingDanger.blackKingSafety
        )
    }
}

@Composable
private fun KingSafetyDisplay(
    color: ChessColor,
    safety: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (color == ChessColor.WHITE) Color.White else Color.Black
                    )
                    .then(
                        if (color == ChessColor.WHITE) {
                            Modifier.background(Color.Gray.copy(alpha = 0.3f))
                        } else Modifier
                    )
            )
            
            Text(
                text = "${color.name} King",
                style = MaterialTheme.typography.labelSmall
            )
        }
        
        val safetyColor = when {
            safety < 30 -> Color(0xFFF44336)
            safety < 50 -> Color(0xFFFF9800)
            safety < 70 -> Color(0xFFFFC107)
            else -> Color(0xFF4CAF50)
        }
        
        Text(
            text = "${safety.toInt()}% safe",
            color = safetyColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TimeImportanceIndicator(importance: TimeImportance) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when (importance) {
                    TimeImportance.CRITICAL -> Color(0xFFF44336).copy(alpha = 0.1f)
                    TimeImportance.HIGH -> Color(0xFFFF9800).copy(alpha = 0.1f)
                    TimeImportance.LOW -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                    else -> Color.Transparent
                }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            tint = when (importance) {
                TimeImportance.CRITICAL -> Color(0xFFF44336)
                TimeImportance.HIGH -> Color(0xFFFF9800)
                TimeImportance.LOW -> Color(0xFF4CAF50)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(16.dp)
        )
        
        Text(
            text = when (importance) {
                TimeImportance.CRITICAL -> "Time is critical - think fast but accurately!"
                TimeImportance.HIGH -> "Time is important in this position"
                TimeImportance.LOW -> "Take your time - position is not time-sensitive"
                else -> "Normal time management applies"
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (importance == TimeImportance.CRITICAL) FontWeight.Bold else FontWeight.Normal
        )
    }
}

