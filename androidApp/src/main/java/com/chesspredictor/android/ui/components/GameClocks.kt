package com.chesspredictor.android.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chesspredictor.domain.entities.ChessColor
import kotlinx.coroutines.flow.StateFlow

@Composable
fun GameClocks(
    remainingTimes: StateFlow<Map<ChessColor, Long>>,
    activeColor: ChessColor?,
    modifier: Modifier = Modifier
) {
    val remaining by remainingTimes.collectAsState()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChessColor.entries.forEach { color ->
            ClockFace(
                label = if (color == ChessColor.WHITE) "White" else "Black",
                remainingMs = remaining[color] ?: 0L,
                isActive = color == activeColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ClockFace(
    label: String,
    remainingMs: Long,
    isActive: Boolean,
    modifier: Modifier
) {
    val isLow = remainingMs < LOW_TIME_MS
    val container = when {
        isActive && isLow -> MaterialTheme.colorScheme.errorContainer
        isActive -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        modifier = modifier.border(
            width = 1.dp,
            color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
            shape = RoundedCornerShape(8.dp)
        ),
        shape = RoundedCornerShape(8.dp),
        color = container
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                formatClock(remainingMs),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

internal fun formatClock(ms: Long): String {
    val clamped = ms.coerceAtLeast(0)
    val totalSeconds = clamped / 1000
    val minutes = totalSeconds / 60
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return if (clamped < TENTHS_BELOW_MS) "$minutes:$seconds.${clamped % 1000 / 100}" else "$minutes:$seconds"
}

private const val LOW_TIME_MS = 30_000L
private const val TENTHS_BELOW_MS = 10_000L
