package com.chesspredictor.data.datasources

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import android.webkit.*
import com.chesspredictor.domain.entities.EngineSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

actual class StockfishDataSource(private val context: Context) : ChessEngineDataSource {
    companion object {
        private const val TAG = "StockfishDataSource"
        private const val ASSET_HOST = "appassets.androidplatform.net"

        private fun mimeTypeOf(path: String): String = when (path.substringAfterLast('.')) {
            "html" -> "text/html"
            "js" -> "text/javascript"
            else -> "application/octet-stream"
        }
    }
    
    private var webView: WebView? = null
    private var isInitialized = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val outputFlow = MutableSharedFlow<String>()
    private val initLatch = CountDownLatch(1)
    
    override suspend fun initialize() {
        withContext(Dispatchers.Main) {
            try {
                WebView.setWebContentsDebuggingEnabled(
                    context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
                )

                webView = WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        allowFileAccess = false
                        allowContentAccess = false
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        setSupportMultipleWindows(false)
                    }

                    addJavascriptInterface(StockfishJSInterface(), "Android")

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val url = request?.url ?: return null
                            if (url.host != ASSET_HOST) return null
                            val path = url.path?.removePrefix("/") ?: return null
                            return runCatching {
                                WebResourceResponse(mimeTypeOf(path), "UTF-8", context.assets.open(path))
                            }.getOrNull()
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            Log.e(TAG, "WebView error: ${error?.description}")
                        }
                    }
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            consoleMessage?.let {
                                if (it.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                                    Log.e(TAG, "[JS ERROR] ${it.message()} (${it.sourceId()}:${it.lineNumber()})")
                                }
                            }
                            return true
                        }
                    }
                }
                
                webView?.loadUrl("https://$ASSET_HOST/stockfish_loader.html")
                
                withContext(Dispatchers.IO) {
                    val initialized = initLatch.await(60, TimeUnit.SECONDS)
                    if (!initialized) {
                        Log.e(TAG, "Stockfish initialization timeout")
                        isInitialized = false
                        throw IllegalStateException("Stockfish engine initialization timed out after 60 seconds")
                    } else {
                        isInitialized = true
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Stockfish", e)
                isInitialized = false
                throw e
            }
        }
    }

    @Suppress("unused")
    inner class StockfishJSInterface {
        @JavascriptInterface
        fun onMessage(message: String) {
            coroutineScope.launch {
                outputFlow.emit(message)
            }
        }
        
        @JavascriptInterface
        fun onReady() {
            isInitialized = true
            initLatch.countDown()
        }
        
        @JavascriptInterface
        fun onError(error: String) {
            Log.e(TAG, "Stockfish error: $error")
        }
    }
    
    override suspend fun sendCommand(command: String): String {
        return withContext(Dispatchers.Main) {
            webView?.evaluateJavascript("sendCommand('$command')", null)
            
            if (command == "uci") {
                return@withContext "uciok"
            }
            ""
        }
    }
    
    override suspend fun setPosition(fen: String) {
        Log.d(TAG, "Setting position: $fen")
        sendCommand("position fen $fen")
        delay(200)
        sendCommand("isready")
        delay(100)
        Log.d(TAG, "Position set, proceeding with analysis")
    }
    
    private suspend fun applyStrength(strength: EngineStrength, settings: EngineSettings) {
        sendCommand("setoption name UCI_LimitStrength value ${strength.limitStrength}")
        strength.elo?.let { sendCommand("setoption name UCI_Elo value $it") }
        sendCommand("setoption name Skill Level value ${strength.skillLevel}")
        sendCommand("setoption name MultiPV value ${settings.multiPV}")
    }

    override suspend fun analyze(settings: EngineSettings): EngineAnalysis {
        if (!isInitialized) {
            Log.w(TAG, "Engine not initialized, returning empty analysis")
            return EngineAnalysis(
                bestMove = "",
                evaluation = 0f,
                depth = 1,
                principalVariation = emptyList(),
                nodes = 0L,
                time = 0L
            )
        }
        
        return withContext(Dispatchers.IO) {
            sendCommand("stop")
            delay(100)

            sendCommand("isready")
            delay(100)
            
            val strength = EngineStrength.of(settings)
            applyStrength(strength, settings)

            var bestMove = ""
            var evaluation = 0f
            var depth = 0
            var nodes = 0L
            var mate: Int? = null
            var nps = 0L
            var hashfull = 0
            val pv = mutableListOf<String>()
            val alternativeMoves = mutableListOf<String>()
            
            val job = launch {
                outputFlow.collect { line ->
                    when {
                        line.startsWith("bestmove ") -> {
                            Log.d(TAG, "Engine response: $line")
                            val parts = line.split(" ")
                            if (parts.size >= 2) {
                                bestMove = parts[1]
                                Log.d(TAG, "Extracted bestMove: $bestMove")
                            }
                            cancel()
                        }
                        line.startsWith("info ") && line.contains("depth ") -> {
                            val depthMatch = Regex("depth (\\d+)").find(line)
                            depthMatch?.let { 
                                val newDepth = it.groupValues[1].toInt()
                                if (newDepth >= depth) {
                                    depth = newDepth
                                }
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
                append("go depth ${strength.depth}")
                if (settings.timeLimit > 0) {
                    val actualTime = settings.timeLimit.coerceIn(300, 5000)
                    append(" movetime $actualTime")
                }
                if (settings.timeLimit < 100) {
                    append(" nodes 100000")
                }
            }
            
            Log.d(TAG, "Sending go command: $goCommand")
            sendCommand(goCommand)
            
            val timeout = maxOf(settings.timeLimit + 2000L, 5000L)
            val timeoutJob = launch {
                delay(timeout)
                if (job.isActive) {
                    Log.w(TAG, "Engine analysis timed out after ${timeout}ms")
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