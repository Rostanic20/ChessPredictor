package com.chesspredictor.android.ui

import androidx.compose.animation.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chesspredictor.android.ui.components.*
import com.chesspredictor.domain.entities.*
import com.chesspredictor.presentation.viewmodels.ChessBoardViewModel
import com.chesspredictor.presentation.managers.PlayMode
import com.chesspredictor.presentation.managers.PlayerColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessBoardScreen(
    viewModel: ChessBoardViewModel = remember { ChessBoardViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val isEngineReady by viewModel.isEngineReady.collectAsState()
    val currentOpening by viewModel.currentOpening.collectAsState()
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.initializeBoard()
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
            onSquareClick = viewModel::onSquareClick,
            onPlayModeChange = viewModel::setPlayMode,
            onPlayerColorChange = viewModel::setPlayerColor,
            onEngineSettingsChange = viewModel::setEngineSettings
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChessAppBar(
    showCoordinates: Boolean,
    onToggleCoordinates: () -> Unit,
    onFlipBoard: () -> Unit,
    onNewGame: () -> Unit,
    onUndo: () -> Unit,
    onImport: () -> Unit = {},
    onExport: () -> Unit = {}
) {
    TopAppBar(
        title = {},
        actions = {
            IconButton(onClick = onUndo) {
                Icon(
                    Icons.Default.Undo,
                    contentDescription = "Undo move"
                )
            }
            IconButton(onClick = onToggleCoordinates) {
                Icon(
                    imageVector = if (showCoordinates) Icons.Default.Check else Icons.Default.List,
                    contentDescription = "Toggle coordinates",
                    tint = if (showCoordinates) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onFlipBoard) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Flip board"
                )
            }
            IconButton(onClick = onNewGame) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "New game"
                )
            }
            
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { 
                            Row {
                                Icon(
                                    Icons.Default.Upload,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Import Game")
                            }
                        },
                        onClick = {
                            showMenu = false
                            onImport()
                        }
                    )
                    DropdownMenuItem(
                        text = { 
                            Row {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Export Game")
                            }
                        },
                        onClick = {
                            showMenu = false
                            onExport()
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun ChessGameContent(
    paddingValues: PaddingValues,
    uiState: com.chesspredictor.presentation.viewmodels.ChessBoardUiState,
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
    uiState: com.chesspredictor.presentation.viewmodels.ChessBoardUiState,
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
                
                DifficultySettings(
                    currentSettings = uiState.engineSettings,
                    onSettingsChange = onEngineSettingsChange
                )
            }
        }
    }
}

@Composable
private fun PortraitLayout(
    uiState: com.chesspredictor.presentation.viewmodels.ChessBoardUiState,
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
            
            DifficultySettings(
                currentSettings = uiState.engineSettings,
                onSettingsChange = onEngineSettingsChange
            )
        }
    }
}


@Composable
fun EngineEvaluationDisplay(
    evaluation: EngineEvaluation?,
    depth: Int = 0,
    nodes: Long = 0
) {
    if (evaluation == null) return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Engine Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                val displayText = when {
                    evaluation.mate != null -> {
                        val mate = evaluation.mate!!
                        if (mate > 0) "M$mate" else "M${-mate}"
                    }
                    else -> {
                        val score = evaluation.score
                        when {
                            score > 0 -> "+%.2f".format(score)
                            score < 0 -> "%.2f".format(score)
                            else -> "0.00"
                        }
                    }
                }
                
                val scoreColor = when {
                    evaluation.mate != null -> {
                        if (evaluation.mate!! > 0) Color(0xFF4CAF50)
                        else Color(0xFFF44336)
                    }
                    else -> {
                        when {
                            evaluation.score > 1.5 -> Color(0xFF4CAF50)
                            evaluation.score > 0.5 -> Color(0xFF8BC34A)
                            evaluation.score < -1.5 -> Color(0xFFF44336)
                            evaluation.score < -0.5 -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    }
                }
                
                Text(
                    displayText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                val whitePercentage = when {
                    evaluation.mate != null -> {
                        if (evaluation.mate!! > 0) 100f else 0f
                    }
                    else -> {
                        val normalized = (evaluation.score.toFloat() + 10f) / 20f
                        (normalized * 100).coerceIn(0f, 100f)
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(whitePercentage.toFloat() / 100f)
                        .fillMaxHeight()
                        .background(Color.White)
                )
            }
            
            if (depth > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Depth: $depth",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Nodes: ${formatNodes(nodes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                                    PlayMode.ANALYSIS -> "Analysis"
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
                                Text(
                                    "~${calculateElo(difficulty.skillLevel)} Elo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculateElo(skillLevel: Int): Int {
    return 800 + (skillLevel * 100)
}

private fun formatNodes(nodes: Long): String {
    return when {
        nodes >= 1_000_000 -> "${nodes / 1_000_000}M"
        nodes >= 1_000 -> "${nodes / 1_000}K"
        else -> "$nodes"
    }
}

@Composable
fun ChessBoardScreenWithAnalysis(
    viewModel: ChessBoardViewModel,
    onNavigateToAnalysis: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        ChessBoardScreen(viewModel = viewModel)
        
        if (uiState.moveHistory.isNotEmpty()) {
            FloatingActionButton(
                onClick = onNavigateToAnalysis,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "View Analysis",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}