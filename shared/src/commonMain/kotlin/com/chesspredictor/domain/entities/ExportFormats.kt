package com.chesspredictor.domain.entities

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

enum class ExportFormat {
    PGN,
    JSON,
    FEN
}

@Serializable
data class ExportedGame(
    val version: Int = 1,
    val exportDate: String,
    val gameData: GameData,
    val analysis: AnalysisData? = null,
    val settings: GameSettings
)

@Serializable
data class GameData(
    val event: String = "Chess Game",
    val site: String = "ChessPredictor App",
    val date: String,
    val round: String = "-",
    val white: String = "Player",
    val black: String = "Engine",
    val result: String = "*",
    val fen: String,
    val moves: List<ExportedMove>,
    val currentPly: Int,
    val timeControl: String? = null
)

@Serializable
data class ExportedMove(
    val san: String,
    val from: String,
    val to: String,
    val piece: String,
    val captured: String? = null,
    val promotion: String? = null,
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val timeSpent: Long? = null,
    val evaluation: Float? = null,
    val comment: String? = null
)

@Serializable
data class AnalysisData(
    val whiteAccuracy: Float,
    val blackAccuracy: Float,
    val evaluations: List<Float>,
    val keyMoments: List<ExportedKeyMoment>,
    val openingName: String? = null,
    val openingEco: String? = null
)

@Serializable
data class ExportedKeyMoment(
    val moveNumber: Int,
    val description: String,
    val type: String
)

@Serializable
data class GameSettings(
    val playMode: String,
    val playerColor: String,
    val engineDifficulty: String,
    val timeControl: String? = null
)
