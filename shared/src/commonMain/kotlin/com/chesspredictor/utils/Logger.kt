package com.chesspredictor.utils

interface Logger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warning(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

expect class PlatformLogger() : Logger

val ChessLogger: Logger = PlatformLogger()

inline fun Logger.d(tag: String, message: () -> String) = debug(tag, message())
inline fun Logger.i(tag: String, message: () -> String) = info(tag, message())
inline fun Logger.w(tag: String, message: () -> String) = warning(tag, message())
inline fun Logger.e(tag: String, throwable: Throwable? = null, message: () -> String) = error(tag, message(), throwable)