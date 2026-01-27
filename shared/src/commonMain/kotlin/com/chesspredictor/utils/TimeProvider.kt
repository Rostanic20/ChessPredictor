package com.chesspredictor.utils

expect object TimeProvider {
    fun currentTimeMillis(): Long
}