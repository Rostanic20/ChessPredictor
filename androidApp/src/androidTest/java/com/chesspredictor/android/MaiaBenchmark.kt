package com.chesspredictor.android

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chesspredictor.domain.humanlike.MaiaNetwork
import com.chesspredictor.domain.humanlike.MaiaWeights
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaiaBenchmark {

    @Test
    fun timeTheKotlinNetwork() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val loadStarted = System.nanoTime()
        val network = MaiaNetwork(MaiaWeights.parse(context.assets.open("maia3-5m.f16.bin").use { it.readBytes() }))
        val loadMs = (System.nanoTime() - loadStarted) / 1_000_000

        val history = listOf(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
            "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
        )
        network.logits(history, 1500, 1500)
        val times = List(10) {
            val started = System.nanoTime()
            network.logits(history, 1500, 1500)
            (System.nanoTime() - started) / 1_000_000
        }.sorted()

        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString("maia", "load=${loadMs} ms median=${times[5]} ms max=${times.last()} ms")
        })
    }
}
