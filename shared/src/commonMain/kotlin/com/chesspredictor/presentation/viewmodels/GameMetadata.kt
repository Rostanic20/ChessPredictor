package com.chesspredictor.presentation.viewmodels

import com.chesspredictor.domain.entities.EngineDifficulty
import com.chesspredictor.domain.entities.GameEnding
import com.chesspredictor.domain.entities.OpeningInfo
import com.chesspredictor.domain.entities.TimeControl
import com.chesspredictor.domain.usecases.ExportAdditionalData
import com.chesspredictor.presentation.managers.GameStatus
import com.chesspredictor.presentation.managers.PlayMode
import com.chesspredictor.presentation.managers.PlayerColor
import com.chesspredictor.presentation.managers.SettingsManager
import kotlinx.datetime.Clock

internal fun SettingsManager.exportMetadata(
    opening: OpeningInfo?,
    timeControl: TimeControl?,
    status: GameStatus
): ExportAdditionalData = ExportAdditionalData(
    event = "ChessPredictor Game",
    site = "ChessPredictor App",
    date = Clock.System.now().toString().substring(0, 10).replace("-", "."),
    white = if (playerColor.value == PlayerColor.WHITE) "Player" else "Engine",
    black = if (playerColor.value == PlayerColor.BLACK) "Player" else "Engine",
    whiteAccuracy = null,
    blackAccuracy = null,
    evaluations = emptyList(),
    keyMoments = null,
    openingName = opening?.opening?.name,
    openingEco = opening?.opening?.eco,
    timeControl = timeControl?.let { "${it.initialTime / 1000}+${it.increment / 1000}" } ?: "-",
    playMode = playMode.value.name,
    playerColor = playerColor.value.name,
    engineDifficulty = engineSettings.value.difficulty.name,
    result = status.adjudicatedResult()
)

internal fun GameStatus.adjudicatedResult(): String? = when {
    ending == null -> null
    isDraw -> "1/2-1/2"
    ending == GameEnding.WHITE_OUT_OF_TIME -> "0-1"
    ending == GameEnding.BLACK_OUT_OF_TIME -> "1-0"
    else -> null
}

internal fun SettingsManager.applyImportedSettings(data: ExportAdditionalData) {
    PlayMode.entries.find { it.name == data.playMode }?.let(::setPlayMode)
    PlayerColor.entries.find { it.name == data.playerColor }?.let(::setPlayerColor)
    EngineDifficulty.entries.find { it.name == data.engineDifficulty }?.let { difficulty ->
        setEngineSettings(engineSettings.value.withDifficulty(difficulty))
    }
}
