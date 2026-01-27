package com.chesspredictor.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chesspredictor.domain.entities.*
import com.chesspredictor.presentation.viewmodels.ChessBoardUiState
import com.chesspredictor.presentation.managers.PieceAnimation
import com.chesspredictor.presentation.managers.AnimationType

@Composable
fun ModernChessBoard(
    uiState: ChessBoardUiState,
    onSquareClick: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        if (uiState.showCoordinates) {
            // Board with coordinates - proper spacing
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                // Top file labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp)
                ) {
                    val files = if (uiState.isFlipped) ('h' downTo 'a') else ('a'..'h')
                    for (file in files) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = file.toString(),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Board with side labels
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left rank labels
                    Column(
                        modifier = Modifier.width(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val ranks = if (uiState.isFlipped) 1..8 else (8 downTo 1)
                        for (rank in ranks) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rank.toString(),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    // Chess board
                    ChessBoardGrid(
                        uiState = uiState,
                        onSquareClick = onSquareClick,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Right rank labels
                    Column(
                        modifier = Modifier.width(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val ranks = if (uiState.isFlipped) 1..8 else (8 downTo 1)
                        for (rank in ranks) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rank.toString(),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Bottom file labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp)
                ) {
                    val files = if (uiState.isFlipped) ('h' downTo 'a') else ('a'..'h')
                    for (file in files) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = file.toString(),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        } else {
            // Board without coordinates
            ChessBoardGrid(
                uiState = uiState,
                onSquareClick = onSquareClick,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun ChessBoardGrid(
    uiState: ChessBoardUiState,
    onSquareClick: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    val squarePositions = remember { mutableStateOf<Map<Square, Offset>>(emptyMap()) }
    val boardSize = remember { mutableStateOf(IntSize.Zero) }
    
    LaunchedEffect(boardSize.value, uiState.isFlipped, uiState.showCoordinates) {
        if (boardSize.value != IntSize.Zero) {
            val squareSize = boardSize.value.width / 8f
            val positions = mutableMapOf<Square, Offset>()
            
            for (rank in 1..8) {
                for (fileChar in 'a'..'h') {
                    val square = Square(fileChar, rank)
                    
                    val fileIndex = if (uiState.isFlipped) (7 - (fileChar - 'a')) else (fileChar - 'a')
                    val rankIndex = if (uiState.isFlipped) (rank - 1) else (8 - rank)
                    
                    val centerOffset = Offset(
                        x = fileIndex.toFloat() * squareSize + squareSize / 2f,
                        y = rankIndex.toFloat() * squareSize + squareSize / 2f
                    )
                    positions[square] = centerOffset
                }
            }
            squarePositions.value = positions
        } else {
            squarePositions.value = emptyMap()
        }
    }
    
    Box(modifier = modifier) {
        // Board grid with squares
        Column(
            modifier = Modifier
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .onGloballyPositioned { coordinates ->
                    boardSize.value = coordinates.size
                }
        ) {
            val ranks = if (uiState.isFlipped) 1..8 else (8 downTo 1)
            ranks.forEachIndexed { rankIndex, rank ->
                Row(modifier = Modifier.weight(1f)) {
                    val files = if (uiState.isFlipped) ('h' downTo 'a') else ('a'..'h')
                    files.forEachIndexed { fileIndex, file ->
                        val square = Square(file, rank)
                        val isAnimatingFrom = square in uiState.animatingPieces
                        val piece = if (isAnimatingFrom) null else uiState.boardState[square]
                        
                        ChessSquare(
                            square = square,
                            piece = piece,
                            isSelected = uiState.selectedSquare == square,
                            isPossibleMove = square in uiState.possibleMoves,
                            isLastMoveFrom = uiState.lastMove?.from == square,
                            isLastMoveTo = uiState.lastMove?.to == square,
                            isCheck = uiState.isCheck && 
                                uiState.boardState[square] is ChessPiece.King && 
                                uiState.boardState[square]?.color == uiState.currentTurn,
                            onClick = { onSquareClick(square) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        if (squarePositions.value.isNotEmpty() && boardSize.value != IntSize.Zero) {
            AnimatedPiecesOverlay(
                animations = uiState.animations,
                squarePositions = squarePositions.value,
                boardSize = boardSize.value,
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(10f)
            )
        }
    }
}

@Composable
fun ChessSquare(
    square: Square,
    piece: ChessPiece?,
    isSelected: Boolean,
    isPossibleMove: Boolean,
    isLastMoveFrom: Boolean,
    isLastMoveTo: Boolean,
    isCheck: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightSquare = ((square.file - 'a') + (square.rank - 1)) % 2 == 0
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCheck -> Color(0xFFFF6B6B) // Red for check
            isSelected -> Color(0xFF7FA650) // Green for selected
            isLastMoveFrom || isLastMoveTo -> {
                if (isLightSquare) Color(0xFFF7EC83) else Color(0xFFD9CA61) // Yellow for last move
            }
            isLightSquare -> Color(0xFFF0D9B5) // Light squares
            else -> Color(0xFFB58863) // Dark squares
        },
        animationSpec = tween(200),
        label = "square color"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable { onClick() }
            .testTag("chess_square_$square"),
        contentAlignment = Alignment.Center
    ) {
        // Possible move indicator
        if (isPossibleMove) {
            Box(
                modifier = Modifier
                    .size(if (piece != null) 42.dp else 16.dp)
                    .clip(CircleShape)
                    .background(
                        if (piece != null) Color.Black.copy(alpha = 0.2f)
                        else Color.Black.copy(alpha = 0.3f)
                    )
            )
        }
        
        // Chess piece
        piece?.let {
            ChessPieceView(piece = it)
        }
    }
}

@Composable
fun ChessPieceView(piece: ChessPiece, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = getPieceUnicode(piece),
            fontSize = 32.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CapturedPiecesDisplay(
    capturedPieces: List<ChessPiece>,
    color: ChessColor,
    modifier: Modifier = Modifier
) {
    // Show pieces of the OPPOSITE color (these are the pieces this color captured)
    val pieces = capturedPieces
        .filter { it.color != color }  // Changed from == to !=
        .groupBy { piece ->
            when (piece) {
                is ChessPiece.Queen -> 5
                is ChessPiece.Rook -> 4
                is ChessPiece.Bishop -> 3
                is ChessPiece.Knight -> 2
                is ChessPiece.Pawn -> 1
                else -> 0
            }
        }
        .toSortedMap(reverseOrder())
        .flatMap { it.value }
    
    val materialValue = pieces.sumOf { piece ->
        when (piece) {
            is ChessPiece.Queen -> 9
            is ChessPiece.Rook -> 5
            is ChessPiece.Bishop -> 3
            is ChessPiece.Knight -> 3
            is ChessPiece.Pawn -> 1
            else -> 0
        }.toInt()
    }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (color == ChessColor.WHITE) "White" else "Black"} captured:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (materialValue > 0) {
                    Text(
                        text = "+$materialValue",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (pieces.isEmpty()) {
                    Text(
                        text = "None",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                } else {
                    pieces.forEach { piece ->
                        Text(
                            text = getPieceUnicode(piece),
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MoveHistoryDisplay(
    moveHistory: List<DetailedMove>,
    openingInfo: OpeningInfo? = null,
    gameAnalysis: GameAnalysis? = null,
    showAnalysis: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Move History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                openingInfo?.opening?.let { opening ->
                    Text(
                        text = opening.eco,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Opening name if detected
            openingInfo?.opening?.let { opening ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = opening.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                openingInfo.variation?.let { variation ->
                    Text(
                        text = variation,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (moveHistory.isEmpty()) {
                Text(
                    text = "No moves yet",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(moveHistory.chunked(2)) { index, moves ->
                        if (showAnalysis) {
                            // Enhanced analysis view
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        modifier = Modifier.width(28.dp),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    moves.getOrNull(0)?.let { move ->
                                        val moveIndex = index * 2
                                        val analysis = gameAnalysis?.moves?.getOrNull(moveIndex)
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = move.san.ifEmpty { 
                                                        "${move.move.from}-${move.move.to}" 
                                                    },
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Normal
                                                )
                                                
                                                analysis?.let { moveAnalysis ->
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = getMoveQualitySymbol(moveAnalysis.quality),
                                                        fontSize = 11.sp,
                                                        color = getMoveQualityColor(moveAnalysis.quality)
                                                    )
                                                }
                                            }
                                            
                                            analysis?.comment?.takeIf { it.isNotEmpty() }?.let { comment ->
                                                Text(
                                                    text = comment,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    moves.getOrNull(1)?.let { move ->
                                        val moveIndex = index * 2 + 1
                                        val analysis = gameAnalysis?.moves?.getOrNull(moveIndex)
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = move.san.ifEmpty { 
                                                        "${move.move.from}-${move.move.to}" 
                                                    },
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Normal
                                                )
                                                
                                                analysis?.let { moveAnalysis ->
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = getMoveQualitySymbol(moveAnalysis.quality),
                                                        fontSize = 11.sp,
                                                        color = getMoveQualityColor(moveAnalysis.quality)
                                                    )
                                                }
                                            }
                                            
                                            analysis?.comment?.takeIf { it.isNotEmpty() }?.let { comment ->
                                                Text(
                                                    text = comment,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Simple view for game screen
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    modifier = Modifier.width(28.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                moves.getOrNull(0)?.let { move ->
                                    Text(
                                        text = move.san.ifEmpty { 
                                            "${move.move.from}-${move.move.to}" 
                                        },
                                        modifier = Modifier.width(70.dp),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                
                                moves.getOrNull(1)?.let { move ->
                                    Text(
                                        text = move.san.ifEmpty { 
                                            "${move.move.from}-${move.move.to}" 
                                        },
                                        modifier = Modifier.width(70.dp),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun NewGameConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Start New Game?",
                fontWeight = FontWeight.Bold
            )
        },
        text = { 
            Text("Your current game progress will be lost. Are you sure you want to start a new game?")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("New Game")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun GameStatusCard(
    uiState: ChessBoardUiState,
    isThinking: Boolean,
    isEngineReady: Boolean,
    modifier: Modifier = Modifier
) {
    if (uiState.isCheckmate || uiState.isStalemate || uiState.isDraw) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    uiState.isCheckmate -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = when {
                    uiState.isCheckmate -> "CHECKMATE! ${if (uiState.currentTurn == ChessColor.WHITE) "Black" else "White"} wins!"
                    uiState.isStalemate -> "STALEMATE! Game is a draw."
                    else -> "DRAW! Game ended in a draw."
                },
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.isCheck) 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Turn: ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (uiState.currentTurn == ChessColor.WHITE) Color.White 
                                else Color.Black
                            )
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    )
                    if (uiState.isCheck) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "CHECK!",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                if (isThinking) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Thinking...", 
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else if (!isEngineReady) {
                    Text(
                        "Engine loading...", 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedPiecesOverlay(
    animations: List<PieceAnimation>,
    squarePositions: Map<Square, Offset>,
    boardSize: IntSize,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        animations.forEach { animation ->
            key(animation.animationId) {
                AnimatedChessPiece(
                    animation = animation,
                    squarePositions = squarePositions,
                    boardSize = boardSize,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun AnimatedChessPiece(
    animation: PieceAnimation,
    squarePositions: Map<Square, Offset>,
    boardSize: IntSize,
    modifier: Modifier = Modifier
) {
    val fromPosition = squarePositions[animation.fromSquare]
    val toPosition = squarePositions[animation.toSquare]
    
    if (fromPosition == null || toPosition == null || 
        fromPosition == Offset.Zero || toPosition == Offset.Zero) return
    
    var targetPosition by remember(animation.animationId) { mutableStateOf(fromPosition) }
    
    val animatedOffset by animateOffsetAsState(
        targetValue = targetPosition,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "piece_position"
    )
    
    LaunchedEffect(animation.animationId) {
        targetPosition = toPosition
    }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = when (animation.animationType) {
            AnimationType.PIECE_FADE_OUT,
            AnimationType.EN_PASSANT_CAPTURE -> 0f
            else -> 1f
        },
        animationSpec = tween(300),
        label = "piece_alpha"
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = when (animation.animationType) {
            AnimationType.PIECE_FADE_OUT,
            AnimationType.EN_PASSANT_CAPTURE -> 0.8f // Slightly shrink captured pieces
            AnimationType.PROMOTION -> 1.2f // Slightly grow promoted pieces
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "piece_scale"
    )
    
    if (boardSize != IntSize.Zero) {
        val squareSize = boardSize.width / 8f
        
        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(squareSize.dp)
                    .offset(
                        x = (animatedOffset.x - squareSize / 2f).dp,
                        y = (animatedOffset.y - squareSize / 2f).dp
                    )
                    .scale(animatedScale)
                    .alpha(animatedAlpha),
                contentAlignment = Alignment.Center
            ) {
                ChessPieceView(
                    piece = animation.piece,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


@Composable
fun OpeningInfoCard(
    openingInfo: OpeningInfo?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Opening",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                openingInfo?.opening?.let { opening ->
                    Text(
                        text = opening.eco,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            if (openingInfo?.opening != null) {
                openingInfo.opening?.let { opening ->
                    Text(
                        text = opening.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    openingInfo.variation?.let { variation ->
                        Text(
                            text = variation,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    
                    Text(
                        text = "Moves matched: ${openingInfo.moveNumber}/${opening.moves.size}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                Text(
                    text = "No opening detected yet",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun LiveAnalysisCard(
    gameAnalysis: GameAnalysis?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Analysis",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (gameAnalysis != null) {
                    Text(
                        text = gameAnalysis.gamePhase.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (gameAnalysis != null) {
                // Last Move Analysis - Most Important Info
                val lastMove = gameAnalysis.moves.lastOrNull()
                if (lastMove != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Last move: ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = getMoveQualitySymbol(lastMove.quality),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = lastMove.quality.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 12.sp,
                                color = getMoveQualityColor(lastMove.quality),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Compact accuracy for current turn
                        val moveCount = gameAnalysis.moves.size
                        val isWhiteTurn = moveCount % 2 == 0
                        val currentPlayerAccuracy = if (isWhiteTurn) gameAnalysis.accuracy.white else gameAnalysis.accuracy.black
                        val playerName = if (isWhiteTurn) "White" else "Black"
                        
                        if (currentPlayerAccuracy > 0) {
                            Text(
                                text = "$playerName: ${currentPlayerAccuracy.toInt()}%",
                                fontSize = 12.sp,
                                color = getAccuracyColor(currentPlayerAccuracy),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // No moves yet
                    Text(
                        text = "Game starting...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Play moves to see live analysis",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun MoveAnalysisCard(
    gameAnalysis: GameAnalysis?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Game Analysis",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (gameAnalysis != null) {
                    Text(
                        text = gameAnalysis.gamePhase.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (gameAnalysis != null) {
                // Accuracy Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "White Accuracy",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (gameAnalysis.accuracy.white > 0) "${gameAnalysis.accuracy.white.toInt()}%" else "—",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (gameAnalysis.accuracy.white > 0) getAccuracyColor(gameAnalysis.accuracy.white) 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column {
                        Text(
                            text = "Black Accuracy", 
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (gameAnalysis.accuracy.black > 0) "${gameAnalysis.accuracy.black.toInt()}%" else "—",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (gameAnalysis.accuracy.black > 0) getAccuracyColor(gameAnalysis.accuracy.black)
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Key Moments
                if (gameAnalysis.keyMoments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Key Moments",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    gameAnalysis.keyMoments.take(3).forEach { moment ->
                        Text(
                            text = "Move ${moment.moveNumber}: ${moment.description}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Recent Move Analysis
                val lastMove = gameAnalysis.moves.lastOrNull()
                if (lastMove != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Last move: ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = getMoveQualitySymbol(lastMove.quality),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = lastMove.quality.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 11.sp,
                            color = getMoveQualityColor(lastMove.quality)
                        )
                    }
                    if (lastMove.comment.isNotEmpty()) {
                        Text(
                            text = lastMove.comment,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
                
            } else {
                Text(
                    text = "Play at least 6 moves (3 per player) to see analysis",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun getAccuracyColor(accuracy: Float): Color {
    return when {
        accuracy >= 90 -> Color(0xFF4CAF50) // Green
        accuracy >= 80 -> Color(0xFF8BC34A) // Light green
        accuracy >= 70 -> Color(0xFFFFEB3B) // Yellow
        accuracy >= 60 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
}

@Composable
private fun getMoveQualityColor(quality: MoveQuality): Color {
    return when (quality) {
        MoveQuality.BRILLIANT -> Color(0xFF00BCD4) // Cyan
        MoveQuality.GREAT -> Color(0xFF4CAF50) // Green
        MoveQuality.GOOD -> MaterialTheme.colorScheme.onSurfaceVariant
        MoveQuality.INACCURACY -> Color(0xFFFF9800) // Orange
        MoveQuality.MISTAKE -> Color(0xFFFF5722) // Deep orange
        MoveQuality.BLUNDER -> Color(0xFFF44336) // Red
    }
}

private fun getMoveQualitySymbol(quality: MoveQuality): String {
    return when (quality) {
        MoveQuality.BRILLIANT -> "♦"
        MoveQuality.GREAT -> "!"
        MoveQuality.GOOD -> ""
        MoveQuality.INACCURACY -> "?!"
        MoveQuality.MISTAKE -> "??"
        MoveQuality.BLUNDER -> "???"
    }
}

fun getPieceUnicode(piece: ChessPiece): String = when (piece) {
    is ChessPiece.Pawn -> if (piece.color == ChessColor.WHITE) "♙" else "♟"
    is ChessPiece.Knight -> if (piece.color == ChessColor.WHITE) "♘" else "♞"
    is ChessPiece.Bishop -> if (piece.color == ChessColor.WHITE) "♗" else "♝"
    is ChessPiece.Rook -> if (piece.color == ChessColor.WHITE) "♖" else "♜"
    is ChessPiece.Queen -> if (piece.color == ChessColor.WHITE) "♕" else "♛"
    is ChessPiece.King -> if (piece.color == ChessColor.WHITE) "♔" else "♚"
}