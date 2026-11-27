package com.chesspredictor.domain.humanlike

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

class MaiaWeights private constructor(private val tensors: Map<String, FloatArray>) {
    operator fun get(name: String): FloatArray = tensors[name] ?: error("missing tensor $name")

    companion object {
        fun parse(bytes: ByteArray): MaiaWeights {
            val reader = LittleEndianReader(bytes)
            check(reader.ascii(8) == "MAIA3F16") { "not a Maia-3 fp16 weight file" }
            val tensors = HashMap<String, FloatArray>()
            repeat(reader.int()) {
                val name = reader.ascii(reader.unsignedShort())
                val size = (0 until reader.byte()).fold(1) { total, _ -> total * reader.int() }
                tensors[name] = FloatArray(size) { halfToFloat(reader.unsignedShort()) }
            }
            return MaiaWeights(tensors)
        }

        private fun halfToFloat(bits: Int): Float {
            val sign = if (bits and 0x8000 != 0) -1f else 1f
            val exponent = (bits shr 10) and 0x1f
            val mantissa = bits and 0x3ff
            return when (exponent) {
                0 -> sign * mantissa * 5.9604645e-8f
                31 -> if (mantissa == 0) sign * Float.POSITIVE_INFINITY else Float.NaN
                else -> sign * Float.fromBits(((exponent - 15 + 127) shl 23) or (mantissa shl 13))
            }
        }
    }

    private class LittleEndianReader(private val bytes: ByteArray) {
        private var position = 0
        fun byte(): Int = bytes[position++].toInt() and 0xff
        fun unsignedShort(): Int = byte() or (byte() shl 8)
        fun int(): Int = unsignedShort() or (unsignedShort() shl 16)
        fun ascii(length: Int): String = bytes.decodeToString(position, position + length).also { position += length }
    }
}

class LazyMaiaNetwork(load: () -> ByteArray) : HumanMovePredictor {
    private val network by lazy { MaiaNetwork(MaiaWeights.parse(load())) }

    override suspend fun logits(fens: List<String>, selfElo: Int, oppoElo: Int): FloatArray = network.logits(fens, selfElo, oppoElo)
}

class MaiaNetwork(private val weights: MaiaWeights) : HumanMovePredictor {

    override suspend fun logits(fens: List<String>, selfElo: Int, oppoElo: Int): FloatArray {
        val tokens = MaiaEncoding.tokens(fens)
        var x = embed(tokens, selfElo, oppoElo)
        for (block in 0 until BLOCKS) {
            currentCoroutineContext().ensureActive()
            x = block(x, "transformer.layers.$block.")
        }
        x = layerNorm(x, SQUARES, DIM, weights["transformer.norm.weight"], weights["transformer.norm.bias"])
        return policy(x)
    }

    private fun embed(tokens: FloatArray, selfElo: Int, oppoElo: Int): FloatArray {
        val selfEmbedding = elo(selfElo)
        val oppoEmbedding = elo(oppoElo)
        val input = FloatArray(SQUARES * PROJECTION_IN)
        for (square in 0 until SQUARES) {
            tokens.copyInto(input, square * PROJECTION_IN, square * MaiaEncoding.FEATURES, square * MaiaEncoding.FEATURES + BOARD_FEATURES)
            selfEmbedding.copyInto(input, square * PROJECTION_IN + BOARD_FEATURES)
            oppoEmbedding.copyInto(input, square * PROJECTION_IN + BOARD_FEATURES + ELO_DIM)
        }
        return linear(input, SQUARES, PROJECTION_IN, weights["token_projection.weight"], weights["token_projection.bias"], DIM)
    }

    private fun elo(rating: Int): FloatArray {
        val low = rating.coerceIn(0, MAX_ELO).toFloat() / MAX_ELO
        val lowEmbedding = weights["elo_embedding_low.weight"]
        val highEmbedding = weights["elo_embedding_high.weight"]
        return FloatArray(ELO_DIM) { low * lowEmbedding[it] + (1 - low) * highEmbedding[it] }
    }

