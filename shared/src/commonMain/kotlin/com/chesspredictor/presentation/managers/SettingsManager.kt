package com.chesspredictor.presentation.managers

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.EngineSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager {
    private val _isFlipped = MutableStateFlow(false)
    val isFlipped: StateFlow<Boolean> = _isFlipped.asStateFlow()

    private val _showCoordinates = MutableStateFlow(true)
    val showCoordinates: StateFlow<Boolean> = _showCoordinates.asStateFlow()

    private val _showNewGameDialog = MutableStateFlow(false)
    val showNewGameDialog: StateFlow<Boolean> = _showNewGameDialog.asStateFlow()

    private val _playMode = MutableStateFlow(PlayMode.VS_ENGINE)
    val playMode: StateFlow<PlayMode> = _playMode.asStateFlow()

    private val _playerColor = MutableStateFlow(PlayerColor.WHITE)
    val playerColor: StateFlow<PlayerColor> = _playerColor.asStateFlow()

    private val _engineSettings = MutableStateFlow(EngineSettings())
    val engineSettings: StateFlow<EngineSettings> = _engineSettings.asStateFlow()

    fun toggleFlip() {
        _isFlipped.value = !_isFlipped.value
    }

    fun setFlipped(flipped: Boolean) {
        _isFlipped.value = flipped
    }

    fun toggleCoordinates() {
        _showCoordinates.value = !_showCoordinates.value
    }

    fun setShowCoordinates(show: Boolean) {
        _showCoordinates.value = show
    }

    fun openNewGameDialog() {
        _showNewGameDialog.value = true
    }

    fun hideNewGameDialog() {
        _showNewGameDialog.value = false
    }

    fun setPlayMode(mode: PlayMode) {
        _playMode.value = mode
    }

    fun setPlayerColor(color: PlayerColor) {
        _playerColor.value = color
    }

    fun setEngineSettings(settings: EngineSettings) {
        _engineSettings.value = settings
    }

    fun shouldBotMove(currentTurn: ChessColor): Boolean {
        if (_playMode.value != PlayMode.VS_ENGINE) return false
        val botColor = if (_playerColor.value == PlayerColor.WHITE) ChessColor.BLACK else ChessColor.WHITE
        return currentTurn == botColor
    }
}

enum class PlayMode {
    VS_ENGINE,
    VS_HUMAN
}

enum class PlayerColor {
    WHITE,
    BLACK
}
