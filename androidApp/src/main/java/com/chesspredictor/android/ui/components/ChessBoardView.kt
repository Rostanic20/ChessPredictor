package com.chesspredictor.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.Square
import com.chesspredictor.presentation.managers.AnimationType
import com.chesspredictor.presentation.managers.PieceAnimation
import com.chesspredictor.presentation.viewmodels.ChessBoardUiState
import kotlin.math.roundToInt

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
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                FileLabels(uiState.isFlipped)

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RankLabels(uiState.isFlipped)

                    ChessBoardGrid(
                        uiState = uiState,
                        onSquareClick = onSquareClick,
                        modifier = Modifier.weight(1f)
                    )

                    RankLabels(uiState.isFlipped)
                }

                Spacer(modifier = Modifier.height(2.dp))

                FileLabels(uiState.isFlipped)
            }
        } else {
            ChessBoardGrid(
                uiState = uiState,
                onSquareClick = onSquareClick,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun FileLabels(isFlipped: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp)
    ) {
        val files = if (isFlipped) ('h' downTo 'a') else ('a'..'h')
        for (file in files) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CoordinateLabel(file.toString())
            }
        }
    }
}

@Composable
private fun RankLabels(isFlipped: Boolean) {
    Column(
        modifier = Modifier.width(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        val ranks = if (isFlipped) 1..8 else (8 downTo 1)
        for (rank in ranks) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CoordinateLabel(rank.toString())
            }
        }
    }
}

@Composable
private fun CoordinateLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
    )
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
        Column(
            modifier = Modifier
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .onGloballyPositioned { coordinates ->
                    boardSize.value = coordinates.size
                }
        ) {
            val ranks = if (uiState.isFlipped) 1..8 else (8 downTo 1)
            ranks.forEachIndexed { _, rank ->
                Row(modifier = Modifier.weight(1f)) {
                    val files = if (uiState.isFlipped) ('h' downTo 'a') else ('a'..'h')
                    files.forEachIndexed { _, file ->
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
            isCheck -> Color(0xFFFF6B6B)
            isSelected -> Color(0xFF7FA650)
            isLastMoveFrom || isLastMoveTo -> {
                if (isLightSquare) Color(0xFFF7EC83) else Color(0xFFD9CA61)
            }
            isLightSquare -> Color(0xFFF0D9B5)
            else -> Color(0xFFB58863)
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
            AnimationType.EN_PASSANT_CAPTURE -> 0.8f
            AnimationType.PROMOTION -> 1.2f
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
        val squareSizeDp = with(LocalDensity.current) { squareSize.toDp() }

        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(squareSizeDp)
                    .offset {
                        IntOffset(
                            x = (animatedOffset.x - squareSize / 2f).roundToInt(),
                            y = (animatedOffset.y - squareSize / 2f).roundToInt()
                        )
                    }
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

fun getPieceUnicode(piece: ChessPiece): String = when (piece) {
    is ChessPiece.Pawn -> if (piece.color == ChessColor.WHITE) "♙" else "♟"
    is ChessPiece.Knight -> if (piece.color == ChessColor.WHITE) "♘" else "♞"
    is ChessPiece.Bishop -> if (piece.color == ChessColor.WHITE) "♗" else "♝"
    is ChessPiece.Rook -> if (piece.color == ChessColor.WHITE) "♖" else "♜"
    is ChessPiece.Queen -> if (piece.color == ChessColor.WHITE) "♕" else "♛"
    is ChessPiece.King -> if (piece.color == ChessColor.WHITE) "♔" else "♚"
}
