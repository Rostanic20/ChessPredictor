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

        private val DEPTH = Regex(" depth (\\d+)")
        private val MATE = Regex("score mate (-?\\d+)")
        private val CP = Regex("score cp (-?\\d+)")
        private val NODES = Regex(" nodes (\\d+)")
        private val NPS = Regex(" nps (\\d+)")
        private val HASHFULL = Regex(" hashfull (\\d+)")
        private val PV = Regex(" pv (.+)")
        private val UCI_MOVE = Regex("[a-h][1-8][a-h][1-8][qrnb]?")

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
    private val session = UciSession(::sendCommand)
    
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
                            val path = url.path?.removePrefix("/").orEmpty()
                            return runCatching {
                                WebResourceResponse(mimeTypeOf(path), "UTF-8", context.assets.open(path))
                            }.getOrElse {
                                WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", emptyMap(), null)
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            Log.e(TAG, "WebView error: ${error?.description} for ${request?.url}")
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
            session.onLine(message)
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
        if (isInitialized) session.setPosition(fen)
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
        
        val strength = EngineStrength.of(settings)

        var evaluation = 0f
        var depth = 0
        var nodes = 0L
        var mate: Int? = null
        var nps = 0L
        var hashfull = 0
        val pv = mutableListOf<String>()
        val alternativeMoves = mutableListOf<String>()

        val timeout = maxOf(settings.timeLimit + 2000L, 5000L)
        val bestMoveLine = session.search(strength.setupCommands(settings.multiPV), strength.goCommand(settings.timeLimit), timeout) { line ->
            if (!line.contains(" depth ")) return@search
            DEPTH.find(line)?.let { depth = maxOf(depth, it.groupValues[1].toInt()) }
            val mateScore = MATE.find(line)
            if (mateScore != null) {
                mate = mateScore.groupValues[1].toInt()
                evaluation = if (mateScore.groupValues[1].toInt() > 0) 999f else -999f
            } else {
                CP.find(line)?.let {
                    evaluation = it.groupValues[1].toFloat() / 100f
                    mate = null
                }
            }
            NODES.find(line)?.let { nodes = it.groupValues[1].toLong() }
            NPS.find(line)?.let { nps = it.groupValues[1].toLong() }
            HASHFULL.find(line)?.let { hashfull = it.groupValues[1].toInt() }
            PV.find(line)?.let { match ->
                val moves = match.groupValues[1].split(" ").filter { UCI_MOVE.matches(it) }
                if (moves.isNotEmpty()) {
                    pv.clear()
                    pv.addAll(moves)
                    if (moves[0] !in alternativeMoves && alternativeMoves.size < 3) alternativeMoves.add(moves[0])
                }
            }
        }
        if (bestMoveLine == null) Log.w(TAG, "Engine analysis timed out after ${timeout}ms")

        return EngineAnalysis(
            bestMove = bestMoveLine?.split(" ")?.getOrNull(1)?.takeIf { it != "(none)" }.orEmpty(),
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
    
    override fun getOutput(): Flow<String> = outputFlow.asSharedFlow()
    
    override suspend fun stop() {
        sendCommand("stop")
    }
}