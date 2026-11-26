package com.chesspredictor.data.repositories

import com.chesspredictor.domain.entities.ChessColor
import com.chesspredictor.domain.entities.EngineSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

class IosGameStateRepositoryImpl : GameStateRepository {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val _gameStateFlow = MutableStateFlow<SavedGameState?>(null)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        private const val KEY_FEN = "chess_fen"
        private const val KEY_START_FEN = "chess_start_fen"
        private const val KEY_MOVE_HISTORY = "chess_move_history"
        private const val KEY_CAPTURED_PIECES = "chess_captured_pieces"
        private const val KEY_PLAY_MODE = "chess_play_mode"
        private const val KEY_PLAYER_COLOR = "chess_player_color"
        private const val KEY_ENGINE_SETTINGS = "chess_engine_settings"
        private const val KEY_IS_FLIPPED = "chess_is_flipped"
        private const val KEY_SHOW_COORDINATES = "chess_show_coordinates"
        private const val KEY_TIMESTAMP = "chess_timestamp"
        private const val KEY_TIME_CONTROL = "chess_time_control"
        private const val KEY_WHITE_TIME = "chess_white_time_ms"
        private const val KEY_BLACK_TIME = "chess_black_time_ms"
        private const val KEY_ENDING = "chess_ending"
    }

    override suspend fun saveGameState(gameState: SavedGameState) {
        defaults.setObject(gameState.fen, KEY_FEN)
        val startFen = gameState.startFen
        if (startFen != null) defaults.setObject(startFen, KEY_START_FEN) else defaults.removeObjectForKey(KEY_START_FEN)
        defaults.setObject(SavedGameCodec.encodeMoves(gameState.moveHistory), KEY_MOVE_HISTORY)
        defaults.setObject(SavedGameCodec.encodePieces(gameState.capturedPieces), KEY_CAPTURED_PIECES)
        defaults.setObject(gameState.playMode, KEY_PLAY_MODE)
        defaults.setObject(gameState.playerColor, KEY_PLAYER_COLOR)
        defaults.setObject(json.encodeToString(gameState.engineSettings), KEY_ENGINE_SETTINGS)
        defaults.setBool(gameState.isFlipped, KEY_IS_FLIPPED)
        defaults.setBool(gameState.showCoordinates, KEY_SHOW_COORDINATES)
        defaults.setDouble(gameState.timestamp.toDouble(), KEY_TIMESTAMP)
        val timeControl = gameState.timeControl
        if (timeControl != null) defaults.setObject(SavedGameCodec.encodeTimeControl(timeControl), KEY_TIME_CONTROL) else defaults.removeObjectForKey(KEY_TIME_CONTROL)
        val times = gameState.remainingTimes
        if (times != null) {
            defaults.setDouble(times.getValue(ChessColor.WHITE).toDouble(), KEY_WHITE_TIME)
            defaults.setDouble(times.getValue(ChessColor.BLACK).toDouble(), KEY_BLACK_TIME)
        } else {
            defaults.removeObjectForKey(KEY_WHITE_TIME)
            defaults.removeObjectForKey(KEY_BLACK_TIME)
        }
        val ending = gameState.ending
        if (ending != null) defaults.setObject(ending.name, KEY_ENDING) else defaults.removeObjectForKey(KEY_ENDING)
        defaults.synchronize()
        _gameStateFlow.value = gameState
    }

    override suspend fun loadGameState(): SavedGameState? {
        val fen = defaults.stringForKey(KEY_FEN) ?: return null
        val state = SavedGameState(
            fen = fen,
            startFen = defaults.stringForKey(KEY_START_FEN),
            moveHistory = SavedGameCodec.decodeMoves(defaults.stringForKey(KEY_MOVE_HISTORY) ?: ""),
            capturedPieces = SavedGameCodec.decodePieces(defaults.stringForKey(KEY_CAPTURED_PIECES) ?: ""),
            playMode = defaults.stringForKey(KEY_PLAY_MODE) ?: "VS_ENGINE",
            playerColor = defaults.stringForKey(KEY_PLAYER_COLOR) ?: "WHITE",
            engineSettings = defaults.stringForKey(KEY_ENGINE_SETTINGS)?.let {
                try { json.decodeFromString<EngineSettings>(it) } catch (_: Exception) { EngineSettings() }
            } ?: EngineSettings(),
            isFlipped = defaults.boolForKey(KEY_IS_FLIPPED),
            showCoordinates = if (defaults.objectForKey(KEY_SHOW_COORDINATES) != null) defaults.boolForKey(KEY_SHOW_COORDINATES) else true,
            timestamp = defaults.doubleForKey(KEY_TIMESTAMP).toLong(),
            timeControl = SavedGameCodec.decodeTimeControl(defaults.stringForKey(KEY_TIME_CONTROL)),
            remainingTimes = SavedGameCodec.remainingTimes(
                defaults.objectForKey(KEY_WHITE_TIME)?.let { defaults.doubleForKey(KEY_WHITE_TIME).toLong() },
                defaults.objectForKey(KEY_BLACK_TIME)?.let { defaults.doubleForKey(KEY_BLACK_TIME).toLong() }
            ),
            ending = SavedGameCodec.decodeEnding(defaults.stringForKey(KEY_ENDING))
        )
        _gameStateFlow.value = state
        return state
    }

    override suspend fun clearGameState() {
        listOf(KEY_FEN, KEY_START_FEN, KEY_MOVE_HISTORY, KEY_CAPTURED_PIECES, KEY_PLAY_MODE,
            KEY_PLAYER_COLOR, KEY_ENGINE_SETTINGS, KEY_IS_FLIPPED,
            KEY_SHOW_COORDINATES, KEY_TIMESTAMP, KEY_TIME_CONTROL, KEY_WHITE_TIME,
            KEY_BLACK_TIME, KEY_ENDING).forEach {
            defaults.removeObjectForKey(it)
        }
        defaults.synchronize()
        _gameStateFlow.value = null
    }

    override fun observeGameState(): Flow<SavedGameState?> = _gameStateFlow.asStateFlow()
}
