package com.chesspredictor.eval

import com.chesspredictor.data.datasources.ChessEngineDataSource
import com.chesspredictor.data.datasources.EngineAnalysis
import com.chesspredictor.data.datasources.EngineStrength
import com.chesspredictor.domain.entities.EngineSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class NodeStockfish(script: File, transcript: File? = null) : ChessEngineDataSource, AutoCloseable {
    private val log = transcript?.printWriter()
    private val process = ProcessBuilder("node", script.absolutePath).redirectErrorStream(true).start()
    private val input = process.outputStream.bufferedWriter()
    private val lines = Channel<String>(Channel.UNLIMITED)
    private val searchesFinished = AtomicInteger()
    private val exclusive = Mutex()
    private var searchesStarted = 0
    private val recent = AtomicReference<List<String>>(emptyList())
    private val sent = ArrayDeque<String>()

    fun diagnostics(): String =
        "searches started=$searchesStarted finished=${searchesFinished.get()} alive=${process.isAlive} " +
            "sent=${sent.toList()} received=${recent.get().map { it.take(80) }}"

    init {
        thread(isDaemon = true) {
            val tail = ArrayDeque<String>()
            process.inputStream.bufferedReader().forEachLine {
                log?.let { writer -> synchronized(writer) { writer.println("< $it"); writer.flush() } }
                if (it.startsWith("bestmove ")) searchesFinished.incrementAndGet()
                lines.trySend(it)
                tail.addLast(it)
                if (tail.size > 5) tail.removeFirst()
                recent.set(tail.toList())
            }
            System.err.println("engine exited with ${process.waitFor()}, last output: $tail")
            lines.close()
        }
    }

    override suspend fun sendCommand(command: String): String {
        sent.addLast(command)
        if (sent.size > 6) sent.removeFirst()
        log?.let { writer -> synchronized(writer) { writer.println("> $command"); writer.flush() } }
        input.write(command)
        input.newLine()
        input.flush()
        return ""
    }

    override suspend fun initialize() = withContext(NonCancellable) {
        sendCommand("uci")
        awaitLine("uciok")
        settle()
    }

    override suspend fun setPosition(fen: String) = exclusive.withLock {
        withContext(NonCancellable) {
            settle()
            sendCommand("position fen $fen")
        }
        Unit
    }

    override suspend fun analyze(settings: EngineSettings): EngineAnalysis = exclusive.withLock {
        withContext(NonCancellable) { settle() }
        currentCoroutineContext().ensureActive()
        search(settings)
    }

    private suspend fun search(settings: EngineSettings): EngineAnalysis {
        val strength = EngineStrength.of(settings)
        sendCommand("setoption name UCI_LimitStrength value ${strength.limitStrength}")
        strength.elo?.let { sendCommand("setoption name UCI_Elo value $it") }
        sendCommand("setoption name Skill Level value ${strength.skillLevel}")
        sendCommand("setoption name MultiPV value ${settings.multiPV}")
        val go = buildString {
            append("go depth ${strength.depth}")
            if (settings.timeLimit > 0) append(" movetime ${settings.timeLimit.coerceIn(300, 5000)}")
            if (settings.timeLimit < 100) append(" nodes 100000")
        }
        sendCommand(go)
        searchesStarted++

        var evaluation = 0f
        var depth = 0
        var mate: Int? = null
        while (true) {
            val line = lines.receive()
            if (line.startsWith("bestmove ")) {
                return EngineAnalysis(
                    bestMove = line.split(" ")[1].takeIf { it != "(none)" } ?: "",
                    evaluation = evaluation,
                    depth = depth,
                    nodes = 0L,
                    time = settings.timeLimit.toLong(),
                    mate = mate
                )
            }
            if (line.startsWith("info ") && line.contains(" depth ")) {
                DEPTH.find(line)?.let { depth = maxOf(depth, it.groupValues[1].toInt()) }
                val mateScore = MATE.find(line)
                if (mateScore != null) {
                    mate = mateScore.groupValues[1].toInt()
                    evaluation = if (mate > 0) 999f else -999f
                } else {
                    CP.find(line)?.let {
                        evaluation = it.groupValues[1].toFloat() / 100f
                        mate = null
                    }
                }
            }
        }
    }

    override suspend fun stop() {
        sendCommand("stop")
    }

    override fun getOutput(): Flow<String> = emptyFlow()

    override fun close() {
        runCatching { input.write("quit\n"); input.flush() }
        process.destroy()
        log?.close()
    }

    private suspend fun settle() {
        if (searchesFinished.get() < searchesStarted) {
            sendCommand("stop")
            withContext(Dispatchers.IO) {
                while (searchesFinished.get() < searchesStarted) Thread.sleep(5)
            }
        }
        sendCommand("isready")
        awaitLine("readyok")
    }

    private suspend fun awaitLine(prefix: String) {
        while (!lines.receive().startsWith(prefix)) Unit
    }

    private companion object {
        val DEPTH = Regex(" depth (\\d+)")
        val MATE = Regex("score mate (-?\\d+)")
        val CP = Regex("score cp (-?\\d+)")
    }
}
