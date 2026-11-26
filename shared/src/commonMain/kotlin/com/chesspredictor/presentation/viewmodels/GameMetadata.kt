package com.chesspredictor.presentation.viewmodels

import com.chesspredictor.domain.entities.EngineDifficulty
import com.chesspredictor.domain.entities.OpeningInfo
import com.chesspredictor.domain.usecases.ExportAdditionalData
import com.chesspredictor.presentation.managers.PlayMode
import com.chesspredictor.presentation.managers.PlayerColor
import com.chesspredictor.presentation.managers.SettingsManager
import kotlinx.datetime.Clock

internal fun SettingsManager.exportMetadata(opening: OpeningInfo?): ExportAdditionalData = ExportAdditionalData(
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
    timeControl = currentTimeControl.value.let { "${it.initialTime / 60000}+${it.increment / 1000}" },
    playMode = playMode.value.name,
    playerColor = playerColor.value.name,
    engineDifficulty = engineSettings.value.difficulty.name
)

internal fun SettingsManager.applyImportedSettings(data: ExportAdditionalData) {
    PlayMode.entries.find { it.name == data.playMode }?.let(::setPlayMode)
    PlayerColor.entries.find { it.name == data.playerColor }?.let(::setPlayerColor)
    EngineDifficulty.entries.find { it.name == data.engineDifficulty }?.let { difficulty ->
        setEngineSettings(engineSettings.value.withDifficulty(difficulty))
    }
}
