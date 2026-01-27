package com.chesspredictor.utils

actual object TimeProvider {
    actual fun currentTimeMillis(): Long = System.currentTimeMillis()
}