package com.chesspredictor.android.data.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.chesspredictor.data.repositories.GameStateRepository
import com.chesspredictor.data.repositories.SavedGameState
import com.chesspredictor.domain.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException

// Extension property to get DataStore instance
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
            preferences[MOVE_HISTORY_KEY] = serializeMoveHistory(gameState.moveHistory)
            preferences[CAPTURED_PIECES_KEY] = serializeCapturedPieces(gameState.capturedPieces)
            preferences[PLAY_MODE_KEY] = gameState.playMode
            preferences[PLAYER_COLOR_KEY] = gameState.playerColor
            preferences[ENGINE_SETTINGS_KEY] = json.encodeToString(gameState.engineSettings)
            preferences[IS_FLIPPED_KEY] = gameState.isFlipped
            preferences[SHOW_COORDINATES_KEY] = gameState.showCoordinates
            preferences[TIMESTAMP_KEY] = gameState.timestamp
        }
    }
    
    override suspend fun loadGameState(): SavedGameState? {
        return context.gameStateDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val fen = preferences[FEN_KEY]
                if (fen != null) {
                    SavedGameState(
                        fen = fen,
                        moveHistory = deserializeMoveHistory(preferences[MOVE_HISTORY_KEY] ?: ""),
                        capturedPieces = deserializeCapturedPieces(preferences[CAPTURED_PIECES_KEY] ?: ""),
                        playMode = preferences[PLAY_MODE_KEY] ?: "VS_ENGINE",
                        playerColor = preferences[PLAYER_COLOR_KEY] ?: "WHITE",
                        engineSettings = preferences[ENGINE_SETTINGS_KEY]?.let {
                            json.decodeFromString<EngineSettings>(it)
                        } ?: EngineSettings(),
                        isFlipped = preferences[IS_FLIPPED_KEY] ?: false,
                        showCoordinates = preferences[SHOW_COORDINATES_KEY] ?: true,
                        timestamp = preferences[TIMESTAMP_KEY] ?: 0L
                    )
                } else {
                    null
                }
            }
            .first()
    }
    
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
            .map { preferences ->
                val fen = preferences[FEN_KEY]
                if (fen != null) {
                    SavedGameState(
                        fen = fen,
                        moveHistory = deserializeMoveHistory(preferences[MOVE_HISTORY_KEY] ?: ""),
                        capturedPieces = deserializeCapturedPieces(preferences[CAPTURED_PIECES_KEY] ?: ""),
                        playMode = preferences[PLAY_MODE_KEY] ?: "VS_ENGINE",
                        playerColor = preferences[PLAYER_COLOR_KEY] ?: "WHITE",
                        engineSettings = preferences[ENGINE_SETTINGS_KEY]?.let {
                            json.decodeFromString<EngineSettings>(it)
                        } ?: EngineSettings(),
                        isFlipped = preferences[IS_FLIPPED_KEY] ?: false,
                        showCoordinates = preferences[SHOW_COORDINATES_KEY] ?: true,
                        timestamp = preferences[TIMESTAMP_KEY] ?: 0L
                    )
                } else {
                    null
                }
            }
    }
    
    private fun serializeMoveHistory(moves: List<DetailedMove>): String {
        // Simple serialization format: from|to|san;from|to|san...
        return moves.joinToString(";") { move ->
            "${move.move.from.file}${move.move.from.rank}|" +
            "${move.move.to.file}${move.move.to.rank}|" +
            "${move.san}"
        }
    }
    
    private fun deserializeMoveHistory(serialized: String): List<DetailedMove> {
        if (serialized.isEmpty()) return emptyList()
        
        return serialized.split(";").mapNotNull { moveStr ->
            try {
                val parts = moveStr.split("|")
                if (parts.size >= 3) {
                    val from = Square(parts[0][0], parts[0][1].toString().toInt())
                    val to = Square(parts[1][0], parts[1][1].toString().toInt())
                    DetailedMove(
                        move = ChessMove(
                            from = from,
                            to = to,
                            piece = ChessPiece.Pawn(ChessColor.WHITE) // Will be corrected when game state is restored
                        ),
                        moveNumber = 1, // Default value, actual number will be determined from position
                        isWhiteMove = true, // Default value, will be corrected based on actual game
                        san = parts[2],
                        previousCastlingRights = CastlingRights(true, true, true, true),
                        previousEnPassantSquare = null
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private fun serializeCapturedPieces(pieces: List<ChessPiece>): String {
        return pieces.joinToString(",") { piece ->
            val typeChar = when (piece) {
                is ChessPiece.Pawn -> "P"
                is ChessPiece.Knight -> "N"
                is ChessPiece.Bishop -> "B"
                is ChessPiece.Rook -> "R"
                is ChessPiece.Queen -> "Q"
                is ChessPiece.King -> "K"
            }
            "${typeChar}${if (piece.color == ChessColor.WHITE) "W" else "B"}"
        }
    }
    
    private fun deserializeCapturedPieces(serialized: String): List<ChessPiece> {
        if (serialized.isEmpty()) return emptyList()
        
        return serialized.split(",").mapNotNull { pieceStr ->
            try {
                val type = pieceStr[0]
                val color = if (pieceStr[1] == 'W') ChessColor.WHITE else ChessColor.BLACK
                when (type) {
                    'P' -> ChessPiece.Pawn(color)
                    'N' -> ChessPiece.Knight(color)
                    'B' -> ChessPiece.Bishop(color)
                    'R' -> ChessPiece.Rook(color)
                    'Q' -> ChessPiece.Queen(color)
                    'K' -> ChessPiece.King(color)
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}