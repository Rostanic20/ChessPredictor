package com.chesspredictor.data.datasources

import com.chesspredictor.domain.entities.EngineSettings
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.CoreGraphics.CGRectMake
import platform.Foundation.*
import platform.WebKit.*
import platform.darwin.NSObject
import kotlin.concurrent.AtomicInt
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class StockfishDataSource : ChessEngineDataSource {
    private var webView: WKWebView? = null
    private val initialized = AtomicInt(0)
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val outputFlow = MutableSharedFlow<String>()
    private val initResumed = AtomicInt(0)
    private var initContinuation: CancellableContinuation<Boolean>? = null

    private fun escapeForJs(s: String): String =
        s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r")

    override suspend fun initialize() {
        withContext(Dispatchers.Main) {
            try {
                val config = WKWebViewConfiguration()
                val contentController = config.userContentController

                val handler = object : NSObject(), WKScriptMessageHandlerProtocol {
                    override fun userContentController(
                        userContentController: WKUserContentController,
                        didReceiveScriptMessage: WKScriptMessage
                    ) {
                        val body = didReceiveScriptMessage.body
                        if (body is Map<*, *>) {
                            val type = body["type"] as? String ?: return
                            val data = body["data"] as? String ?: return
                            when (type) {
                                "message" -> {
                                    coroutineScope.launch {
                                        outputFlow.emit(data)
                                    }
                                }
                                "ready" -> {
                                    initialized.compareAndSet(0, 1)
                                    if (initResumed.compareAndSet(0, 1)) {
                                        initContinuation?.resume(true)
                                        initContinuation = null
                                    }
                                }
                                "error" -> {
                                    NSLog("StockfishDataSource JS error: %@", data)
                                }
                            }
                        }
                    }
                }

                contentController.addScriptMessageHandler(handler, "iosHandler")

                val wv = WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = config)
                webView = wv

                val bundle = NSBundle.mainBundle
                val htmlPath = bundle.pathForResource("stockfish_loader", ofType = "html")
                if (htmlPath != null) {
                    val htmlUrl = NSURL.fileURLWithPath(htmlPath)
                    val dirUrl = htmlUrl.URLByDeletingLastPathComponent!!
                    wv.loadFileURL(htmlUrl, allowingReadAccessToURL = dirUrl)
                } else {
                    NSLog("StockfishDataSource: stockfish_loader.html not found in bundle")
                    initialized.compareAndSet(0, 1)
                    return@withContext
                }

                val initialized = suspendCancellableCoroutine<Boolean> { cont ->
                    initContinuation = cont
                    coroutineScope.launch {
                        delay(60_000)
                        if (initResumed.compareAndSet(0, 1)) {
                            NSLog("StockfishDataSource: Initialization timeout")
                            cont.resume(false)
                        }
                    }
                }

                if (!initialized) {
                    NSLog("StockfishDataSource: Engine failed to initialize")
                }
            } catch (e: Exception) {
                NSLog("StockfishDataSource: Failed to initialize: %@", e.message ?: "unknown")
            }
        }
    }

    override suspend fun sendCommand(command: String): String {
        return withContext(Dispatchers.Main) {
            val escaped = escapeForJs(command)
            webView?.evaluateJavaScript("sendCommand('$escaped')", completionHandler = null)
            if (command == "uci") return@withContext "uciok"
            ""
        }
    }

    override suspend fun setPosition(fen: String) {
        sendCommand("position fen $fen")
        delay(200)
        sendCommand("isready")
        delay(100)
    }

    private fun calculateElo(skillLevel: Int): Int = 800 + skillLevel * 100

    private suspend fun configureForSkillLevel(settings: EngineSettings) {
        sendCommand("setoption name Skill Level value ${settings.skillLevel}")

        if (settings.skillLevel < 20) {
            sendCommand("setoption name UCI_LimitStrength value true")
            sendCommand("setoption name UCI_Elo value ${calculateElo(settings.skillLevel)}")
        } else {
            sendCommand("setoption name UCI_LimitStrength value false")
        }

        sendCommand("setoption name Contempt value ${settings.contempt}")
        sendCommand("setoption name MultiPV value ${settings.multiPV}")

        if (settings.skillLevel < 10) {
            sendCommand("setoption name Hash value 16")
        } else {
            sendCommand("setoption name Hash value ${settings.hashSize}")
        }
    }

    override suspend fun analyze(settings: EngineSettings): EngineAnalysis {
        if (initialized.value == 0) {
            return EngineAnalysis(
                bestMove = "",
                evaluation = 0f,
                depth = 1,
                principalVariation = emptyList(),
                nodes = 0L,
                time = 0L
            )
        }

        return withContext(Dispatchers.Main) {
            sendCommand("stop")
            delay(100)
            sendCommand("isready")
            delay(100)

            configureForSkillLevel(settings)

            var bestMove = ""
            var evaluation = 0f
            var depth = 0
            var nodes = 0L
            var mate: Int? = null
            var nps = 0L
            var hashfull = 0
            val pv = mutableListOf<String>()
            val alternativeMoves = mutableListOf<String>()

            val job = coroutineScope.launch {
                outputFlow.collect { line ->
                    when {
                        line.startsWith("bestmove ") -> {
                            val parts = line.split(" ")
                            if (parts.size >= 2) {
                                bestMove = parts[1]
                            }
                            cancel()
                        }
                        line.startsWith("info ") && line.contains("depth ") -> {
                            val depthMatch = Regex("depth (\\d+)").find(line)
                            depthMatch?.let {
                                val newDepth = it.groupValues[1].toInt()
                                if (newDepth >= depth) depth = newDepth
                            }

                            val mateMatch = Regex("score mate (-?\\d+)").find(line)
                            if (mateMatch != null) {
                                mate = mateMatch.groupValues[1].toInt()
                                evaluation = if (mate!! > 0) 999f else -999f
                            } else {
                                val scoreMatch = Regex("score cp (-?\\d+)").find(line)
                                scoreMatch?.let {
                                    evaluation = it.groupValues[1].toFloat() / 100f
                                    mate = null
                                }
                            }

                            val nodesMatch = Regex("nodes (\\d+)").find(line)
                            nodesMatch?.let { nodes = it.groupValues[1].toLong() }

                            val npsMatch = Regex("nps (\\d+)").find(line)
                            npsMatch?.let { nps = it.groupValues[1].toLong() }

                            val hashMatch = Regex("hashfull (\\d+)").find(line)
                            hashMatch?.let { hashfull = it.groupValues[1].toInt() }

                            val pvMatch = Regex("pv (.+)").find(line)
                            pvMatch?.let {
                                val moves = it.groupValues[1].split(" ").filter { move ->
                                    move.matches(Regex("[a-h][1-8][a-h][1-8][qrnb]?"))
                                }
                                if (moves.isNotEmpty()) {
                                    pv.clear()
                                    pv.addAll(moves)
                                    val firstMove = moves[0]
                                    if (!alternativeMoves.contains(firstMove) && alternativeMoves.size < 3) {
                                        alternativeMoves.add(firstMove)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val goCommand = buildString {
                append("go")
                if (settings.difficulty.depth > 0) {
                    append(" depth ${settings.difficulty.depth}")
                }
                if (settings.timeLimit > 0) {
                    val actualTime = settings.timeLimit.coerceIn(300, 5000)
                    append(" movetime $actualTime")
                }
                if (settings.timeLimit < 100) {
                    append(" nodes 100000")
                }
            }

            sendCommand(goCommand)

            val timeout = maxOf(settings.timeLimit + 2000L, 5000L)
            val timeoutJob = coroutineScope.launch {
                delay(timeout)
                if (job.isActive) {
                    job.cancel()
                }
            }

            job.join()
            timeoutJob.cancel()

            EngineAnalysis(
                bestMove = bestMove,
                evaluation = evaluation,
                depth = depth,
                principalVariation = pv,
                nodes = nodes,
                time = settings.difficulty.timeMs.toLong(),
                mate = mate,
                alternativeMoves = alternativeMoves,
                nps = nps,
                hashfull = hashfull
            )
        }
    }

    override fun getOutput(): Flow<String> = outputFlow.asSharedFlow()

    override suspend fun stop() {
        sendCommand("stop")
    }
}
