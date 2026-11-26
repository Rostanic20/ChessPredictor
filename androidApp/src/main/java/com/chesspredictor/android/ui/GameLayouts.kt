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
import com.chesspredictor.android.ui.components.GameStatusCard
import com.chesspredictor.android.ui.components.ModernChessBoard
import com.chesspredictor.android.ui.components.MoveHistoryDisplay
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.entities.OpeningInfo
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.presentation.managers.PlayMode
import com.chesspredictor.presentation.managers.PlayerColor
import com.chesspredictor.presentation.viewmodels.ChessBoardUiState

@Composable
internal fun ChessGameContent(
    paddingValues: PaddingValues,
    uiState: ChessBoardUiState,
    isThinking: Boolean,
    isEngineReady: Boolean,
    currentOpening: OpeningInfo?,
    isLandscape: Boolean,
    onSquareClick: (Square) -> Unit,
    onPlayModeChange: (PlayMode) -> Unit,
    onPlayerColorChange: (PlayerColor) -> Unit,
    onEngineSettingsChange: (EngineSettings) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLandscape) {
            LandscapeLayout(
                uiState = uiState,
                isThinking = isThinking,
                isEngineReady = isEngineReady,
                currentOpening = currentOpening,
                onSquareClick = onSquareClick,
                onPlayModeChange = onPlayModeChange,
                onPlayerColorChange = onPlayerColorChange,
                onEngineSettingsChange = onEngineSettingsChange
            )
        } else {
            PortraitLayout(
                uiState = uiState,
                isThinking = isThinking,
                isEngineReady = isEngineReady,
                currentOpening = currentOpening,
                onSquareClick = onSquareClick,
                onPlayModeChange = onPlayModeChange,
                onPlayerColorChange = onPlayerColorChange,
                onEngineSettingsChange = onEngineSettingsChange
            )
        }
    }
}

@Composable
private fun LandscapeLayout(
    uiState: ChessBoardUiState,
    isThinking: Boolean,
    isEngineReady: Boolean,
    currentOpening: OpeningInfo?,
    onSquareClick: (Square) -> Unit,
    onPlayModeChange: (PlayMode) -> Unit,
    onPlayerColorChange: (PlayerColor) -> Unit,
    onEngineSettingsChange: (EngineSettings) -> Unit
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
                if (uiState.isCheckmate || uiState.isStalemate || uiState.isDraw) {
                    GameStatusCard(
                        uiState = uiState,
                        isThinking = isThinking,
                        isEngineReady = isEngineReady,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }

                ModernChessBoard(
                    uiState = uiState,
                    onSquareClick = onSquareClick,
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
            if (!uiState.isCheckmate && !uiState.isStalemate && !uiState.isDraw) {
                GameStatusCard(
                    uiState = uiState,
                    isThinking = isThinking,
                    isEngineReady = isEngineReady
                )
            }

            if (uiState.moveHistory.isNotEmpty()) {
                MoveHistoryDisplay(
                    moveHistory = uiState.moveHistory,
                    openingInfo = currentOpening,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            GameSettingsPanel(uiState, onPlayModeChange, onPlayerColorChange, onEngineSettingsChange)
        }
    }
}

@Composable
private fun PortraitLayout(
    uiState: ChessBoardUiState,
    isThinking: Boolean,
    isEngineReady: Boolean,
    currentOpening: OpeningInfo?,
    onSquareClick: (Square) -> Unit,
    onPlayModeChange: (PlayMode) -> Unit,
    onPlayerColorChange: (PlayerColor) -> Unit,
    onEngineSettingsChange: (EngineSettings) -> Unit
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
                ModernChessBoard(
                    uiState = uiState,
                    onSquareClick = onSquareClick,
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

        GameSettingsPanel(uiState, onPlayModeChange, onPlayerColorChange, onEngineSettingsChange)
    }
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