    private fun block(x: FloatArray, prefix: String): FloatArray {
        val attention = attention(x, prefix)
        for (i in x.indices) attention[i] += x[i]
        val afterAttention = rmsNorm(attention, weights["${prefix}norm1.weight"])
        val hidden = linear(afterAttention, SQUARES, DIM, weights["${prefix}linear1.weight"], weights["${prefix}linear1.bias"], MLP_DIM)
        for (i in hidden.indices) hidden[i] = gelu(hidden[i])
        val feedForward = linear(hidden, SQUARES, MLP_DIM, weights["${prefix}linear2.weight"], weights["${prefix}linear2.bias"], DIM)
        for (i in feedForward.indices) feedForward[i] += afterAttention[i]
        return rmsNorm(feedForward, weights["${prefix}norm2.weight"])
    }

    private fun attention(x: FloatArray, prefix: String): FloatArray {
        val bias = geometricBias(x, prefix)
        val qkv = linear(x, SQUARES, DIM, weights["${prefix}self_attn.mha.in_proj_weight"], null, 3 * DIM)
        val heads = FloatArray(SQUARES * DIM)
        val scores = FloatArray(SQUARES)
        val scale = 1f / sqrt(HEAD_DIM.toFloat())
        for (head in 0 until HEADS) {
            val offset = head * HEAD_DIM
            for (query in 0 until SQUARES) {
                var top = Float.NEGATIVE_INFINITY
                for (key in 0 until SQUARES) {
                    var dot = 0f
                    val q = query * 3 * DIM + offset
                    val k = key * 3 * DIM + DIM + offset
                    for (d in 0 until HEAD_DIM) dot += qkv[q + d] * qkv[k + d]
                    val score = dot * scale + bias[(head * SQUARES + query) * SQUARES + key]
                    scores[key] = score
                    if (score > top) top = score
                }
                var total = 0f
                for (key in 0 until SQUARES) {
                    scores[key] = exp(scores[key] - top)
                    total += scores[key]
                }
                val out = query * DIM + offset
                for (key in 0 until SQUARES) {
                    val weight = scores[key] / total
                    val v = key * 3 * DIM + 2 * DIM + offset
                    for (d in 0 until HEAD_DIM) heads[out + d] += weight * qkv[v + d]
                }
            }
        }
        return linear(heads, SQUARES, DIM, weights["${prefix}self_attn.mha.out_proj.weight"], null, DIM)
    }

    private fun geometricBias(x: FloatArray, prefix: String): FloatArray {
        val mean = FloatArray(DIM)
        for (square in 0 until SQUARES) for (d in 0 until DIM) mean[d] += x[square * DIM + d]
        for (d in 0 until DIM) mean[d] /= SQUARES
        var y = linear(mean, 1, DIM, weights["${prefix}self_attn.sm2.weight"], weights["${prefix}self_attn.sm2.bias"], GAB_INTERMEDIATE)
        for (i in y.indices) y[i] = gelu(y[i])
        y = layerNorm(y, 1, GAB_INTERMEDIATE, weights["${prefix}self_attn.ln1.weight"], weights["${prefix}self_attn.ln1.bias"])
        y = linear(y, 1, GAB_INTERMEDIATE, weights["${prefix}self_attn.sm3.weight"], weights["${prefix}self_attn.sm3.bias"], HEADS * GAB_GEN)
        for (i in y.indices) y[i] = gelu(y[i])
        y = layerNorm(y, 1, HEADS * GAB_GEN, weights["${prefix}self_attn.ln2.weight"], weights["${prefix}self_attn.ln2.bias"])
        val shared = weights["gab_shared_weight"]
        val bias = FloatArray(HEADS * SQUARES * SQUARES)
        for (head in 0 until HEADS) {
            val generator = head * GAB_GEN
            for (pair in 0 until SQUARES * SQUARES) {
                var sum = 0f
                val row = pair * GAB_GEN
                for (i in 0 until GAB_GEN) sum += y[generator + i] * shared[row + i]
                bias[head * SQUARES * SQUARES + pair] = sum
            }
        }
        return bias
    }

