package com.chesspredictor.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.ChessPiece

@Composable
fun PromotionDialog(
    color: ChessColor,
    onSelect: (ChessPiece) -> Unit,
    onDismiss: () -> Unit
) {
    val choices = listOf(
        "Queen" to ChessPiece.Queen(color),
        "Rook" to ChessPiece.Rook(color),
        "Bishop" to ChessPiece.Bishop(color),
        "Knight" to ChessPiece.Knight(color)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Promote to") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                choices.forEach { (name, piece) ->
                    OutlinedCard(
                        onClick = { onSelect(piece) },
                        modifier = Modifier
                            .size(64.dp)
                            .semantics { contentDescription = name }
                    ) {
                        ChessPieceView(piece = piece)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DrawOfferDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text("Draw offered") },
        text = { Text("Your opponent offers a draw.") },
        confirmButton = {
            TextButton(onClick = onAccept) { Text("Accept") }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text("Decline") }
        }
    )
}
