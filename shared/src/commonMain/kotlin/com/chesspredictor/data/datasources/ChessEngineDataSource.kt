package com.chesspredictor.data.datasources

import com.chesspredictor.domain.entities.ChessBoard
import com.chesspredictor.domain.entities.ChessMove
import com.chesspredictor.domain.entities.EngineSettings
import kotlinx.coroutines.flow.Flow

interface ChessEngineDataSource {
    suspend fun sendCommand(command: String): String
    suspend fun initialize()
    suspend fun setPosition(fen: String)
    suspend fun analyze(settings: EngineSettings): EngineAnalysis
    suspend fun stop()
    fun getOutput(): Flow<String>
}

data class EngineAnalysis(
    val bestMove: String,
    val evaluation: Float,
    val depth: Int,
    val nodes: Long,
    val time: Long,
    val principalVariation: List<String> = emptyList(),
    val mate: Int? = null,
    val alternativeMoves: List<String> = emptyList(),
    val nps: Long = 0,
    val hashfull: Int = 0
)