package com.chesspredictor.utils

actual object TimeProvider {
    actual fun currentTimeMillis(): Long {
        val timeMs = js("Date.now()")
        return timeMs.unsafeCast<Double>().toLong()
    }
}