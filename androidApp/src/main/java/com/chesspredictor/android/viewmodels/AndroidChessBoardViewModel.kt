package com.chesspredictor.android.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.chesspredictor.android.data.repositories.GameStateRepositoryImpl
import com.chesspredictor.domain.humanlike.LazyMaiaNetwork
import com.chesspredictor.presentation.viewmodels.ChessBoardViewModel

class AndroidChessBoardViewModel(
    context: Context
) : ViewModel() {
    private val appContext = context.applicationContext
    private val gameStateRepository = GameStateRepositoryImpl(appContext)

    val chessBoardViewModel = ChessBoardViewModel(
        providedAppModule = com.chesspredictor.di.getAppModule(),
        gameStateRepository = gameStateRepository,
        humanMoves = LazyMaiaNetwork { appContext.assets.open(MAIA_WEIGHTS).use { it.readBytes() } }
    )

    init {
        chessBoardViewModel.initializeBoard()
    }

    override fun onCleared() {
        super.onCleared()
        chessBoardViewModel.close()
    }

    private companion object {
        const val MAIA_WEIGHTS = "maia3-5m.f16.bin"
    }
}
