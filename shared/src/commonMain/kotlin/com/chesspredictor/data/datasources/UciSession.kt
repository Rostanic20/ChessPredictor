package com.chesspredictor.data.datasources

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class UciSession(private val send: suspend (String) -> Unit) {
    private val lines = MutableSharedFlow<String>(extraBufferCapacity = 1024, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val searchesFinished = MutableStateFlow(0)
    private val exclusive = Mutex()
    private var searchesStarted = 0

    fun onLine(line: String) {
        if (line.startsWith("bestmove")) searchesFinished.update { it + 1 }
        lines.tryEmit(line)
    }

    suspend fun setPosition(fen: String) = exclusive.withLock {
        withContext(NonCancellable) {
            settle()
            send("position fen $fen")
        }
    }

    suspend fun search(setup: List<String>, go: String, timeoutMs: Long, onInfo: (String) -> Unit): String? =
        exclusive.withLock {
            withContext(NonCancellable) { settle() }
            setup.forEach { send(it) }
            coroutineScope {
                val bestmove = async(start = CoroutineStart.UNDISPATCHED) {
                    lines.transformWhile { line ->
                        emit(line)
                        !line.startsWith("bestmove")
                    }.onEach { if (it.startsWith("info")) onInfo(it) }.last()
                }
                withContext(NonCancellable) {
                    searchesStarted++
                    send(go)
                }
                withTimeoutOrNull(timeoutMs) { bestmove.await() }.also { if (it == null) bestmove.cancel() }
            }
        }

    private suspend fun settle() {
        if (searchesFinished.value < searchesStarted) {
            send("stop")
            val stopped = withTimeoutOrNull(STOP_TIMEOUT_MS) { searchesFinished.first { it >= searchesStarted } }
            if (stopped == null) searchesFinished.value = searchesStarted
        }
        coroutineScope {
            val ready = async(start = CoroutineStart.UNDISPATCHED) { lines.first { it == "readyok" } }
            send("isready")
            withTimeoutOrNull(READY_TIMEOUT_MS) { ready.await() } ?: ready.cancel()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val READY_TIMEOUT_MS = 5_000L
    }
}