    private fun policy(x: FloatArray): FloatArray {
        val from = linear(x, SQUARES, DIM, weights["proj_sq_from.weight"], null, DIM)
        val to = linear(x, SQUARES, DIM, weights["proj_sq_to.weight"], null, DIM)
        val scale = 1f / sqrt(DIM.toFloat())
        val logits = FloatArray(MaiaEncoding.MOVE_COUNT)
        for (i in 0 until SQUARES) for (j in 0 until SQUARES) {
            var dot = 0f
            for (d in 0 until DIM) dot += from[i * DIM + d] * to[j * DIM + d]
            logits[i * SQUARES + j] = dot * scale
        }
        val promotionWeights = weights["promo_bias_proj.weight"]
        val promotionScale = sqrt(DIM.toFloat())
        for (toFile in 0 until 8) {
            val target = (56 + toFile) * DIM
            for (piece in 0 until 4) {
                var dot = 0f
                for (d in 0 until DIM) dot += to[target + d] * promotionWeights[piece * DIM + d]
                val promotionBias = dot * promotionScale
                for (fromFile in 0 until 8) {
                    logits[SQUARES * SQUARES + (fromFile * 8 + toFile) * 4 + piece] =
                        logits[(48 + fromFile) * SQUARES + 56 + toFile] + promotionBias
                }
            }
        }
        return logits
    }

    private fun linear(input: FloatArray, rows: Int, inDim: Int, weight: FloatArray, bias: FloatArray?, outDim: Int): FloatArray {
        val out = FloatArray(rows * outDim)
        for (row in 0 until rows) {
            val inputOffset = row * inDim
            for (o in 0 until outDim) {
                var sum = bias?.get(o) ?: 0f
                val weightOffset = o * inDim
                for (i in 0 until inDim) sum += input[inputOffset + i] * weight[weightOffset + i]
                out[row * outDim + o] = sum
            }
        }
        return out
    }

    private fun rmsNorm(x: FloatArray, weight: FloatArray): FloatArray {
        val out = FloatArray(x.size)
        for (square in 0 until SQUARES) {
            val offset = square * DIM
            var meanSquare = 0f
            for (d in 0 until DIM) meanSquare += x[offset + d] * x[offset + d]
            val scale = 1f / sqrt(meanSquare / DIM + RMS_EPS)
            for (d in 0 until DIM) out[offset + d] = x[offset + d] * scale * weight[d]
        }
        return out
    }

    private fun layerNorm(x: FloatArray, rows: Int, dim: Int, weight: FloatArray, bias: FloatArray): FloatArray {
        val out = FloatArray(x.size)
        for (row in 0 until rows) {
            val offset = row * dim
            var mean = 0f
            for (d in 0 until dim) mean += x[offset + d]
            mean /= dim
            var variance = 0f
            for (d in 0 until dim) variance += (x[offset + d] - mean) * (x[offset + d] - mean)
            val scale = 1f / sqrt(variance / dim + LAYER_NORM_EPS)
            for (d in 0 until dim) out[offset + d] = (x[offset + d] - mean) * scale * weight[d] + bias[d]
        }
        return out
    }

    private fun gelu(value: Float): Float = 0.5f * value * (1f + erf(value / SQRT_2))

    private fun erf(value: Float): Float {
        val t = 1f / (1f + 0.3275911f * abs(value))
        val polynomial = ((((1.061405429f * t - 1.453152027f) * t + 1.421413741f) * t - 0.284496736f) * t + 0.254829592f) * t
        val result = 1f - polynomial * exp(-value * value)
        return if (value >= 0) result else -result
    }

    private companion object {
        const val SQUARES = MaiaEncoding.SQUARES
        const val DIM = 256
        const val HEADS = 8
        const val HEAD_DIM = DIM / HEADS
        const val MLP_DIM = 512
        const val BLOCKS = 8
        const val ELO_DIM = 128
        const val MAX_ELO = 5000
        const val GAB_INTERMEDIATE = 64
        const val GAB_GEN = 64
        const val BOARD_FEATURES = 12 * MaiaEncoding.HISTORY
        const val PROJECTION_IN = BOARD_FEATURES + 2 * ELO_DIM
        const val RMS_EPS = 1.1920929e-7f
        const val LAYER_NORM_EPS = 1e-5f
        const val SQRT_2 = 1.4142135f
    }
}
