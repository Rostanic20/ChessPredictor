package com.chesspredictor.android.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.chesspredictor.android.data.repositories.GameStateRepositoryImpl
import com.chesspredictor.presentation.viewmodels.ChessBoardViewModel

class AndroidChessBoardViewModel(
    context: Context
) : ViewModel() {
    private val gameStateRepository = GameStateRepositoryImpl(context.applicationContext)

    val chessBoardViewModel = ChessBoardViewModel(
        providedAppModule = com.chesspredictor.di.getAppModule(),
        gameStateRepository = gameStateRepository
    )

    init {
        chessBoardViewModel.initializeBoard()
    }

    override fun onCleared() {
        super.onCleared()
        chessBoardViewModel.close()
    }
}
