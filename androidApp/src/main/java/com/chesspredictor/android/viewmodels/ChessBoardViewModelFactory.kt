package com.chesspredictor.android.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chesspredictor.android.data.repositories.GameStateRepositoryImpl
import com.chesspredictor.presentation.viewmodels.ChessBoardViewModel

class ChessBoardViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChessBoardViewModel::class.java)) {
            val gameStateRepository = GameStateRepositoryImpl(context.applicationContext)
            return ChessBoardViewModel(
                gameStateRepository = gameStateRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Wrapper to make ChessBoardViewModel compatible with Android's ViewModel
class AndroidChessBoardViewModel(
    context: Context
) : ViewModel() {
    private val gameStateRepository = GameStateRepositoryImpl(context.applicationContext)
    
    val chessBoardViewModel = ChessBoardViewModel(
        providedAppModule = com.chesspredictor.di.getAppModule(),
        gameStateRepository = gameStateRepository
    )
    
    init {
        // Initialize board and load any saved state
        chessBoardViewModel.initializeBoard()
    }
    
    override fun onCleared() {
        super.onCleared()
        // Save state when ViewModel is cleared
        chessBoardViewModel.saveGameState()
    }
}