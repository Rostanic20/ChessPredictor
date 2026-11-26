package com.chesspredictor.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chesspredictor.android.ui.components.CapturedPiecesDisplay
import com.chesspredictor.android.ui.components.GameClocks
import com.chesspredictor.android.ui.components.GameStatusCard
import com.chesspredictor.android.ui.components.ModernChessBoard
import com.chesspredictor.android.ui.components.MoveHistoryDisplay
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.entities.OpeningInfo
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.domain.entities.TimeControl
import com.chesspredictor.presentation.managers.PlayMode
import com.chesspredictor.presentation.managers.PlayerColor
import com.chesspredictor.presentation.viewmodels.ChessBoardUiState
import kotlinx.coroutines.flow.StateFlow

internal class GameActions(
    val onSquareClick: (Square) -> Unit,
    val onPlayModeChange: (PlayMode) -> Unit,
    val onPlayerColorChange: (PlayerColor) -> Unit,
    val onEngineSettingsChange: (EngineSettings) -> Unit,
    val onTimeControlChange: (TimeControl?) -> Unit
)

@Composable
internal fun ChessGameContent(
    paddingValues: PaddingValues,
    uiState: ChessBoardUiState,
    isThinking: Boolean,
    isEngineReady: Boolean,
    currentOpening: OpeningInfo?,
    remainingTimes: StateFlow<Map<ChessColor, Long>>,
    isLandscape: Boolean,
    actions: GameActions
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLandscape) {
            LandscapeLayout(uiState, isThinking, isEngineReady, currentOpening, remainingTimes, actions)
        } else {
            PortraitLayout(uiState, isThinking, isEngineReady, currentOpening, remainingTimes, actions)
        }
    }
}

@Composable
private fun LandscapeLayout(
    uiState: ChessBoardUiState,
    isThinking: Boolean,
    isEngineReady: Boolean,
    currentOpening: OpeningInfo?,
    remainingTimes: StateFlow<Map<ChessColor, Long>>,
    actions: GameActions
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ModernChessBoard(
                    uiState = uiState,
                    onSquareClick = actions.onSquareClick,
                    modifier = Modifier.aspectRatio(1f)
                )

                Spacer(modifier = Modifier.height(8.dp))
                CapturedPiecesRow(uiState)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GameStatusCard(
                uiState = uiState,
                isThinking = isThinking,
                isEngineReady = isEngineReady
            )

            ClocksIfEnabled(uiState, remainingTimes)

            if (uiState.moveHistory.isNotEmpty()) {
                MoveHistoryDisplay(
                    moveHistory = uiState.moveHistory,
                    openingInfo = currentOpening,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            GameSettingsPanel(
                uiState,
                actions.onPlayModeChange,
                actions.onPlayerColorChange,
                actions.onEngineSettingsChange,
                actions.onTimeControlChange
            )
        }
    }
}

@Composable
private fun PortraitLayout(
    uiState: ChessBoardUiState,
    isThinking: Boolean,
    isEngineReady: Boolean,
    currentOpening: OpeningInfo?,
    remainingTimes: StateFlow<Map<ChessColor, Long>>,
    actions: GameActions
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GameStatusCard(
            uiState = uiState,
            isThinking = isThinking,
            isEngineReady = isEngineReady
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClocksIfEnabled(uiState, remainingTimes)

                ModernChessBoard(
                    uiState = uiState,
                    onSquareClick = actions.onSquareClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                CapturedPiecesRow(uiState)
            }
        }

        if (uiState.moveHistory.isNotEmpty()) {
            MoveHistoryDisplay(
                moveHistory = uiState.moveHistory,
                openingInfo = currentOpening,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 150.dp)
            )
        }

        GameSettingsPanel(
            uiState,
            actions.onPlayModeChange,
            actions.onPlayerColorChange,
            actions.onEngineSettingsChange,
            actions.onTimeControlChange
        )
    }
}

@Composable
private fun ClocksIfEnabled(
    uiState: ChessBoardUiState,
    remainingTimes: StateFlow<Map<ChessColor, Long>>,
    modifier: Modifier = Modifier
) {
    if (uiState.timeControl == null) return
    GameClocks(
        remainingTimes = remainingTimes,
        activeColor = uiState.currentTurn.takeIf { !uiState.isGameOver && uiState.moveHistory.isNotEmpty() },
        modifier = modifier
    )
}

@Composable
private fun CapturedPiecesRow(uiState: ChessBoardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CapturedPiecesDisplay(
            capturedPieces = uiState.capturedPieces,
            color = ChessColor.WHITE,
            modifier = Modifier.weight(1f)
        )
        CapturedPiecesDisplay(
            capturedPieces = uiState.capturedPieces,
            color = ChessColor.BLACK,
            modifier = Modifier.weight(1f)
        )
    }
}
