package com.chesspredictor.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessPiece
import com.chesspredictor.domain.entities.DetailedMove
import com.chesspredictor.domain.entities.OpeningInfo
import com.chesspredictor.presentation.viewmodels.ChessBoardUiState

@Composable
fun CapturedPiecesDisplay(
    capturedPieces: List<ChessPiece>,
    color: ChessColor,
    modifier: Modifier = Modifier
) {
    val pieces = capturedPieces
        .filter { it.color != color }
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
