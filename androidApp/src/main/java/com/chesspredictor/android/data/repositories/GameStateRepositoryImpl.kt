package com.chesspredictor.android.data.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.chesspredictor.data.repositories.GameStateRepository
import com.chesspredictor.data.repositories.SavedGameCodec
import com.chesspredictor.data.repositories.SavedGameState
import com.chesspredictor.domain.entities.EngineSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException

val Context.gameStateDataStore: DataStore<Preferences> by preferencesDataStore(name = "game_state")

class GameStateRepositoryImpl(
    private val context: Context
) : GameStateRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        private val FEN_KEY = stringPreferencesKey("fen")
        private val START_FEN_KEY = stringPreferencesKey("start_fen")
        private val MOVE_HISTORY_KEY = stringPreferencesKey("move_history")
        private val CAPTURED_PIECES_KEY = stringPreferencesKey("captured_pieces")
        private val PLAY_MODE_KEY = stringPreferencesKey("play_mode")
        private val PLAYER_COLOR_KEY = stringPreferencesKey("player_color")
        private val ENGINE_SETTINGS_KEY = stringPreferencesKey("engine_settings")
        private val IS_FLIPPED_KEY = booleanPreferencesKey("is_flipped")
        private val SHOW_COORDINATES_KEY = booleanPreferencesKey("show_coordinates")
        private val TIMESTAMP_KEY = longPreferencesKey("timestamp")
    }

    override suspend fun saveGameState(gameState: SavedGameState) {
        context.gameStateDataStore.edit { preferences ->
            preferences[FEN_KEY] = gameState.fen
            val startFen = gameState.startFen
            if (startFen != null) preferences[START_FEN_KEY] = startFen else preferences.remove(START_FEN_KEY)
            preferences[MOVE_HISTORY_KEY] = SavedGameCodec.encodeMoves(gameState.moveHistory)
            preferences[CAPTURED_PIECES_KEY] = SavedGameCodec.encodePieces(gameState.capturedPieces)
            preferences[PLAY_MODE_KEY] = gameState.playMode
            preferences[PLAYER_COLOR_KEY] = gameState.playerColor
            preferences[ENGINE_SETTINGS_KEY] = json.encodeToString(gameState.engineSettings)
            preferences[IS_FLIPPED_KEY] = gameState.isFlipped
            preferences[SHOW_COORDINATES_KEY] = gameState.showCoordinates
            preferences[TIMESTAMP_KEY] = gameState.timestamp
        }
    }

    override suspend fun loadGameState(): SavedGameState? = observeGameState().first()

    override suspend fun clearGameState() {
        context.gameStateDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    override fun observeGameState(): Flow<SavedGameState?> {
        return context.gameStateDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map(::toSavedGameState)
    }

    private fun toSavedGameState(preferences: Preferences): SavedGameState? {
        val fen = preferences[FEN_KEY] ?: return null
        return SavedGameState(
            fen = fen,
            startFen = preferences[START_FEN_KEY],
            moveHistory = SavedGameCodec.decodeMoves(preferences[MOVE_HISTORY_KEY] ?: ""),
            capturedPieces = SavedGameCodec.decodePieces(preferences[CAPTURED_PIECES_KEY] ?: ""),
            playMode = preferences[PLAY_MODE_KEY] ?: "VS_ENGINE",
            playerColor = preferences[PLAYER_COLOR_KEY] ?: "WHITE",
            engineSettings = preferences[ENGINE_SETTINGS_KEY]?.let {
                json.decodeFromString<EngineSettings>(it)
            } ?: EngineSettings(),
            isFlipped = preferences[IS_FLIPPED_KEY] ?: false,
            showCoordinates = preferences[SHOW_COORDINATES_KEY] ?: true,
            timestamp = preferences[TIMESTAMP_KEY] ?: 0L
        )
    }
}
