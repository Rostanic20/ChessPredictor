package com.chesspredictor.web.utils

import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.Worker
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

external fun setTimeout(handler: () -> Unit, timeout: Int): Int
external fun clearTimeout(handle: Int)

private var stockfishWorker: Worker? = null
private val initDeferred = CompletableDeferred<Worker>()

suspend fun getStockfishWorker(): Worker {
    if (stockfishWorker != null) {
        return stockfishWorker!!
    }
    
    if (!initDeferred.isCompleted) {
        try {
            stockfishWorker = Worker("stockfish.js")
            
            // Test if worker responds to UCI
            val testSuccess = suspendCancellableCoroutine<Boolean> { cont ->
                var resumed = false
                val timeout = setTimeout({
                    if (!resumed) {
                        resumed = true
                        cont.resume(false)
                    }
                }, 3000)

                stockfishWorker?.onmessage = { event ->
                    val message = event.data.toString()
                    if (!resumed) {
                        resumed = true
                        clearTimeout(timeout)
                        stockfishWorker?.onmessage = null
                        cont.resume(true)
                    }
                }
                
                stockfishWorker?.onerror = { error ->
                    console.error("Stockfish worker error:", error)
                    if (!resumed) {
                        resumed = true
                        clearTimeout(timeout)
                        cont.resume(false)
                    }
                }
                
                // Send test UCI command
                stockfishWorker?.postMessage("uci")
            }
            
            if (testSuccess) {
                initDeferred.complete(stockfishWorker!!)
            } else {
                throw Exception("Worker did not respond to UCI")
            }
        } catch (e: Throwable) {
            try {
                val stockfishLoader = js("window.StockfishLoader")
                val promise = stockfishLoader.loadStockfish() as Promise<Worker>

                stockfishWorker = promise.await()
                initDeferred.complete(stockfishWorker!!)
            } catch (cdnError: Throwable) {
                
                stockfishWorker = createSimpleEngine()
                initDeferred.complete(stockfishWorker!!)
            }
        }
    }
    
    return initDeferred.await()
}

@JsName("Promise")
external class Promise<T> {
    fun then(onFulfilled: (T) -> Unit): Promise<Unit>
    fun catch(onRejected: (Throwable) -> Unit): Promise<Unit>
}

suspend fun <T> Promise<T>.await(): T = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
    then { value ->
        cont.resume(value) { }
    }.catch { error ->
        cont.resumeWithException(error)
    }
}