package com.chesspredictor.eval

import com.chesspredictor.data.datasources.ChessEngineDataSource
import com.chesspredictor.data.datasources.EngineAnalysis
import com.chesspredictor.data.datasources.EngineStrength
import com.chesspredictor.data.datasources.UciSession
import com.chesspredictor.domain.entities.EngineSettings
import com.chesspredictor.presentation.ChessConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.io.File
import kotlin.concurrent.thread

class NodeStockfish(script: File, transcript: File? = null) : ChessEngineDataSource, AutoCloseable {
    private val log = transcript?.printWriter()
    private val process = ProcessBuilder("node", script.absolutePath).redirectErrorStream(true).start()
    private val input = process.outputStream.bufferedWriter()
    private val session = UciSession(::write, clock = Dispatchers.Default)
    private val sent = ArrayDeque<String>()
    @Volatile private var received = emptyList<String>()

    fun diagnostics(): String = "alive=${process.isAlive} sent=${sent.toList()} received=${received.map { it.take(80) }}"

    init {
        thread(isDaemon = true) {
            val tail = ArrayDeque<String>()
            process.inputStream.bufferedReader().forEachLine {
                log?.let { writer -> synchronized(writer) { writer.println("< $it"); writer.flush() } }
                session.onLine(it)
                tail.addLast(it)
                if (tail.size > 5) tail.removeFirst()
                received = tail.toList()
            }
            System.err.println("engine exited with ${process.waitFor()}, last output: $tail")
        }
    }

    private fun write(command: String) {
        sent.addLast(command)
        if (sent.size > 6) sent.removeFirst()
        log?.let { writer -> synchronized(writer) { writer.println("> $command"); writer.flush() } }
        input.write(command)
        input.newLine()
        input.flush()
    }

    override suspend fun sendCommand(command: String): String {
        write(command)
        return ""
    }

    override suspend fun initialize() {
        write("uci")
        session.setPosition(ChessConstants.STARTING_POSITION_FEN)
    }

    override suspend fun setPosition(fen: String) = session.setPosition(fen)

    override suspend fun analyze(settings: EngineSettings): EngineAnalysis {
        val strength = EngineStrength.of(settings)
        var evaluation = 0f
        var depth = 0
        var mate: Int? = null
        val line = session.search(strength.setupCommands(settings.multiPV), strength.goCommand(settings.timeLimit), 60_000) { info ->
            DEPTH.find(info)?.let { depth = maxOf(depth, it.groupValues[1].toInt()) }
            val mateScore = MATE.find(info)
            if (mateScore != null) {
                mate = mateScore.groupValues[1].toInt()
                evaluation = if (mateScore.groupValues[1].toInt() > 0) 999f else -999f
            } else {
                CP.find(info)?.let {
                    evaluation = it.groupValues[1].toFloat() / 100f
                    mate = null
                }
            }
        }
        return EngineAnalysis(
            bestMove = line?.split(" ")?.getOrNull(1)?.takeIf { it != "(none)" }.orEmpty(),
            evaluation = evaluation,
            depth = depth,
            nodes = 0L,
            time = settings.timeLimit.toLong(),
            mate = mate
        )
    }

    override suspend fun stop() = write("stop")

    override fun getOutput(): Flow<String> = emptyFlow()

    override fun close() {
        runCatching { write("quit") }
        process.destroy()
        log?.close()
    }

    private companion object {
        val DEPTH = Regex(" depth (\\d+)")
        val MATE = Regex("score mate (-?\\d+)")
        val CP = Regex("score cp (-?\\d+)")
    }
}
