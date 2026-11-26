package com.chesspredictor.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import com.chesspredictor.android.ui.components.DrawOfferDialog
import com.chesspredictor.android.ui.components.ExportGameDialog
import com.chesspredictor.android.ui.components.ImportGameDialog
import com.chesspredictor.android.ui.components.NewGameConfirmationDialog
import com.chesspredictor.android.ui.components.PromotionDialog
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.presentation.viewmodels.ChessBoardViewModel

@Composable
fun ChessBoardScreen(
    viewModel: ChessBoardViewModel = remember { ChessBoardViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val isEngineReady by viewModel.isEngineReady.collectAsState()
    val currentOpening by viewModel.currentOpening.collectAsState()
    val errorState by viewModel.errorState.collectAsState()
    val engineError by viewModel.engineError.collectAsState()
    val shouldOfferDraw by viewModel.shouldOfferDraw.collectAsState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingPromotion by remember { mutableStateOf<Pair<Square, Square>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorState) {
        val error = errorState ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error.message)
        viewModel.dismissError()
    }

    LaunchedEffect(engineError) {
        val error = engineError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error.errorMessage())
        viewModel.dismissEngineError()
    }

    pendingPromotion?.let { (from, to) ->
        PromotionDialog(
            color = uiState.currentTurn,
            onSelect = { piece ->
                pendingPromotion = null
                viewModel.executePromotionMove(from, to, piece)
            },
            onDismiss = { pendingPromotion = null }
        )
    }

    if (shouldOfferDraw) {
        DrawOfferDialog(
            onAccept = viewModel::acceptDrawOffer,
            onDecline = viewModel::declineDrawOffer
        )
    }

    if (uiState.showNewGameDialog) {
        NewGameConfirmationDialog(
            onConfirm = viewModel::confirmNewGame,
            onDismiss = viewModel::cancelNewGame
        )
    }

    if (showImportDialog) {
        ImportGameDialog(
            viewModel = viewModel,
            onDismiss = { showImportDialog = false }
        )
    }

    if (showExportDialog) {
        ExportGameDialog(
            viewModel = viewModel,
            onDismiss = { showExportDialog = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChessAppBar(
                showCoordinates = uiState.showCoordinates,
                onToggleCoordinates = viewModel::toggleCoordinates,
                onFlipBoard = viewModel::toggleBoardFlipAndSave,
                onNewGame = viewModel::onNewGame,
                onUndo = viewModel::undoLastMove,
                onImport = { showImportDialog = true },
                onExport = { showExportDialog = true }
            )
        }
    ) { paddingValues ->
        ChessGameContent(
            paddingValues = paddingValues,
            uiState = uiState,
            isThinking = isThinking,
            isEngineReady = isEngineReady,
            currentOpening = currentOpening,
            isLandscape = isLandscape,
            onSquareClick = { square ->
                val from = uiState.selectedSquare
                if (from != null && viewModel.isPromotionMove(square)) {
                    pendingPromotion = from to square
                } else {
                    viewModel.onSquareClick(square)
                }
            },
            onPlayModeChange = viewModel::setPlayMode,
            onPlayerColorChange = viewModel::setPlayerColor,
            onEngineSettingsChange = viewModel::setEngineSettings
        )
    }
}
