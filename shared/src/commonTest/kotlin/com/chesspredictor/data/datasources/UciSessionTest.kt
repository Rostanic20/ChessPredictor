package com.chesspredictor.data.datasources

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UciSessionTest {

    private class StrictEngine(private val scope: CoroutineScope, private val searchMs: Long, private val stopLatencyMs: Long) {
        val session = UciSession(::receive)
        val violations = mutableListOf<String>()
        val sent = mutableListOf<String>()
        private var position = ""
        private var searching = false
        private var finishing: Job? = null

        private fun receive(command: String) {
            sent += command
            when {
                command.startsWith("go") -> {
                    if (searching) violations += "go while searching"
                    searching = true
                    val searched = position
                    finishing = scope.launch {
                        delay(searchMs)
                        finish(searched)
                    }
                }
                command == "stop" -> if (searching) {
                    val searched = position
                    finishing?.cancel()
                    finishing = scope.launch {
                        delay(stopLatencyMs)
                        finish(searched)
                    }
                }
                command == "isready" -> scope.launch { session.onLine("readyok") }
                command.startsWith("position fen ") -> {
                    if (searching) violations += "position while searching"
                    position = command.removePrefix("position fen ")
                }
            }
        }

        private fun finish(searched: String) {
            searching = false
            session.onLine("info depth 1 score cp 10 pv e2e4")
            session.onLine("bestmove $searched")
        }
    }

    private fun TestScope.engine(searchMs: Long, stopLatencyMs: Long) = StrictEngine(backgroundScope, searchMs, stopLatencyMs)

    @Test
    fun aSlowStopIsAwaitedBeforeTheNextSearch() = runTest {
        val engine = engine(searchMs = 2_000, stopLatencyMs = 300)
        val stale = launch {
            engine.session.setPosition("A")
            engine.session.search(emptyList(), "go", 10_000) {}
        }
        advanceTimeBy(50)
        stale.cancel()

        engine.session.setPosition("B")
        val result = engine.session.search(emptyList(), "go", 10_000) {}

        assertEquals("bestmove B", result)
        assertEquals(emptyList(), engine.violations)
    }

    @Test
    fun aSearchThatAlreadyEndedIsNotStopped() = runTest {
        val engine = engine(searchMs = 100, stopLatencyMs = 300)
        engine.session.setPosition("A")
        assertEquals("bestmove A", engine.session.search(emptyList(), "go", 10_000) {})
        val before = currentTime

        engine.session.setPosition("B")
        assertEquals("bestmove B", engine.session.search(emptyList(), "go", 10_000) {})

        assertFalse("stop" in engine.sent)
        assertTrue(currentTime - before < 1_000, "took ${currentTime - before} ms")
        assertEquals(emptyList(), engine.violations)
    }

    @Test
    fun infoLinesReachTheCallerBeforeTheBestMove() = runTest {
        val engine = engine(searchMs = 100, stopLatencyMs = 300)
        val infos = mutableListOf<String>()
        engine.session.setPosition("A")
        engine.session.search(listOf("setoption name MultiPV value 1"), "go", 10_000) { infos += it }

        assertEquals(listOf("info depth 1 score cp 10 pv e2e4"), infos)
        assertTrue(engine.sent.indexOf("setoption name MultiPV value 1") < engine.sent.indexOf("go"))
    }
}
