package com.chesspredictor.android

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.DataInputStream
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer

@RunWith(AndroidJUnit4::class)
class MaiaBenchmark {

    private class Sample(val tokens: FloatArray, val selfElo: Long, val oppoElo: Long, val expected: Int, val legal: BooleanArray)

    @Test
    fun timeAndCheckMaia() {
        val dir = InstrumentationRegistry.getArguments().getString("maiaDir")
        assumeTrue("pass -e maiaDir <dir>", dir != null)
        val (shape, samples) = readSamples(File(dir, "bench_inputs.bin"))
        val report = Bundle()
        val environment = OrtEnvironment.getEnvironment()

        for (model in listOf("maia3-5m.onnx", "maia3-5m-int8.onnx")) {
            for (threads in listOf(1, 4)) {
                val options = OrtSession.SessionOptions().apply { setIntraOpNumThreads(threads) }
                environment.createSession(File(dir, model).readBytes(), options).use { session ->
                    var agreed = 0
                    val times = mutableListOf<Long>()
                    repeat(3) { round ->
                        samples.forEach { sample ->
                            val started = System.nanoTime()
                            val best = bestMove(environment, session, shape, sample)
                            if (round > 0) times += System.nanoTime() - started
                            if (round == 0 && best == sample.expected) agreed++
                        }
                    }
                    times.sort()
                    val line = "$model threads=$threads median=${times[times.size / 2] / 1_000_000.0} ms " +
                        "p95=${times[times.size * 95 / 100] / 1_000_000.0} ms agree=$agreed/${samples.size}"
                    report.putString("$model-$threads", line)
                }
            }
        }
        InstrumentationRegistry.getInstrumentation().sendStatus(0, report)
    }

    private fun bestMove(environment: OrtEnvironment, session: OrtSession, shape: LongArray, sample: Sample): Int {
        val tokens = OnnxTensor.createTensor(environment, FloatBuffer.wrap(sample.tokens), shape)
        val selfElo = OnnxTensor.createTensor(environment, LongBuffer.wrap(longArrayOf(sample.selfElo)), longArrayOf(1))
        val oppoElo = OnnxTensor.createTensor(environment, LongBuffer.wrap(longArrayOf(sample.oppoElo)), longArrayOf(1))
        return session.run(mapOf("tokens" to tokens, "self_elo" to selfElo, "oppo_elo" to oppoElo), setOf("move_logits")).use { result ->
            val logits = (result[0].value as Array<FloatArray>)[0]
            listOf(tokens, selfElo, oppoElo).forEach { it.close() }
            logits.indices.filter { sample.legal[it] }.maxBy { logits[it] }
        }
    }

    private fun readSamples(file: File): Pair<LongArray, List<Sample>> = DataInputStream(file.inputStream().buffered()).use { input ->
        val count = input.readInt()
        val squares = input.readInt()
        val features = input.readInt()
        val moves = input.readInt()
        val samples = List(count) {
            val tokens = FloatArray(squares * features) { input.readFloat() }
            val selfElo = input.readLong()
            val oppoElo = input.readLong()
            val expected = input.readInt()
            val packed = ByteArray((moves + 7) / 8).also { input.readFully(it) }
            val legal = BooleanArray(moves) { index -> (packed[index / 8].toInt() shr (7 - index % 8)) and 1 == 1 }
            Sample(tokens, selfElo, oppoElo, expected, legal)
        }
        longArrayOf(1, squares.toLong(), features.toLong()) to samples
    }
}
