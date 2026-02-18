package com.chesspredictor.utils

import platform.Foundation.NSLog

actual class PlatformLogger : Logger {
    override fun debug(tag: String, message: String) {
        NSLog("D/$tag: $message")
    }

    override fun info(tag: String, message: String) {
        NSLog("I/$tag: $message")
    }

    override fun warning(tag: String, message: String) {
        NSLog("W/$tag: $message")
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        NSLog("E/$tag: $message${throwable?.let { " | ${it.message}" } ?: ""}")
    }
}
