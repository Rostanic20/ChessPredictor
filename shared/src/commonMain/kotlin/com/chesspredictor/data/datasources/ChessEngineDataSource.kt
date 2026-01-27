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
    val bestMove: String, // UCI format e.g. "e2e4"
    val evaluation: Float,
    val depth: Int,
    val nodes: Long,
    val time: Long,
    val principalVariation: List<String> = emptyList(),
    val mate: Int? = null, // Number of moves to mate (positive = white mates, negative = black mates)
    val alternativeMoves: List<String> = emptyList(), // Alternative good moves from MultiPV
    val nps: Long = 0, // Nodes per second
    val hashfull: Int = 0 // Hash table fullness percentage
)