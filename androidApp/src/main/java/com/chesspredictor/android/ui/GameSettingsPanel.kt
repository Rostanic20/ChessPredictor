package com.chesspredictor.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chesspredictor.domain.entities.EngineDifficulty
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.entities.TimeControl
import com.chesspredictor.presentation.managers.PlayMode
import com.chesspredictor.presentation.managers.PlayerColor
import com.chesspredictor.presentation.viewmodels.ChessBoardUiState

@Composable
internal fun GameSettingsPanel(
    uiState: ChessBoardUiState,
    onPlayModeChange: (PlayMode) -> Unit,
    onPlayerColorChange: (PlayerColor) -> Unit,
    onEngineSettingsChange: (EngineSettings) -> Unit,
    onTimeControlChange: (TimeControl?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PlayModeSelector(
            currentMode = uiState.playMode,
            onModeChange = onPlayModeChange
        )

        if (uiState.playMode == PlayMode.VS_ENGINE) {
            PlayerColorSelector(
                currentColor = uiState.playerColor,
                onColorChange = onPlayerColorChange
            )
        }

        TimeControlSelector(
            current = uiState.timeControl,
            onSelect = onTimeControlChange
        )

        DifficultySettings(
            currentSettings = uiState.engineSettings,
            onSettingsChange = onEngineSettingsChange
        )
    }
}

@Composable
private fun TimeControlSelector(
    current: TimeControl?,
    onSelect: (TimeControl?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Clock",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            val options: List<TimeControl?> = listOf(null) + TimeControl.PRESETS
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { option ->
                            FilterChip(
                                selected = current == option,
                                onClick = { onSelect(option) },
                                label = { Text(option?.label ?: "No clock") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayModeSelector(
    currentMode: PlayMode,
    onModeChange: (PlayMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Play Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayMode.values().forEach { mode ->
                    FilterChip(
                        selected = currentMode == mode,
                        onClick = { onModeChange(mode) },
                        label = {
                            Text(
                                when (mode) {
                                    PlayMode.VS_ENGINE -> "vs AI"
                                    PlayMode.VS_HUMAN -> "vs Human"
                                }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerColorSelector(
    currentColor: PlayerColor,
    onColorChange: (PlayerColor) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Play as",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentColor == PlayerColor.WHITE,
                    onClick = { onColorChange(PlayerColor.WHITE) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("White")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = currentColor == PlayerColor.BLACK,
                    onClick = { onColorChange(PlayerColor.BLACK) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Black")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun DifficultySettings(
    currentSettings: EngineSettings,
    onSettingsChange: (EngineSettings) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "AI Difficulty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            currentSettings.difficulty.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    EngineDifficulty.values().filter { it != EngineDifficulty.CUSTOM }.forEach { difficulty ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSettingsChange(currentSettings.copy(difficulty = difficulty))
                                    expanded = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSettings.difficulty == difficulty,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = difficulty.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (currentSettings.difficulty == difficulty) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    difficulty.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
