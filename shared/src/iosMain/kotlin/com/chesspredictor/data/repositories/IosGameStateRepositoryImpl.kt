package com.chesspredictor.data.repositories

import com.chesspredictor.domain.entities.*
import com.chesspredictor.utils.TimeProvider
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
        private const val KEY_MOVE_HISTORY = "chess_move_history"
        private const val KEY_CAPTURED_PIECES = "chess_captured_pieces"
        private const val KEY_PLAY_MODE = "chess_play_mode"
        private const val KEY_PLAYER_COLOR = "chess_player_color"
        private const val KEY_ENGINE_SETTINGS = "chess_engine_settings"
        private const val KEY_IS_FLIPPED = "chess_is_flipped"
        private const val KEY_SHOW_COORDINATES = "chess_show_coordinates"
        private const val KEY_TIMESTAMP = "chess_timestamp"
    }

    override suspend fun saveGameState(gameState: SavedGameState) {
        defaults.setObject(gameState.fen, KEY_FEN)
        defaults.setObject(serializeMoveHistory(gameState.moveHistory), KEY_MOVE_HISTORY)
        defaults.setObject(serializeCapturedPieces(gameState.capturedPieces), KEY_CAPTURED_PIECES)
        defaults.setObject(gameState.playMode, KEY_PLAY_MODE)
        defaults.setObject(gameState.playerColor, KEY_PLAYER_COLOR)
        defaults.setObject(json.encodeToString(gameState.engineSettings), KEY_ENGINE_SETTINGS)
        defaults.setBool(gameState.isFlipped, KEY_IS_FLIPPED)
        defaults.setBool(gameState.showCoordinates, KEY_SHOW_COORDINATES)
        defaults.setDouble(gameState.timestamp.toDouble(), KEY_TIMESTAMP)
        defaults.synchronize()
        _gameStateFlow.value = gameState
    }

    override suspend fun loadGameState(): SavedGameState? {
        val fen = defaults.stringForKey(KEY_FEN) ?: return null
        val state = SavedGameState(
            fen = fen,
            moveHistory = deserializeMoveHistory(defaults.stringForKey(KEY_MOVE_HISTORY) ?: ""),
            capturedPieces = deserializeCapturedPieces(defaults.stringForKey(KEY_CAPTURED_PIECES) ?: ""),
            playMode = defaults.stringForKey(KEY_PLAY_MODE) ?: "VS_ENGINE",
            playerColor = defaults.stringForKey(KEY_PLAYER_COLOR) ?: "WHITE",
            engineSettings = defaults.stringForKey(KEY_ENGINE_SETTINGS)?.let {
                try { json.decodeFromString<EngineSettings>(it) } catch (_: Exception) { EngineSettings() }
            } ?: EngineSettings(),
            isFlipped = defaults.boolForKey(KEY_IS_FLIPPED),
            showCoordinates = if (defaults.objectForKey(KEY_SHOW_COORDINATES) != null) defaults.boolForKey(KEY_SHOW_COORDINATES) else true,
            timestamp = defaults.doubleForKey(KEY_TIMESTAMP).toLong()
        )
        _gameStateFlow.value = state
        return state
    }

    override suspend fun clearGameState() {
        listOf(KEY_FEN, KEY_MOVE_HISTORY, KEY_CAPTURED_PIECES, KEY_PLAY_MODE,
            KEY_PLAYER_COLOR, KEY_ENGINE_SETTINGS, KEY_IS_FLIPPED,
            KEY_SHOW_COORDINATES, KEY_TIMESTAMP).forEach {
            defaults.removeObjectForKey(it)
        }
        defaults.synchronize()
        _gameStateFlow.value = null
    }

    override fun observeGameState(): Flow<SavedGameState?> = _gameStateFlow.asStateFlow()

    private fun serializeMoveHistory(moves: List<DetailedMove>): String {
        return moves.joinToString(";") { move ->
            "${move.move.from.file}${move.move.from.rank}|" +
            "${move.move.to.file}${move.move.to.rank}|" +
            move.san
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
                            piece = ChessPiece.Pawn(ChessColor.WHITE)
                        ),
                        moveNumber = 1,
                        isWhiteMove = true,
                        san = parts[2],
                        previousCastlingRights = CastlingRights(true, true, true, true),
                        previousEnPassantSquare = null
                    )
                } else null
            } catch (_: Exception) { null }
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
            "$typeChar${if (piece.color == ChessColor.WHITE) "W" else "B"}"
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
            } catch (_: Exception) { null }
        }
    }
}
