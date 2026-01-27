package com.chesspredictor.data.datasources

import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.domain.entities.EngineDifficulty
import com.chesspredictor.web.utils.getStockfishWorker
import com.chesspredictor.web.utils.console
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import org.w3c.dom.Worker

actual class StockfishDataSource : ChessEngineDataSource {
    private var worker: Worker? = null
    
    private val outputFlow = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    
    override suspend fun initialize() {
        try {
            // Get the Stockfish worker - this already runs on Main dispatcher
            worker = getStockfishWorker()
            
            worker?.addEventListener("message", { event: dynamic ->
                val data = event.data
                val message = data.toString()

                GlobalScope.launch {
                    outputFlow.emit(message)
                }
            })

            sendCommand("uci")
            delay(200)
        } catch (e: Throwable) {
            console.error("Failed to initialize Stockfish: ${e.message}")
            console.error(e)
            throw Exception("Failed to initialize Stockfish engine: ${e.message}")
        }
    }
    
    override suspend fun sendCommand(command: String): String {
        worker?.postMessage(command)
        return ""
    }
    
    override suspend fun setPosition(fen: String) {
        sendCommand("position fen $fen")
    }
    
    override suspend fun analyze(settings: EngineSettings): EngineAnalysis {
        val depth = settings.difficulty.depth
        val timeLimit = settings.timeLimit  // Use actual timeLimit setting, not difficulty default!
        val skillLevel = settings.difficulty.skillLevel
        
        sendCommand("setoption name UCI_LimitStrength value true")
        delay(50)
        sendCommand("setoption name UCI_Elo value ${settings.difficulty.eloRating}")
        delay(50)

        sendCommand("isready")
        delay(100)

        sendCommand("go depth $depth movetime $timeLimit")
        
        var bestMove = ""
        var evaluation = 0f
        var currentDepth = 0
        var nodes = 0L
        val pv = mutableListOf<String>()
        
        val startTime = js("Date.now()").unsafeCast<Double>().toLong()
        
        outputFlow.takeWhile { line ->
                when {
                    line.startsWith("info") -> {
                        val parts = line.split(" ")
                        var i = 0
                        var pvStart = -1
                        while (i < parts.size) {
                            when (parts[i]) {
                                "depth" -> currentDepth = parts.getOrNull(i + 1)?.toIntOrNull() ?: 0
                                "cp" -> evaluation = (parts.getOrNull(i + 1)?.toFloatOrNull() ?: 0f) / 100f
                                "mate" -> {
                                    val mateIn = parts.getOrNull(i + 1)?.toIntOrNull() ?: 0
                                    evaluation = if (mateIn > 0) 100f else -100f
                                }
                                "nodes" -> nodes = parts.getOrNull(i + 1)?.toLongOrNull() ?: 0L
                                "pv" -> pvStart = i + 1
                            }
                            i++
                        }
                        if (pvStart > 0) {
                            pv.clear()
                            pv.addAll(parts.subList(pvStart, parts.size))
                        }
                        true // Continue collecting
                    }
                    line.startsWith("bestmove") -> {
                        bestMove = line.split(" ").getOrNull(1) ?: ""
                        false // Stop collecting after bestmove
                    }
                    else -> true // Continue for other lines
                }
            }.collect { /* line already processed above */ }
        
        return EngineAnalysis(
            bestMove = bestMove,
            evaluation = evaluation,
            depth = currentDepth,
            nodes = nodes,
            time = js("Date.now()").unsafeCast<Double>().toLong() - startTime,
            principalVariation = pv
        )
    }
    
    override suspend fun stop() {
        sendCommand("stop")
        delay(100)
        sendCommand("quit")
    }
    
    override fun getOutput(): Flow<String> = outputFlow.asSharedFlow()
    
    private suspend fun waitForResponse(expected: String, timeoutMs: Long) {
        withTimeout(timeoutMs) {
            outputFlow.first { it.contains(expected) }
        }
    }
}