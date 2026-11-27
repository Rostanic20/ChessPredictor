package com.chesspredictor.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val SOURCE_URL = "https://github.com/Rostanic20/ChessPredictor"

private class Credit(val name: String, val licence: String, val detail: String)

private val credits = listOf(
    Credit("ChessPredictor", "GNU AGPL-3.0", "Free software: you may use, study, share and change it under the same licence."),
    Credit("Stockfish 17.1", "GNU GPL-3.0", "The Stockfish developers, stockfishchess.org, in the stockfish.js build."),
    Credit("Think times", "Lichess open database, CC0", "Human thinking times learned from public Lichess games.")
)

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About ChessPredictor") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                credits.forEach { credit ->
                    Column {
                        Text(credit.name, fontWeight = FontWeight.Bold)
                        Text(credit.licence, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text(credit.detail, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("Source code: $SOURCE_URL", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { runCatching { uriHandler.openUri(SOURCE_URL) } }) { Text("Source code") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
