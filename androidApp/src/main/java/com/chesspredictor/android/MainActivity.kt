package com.chesspredictor.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chesspredictor.android.viewmodels.AndroidChessBoardViewModel
import com.chesspredictor.di.initializeAndroid
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        initializeAndroid(this)
        
        setContent {
            MaterialTheme {
                Surface {
                    ChessApp()
                }
            }
        }
    }
}

@Composable
fun ChessApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    val androidViewModel: AndroidChessBoardViewModel = viewModel {
        AndroidChessBoardViewModel(context)
    }
    val viewModel = androidViewModel.chessBoardViewModel

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    coroutineScope.launch {
                        viewModel.saveGameState()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    val uiState by viewModel.uiState.collectAsState()
    val hasOngoingGame = uiState.moveHistory.isNotEmpty()
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (hasOngoingGame) {
            showExitDialog = true
        } else {
            (context as? ComponentActivity)?.finish()
        }
    }
    
    com.chesspredictor.android.ui.ChessBoardScreen(
        viewModel = viewModel
    )

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Game?") },
            text = { Text("You have an ongoing game. Your progress will be saved automatically.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        coroutineScope.launch {
                            viewModel.saveGameState()
                            (context as? ComponentActivity)?.finish()
                        }
                    }
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}