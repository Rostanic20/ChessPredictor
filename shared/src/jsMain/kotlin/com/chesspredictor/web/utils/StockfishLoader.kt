package com.chesspredictor.web.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.browser.window
import kotlinx.browser.document
import org.w3c.dom.HTMLScriptElement

private val stockfishDeferred = CompletableDeferred<dynamic>()

suspend fun loadStockfish(): dynamic {
    // Always load from CDN for web version
    return loadStockfishFromCDN()
}

private suspend fun loadStockfishFromCDN(): dynamic {
    if (!stockfishDeferred.isCompleted) {
        try {
            // Load Stockfish.js WASM version which works better in browsers
            val wasmSupported = js("typeof WebAssembly === 'object'")
            
            if (wasmSupported) {
                // Use the WASM version for better performance
                val script = document.createElement("script") as HTMLScriptElement
                script.src = "https://unpkg.com/stockfish.wasm@0.10.0/stockfish.js"
                
                script.onload = {
                    // For stockfish.wasm, we need to wait for it to be ready
                    js("""
                        window.Stockfish().then(function(sf) {
                            window.StockfishInstance = sf;
                        });
                    """)
                    
                    // Give it time to initialize
                    window.setTimeout({
                        val StockfishConstructor = { ->
                            js("window.StockfishInstance")
                        }
                        stockfishDeferred.complete(StockfishConstructor)
                    }, 1000)
                }
                
                script.onerror = { _, _, _, _, _ ->
                    stockfishDeferred.completeExceptionally(Exception("Failed to load Stockfish WASM"))
                }
                
                document.head?.appendChild(script)
            } else {
                // Fallback to asm.js version
                val script = document.createElement("script") as HTMLScriptElement
                script.src = "https://unpkg.com/stockfish.js@10.0.2/stockfish.js"
                
                script.onload = {
                    val Stockfish = js("window.STOCKFISH")
                    stockfishDeferred.complete(Stockfish)
                }
                
                script.onerror = { _, _, _, _, _ ->
                    stockfishDeferred.completeExceptionally(Exception("Failed to load Stockfish"))
                }
                
                document.head?.appendChild(script)
            }
        } catch (e: Throwable) {
            stockfishDeferred.completeExceptionally(e)
        }
    }
    
    return stockfishDeferred.await()
}