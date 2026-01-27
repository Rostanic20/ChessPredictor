package com.chesspredictor.web.utils

import kotlinx.browser.window
import kotlin.js.Promise

@JsName("navigator")
external val navigator: Navigator

external interface Navigator {
    val clipboard: Clipboard
}

external interface Clipboard {
    fun writeText(text: String): Promise<Unit>
    fun readText(): Promise<String>
}

fun copyToClipboard(text: String, onSuccess: () -> Unit = {}, onError: (Throwable) -> Unit = {}) {
    try {
        navigator.clipboard.writeText(text).then(
            { onSuccess() },
            { error -> onError(Exception(error.toString())) }
        )
    } catch (e: Throwable) {
        onError(e)
    }
}

fun readFromClipboard(onSuccess: (String) -> Unit, onError: (Throwable) -> Unit = {}) {
    try {
        navigator.clipboard.readText().then(
            { text -> onSuccess(text) },
            { error -> onError(Exception(error.toString())) }
        )
    } catch (e: Throwable) {
        onError(e)
    }
}

fun createBlob(content: String, type: String = "text/plain"): dynamic {
    val BlobConstructor = js("Blob")
    val options = js("({})")
    options.type = type
    return js("new Blob([content], options)")
}

fun createObjectURL(blob: dynamic): String {
    return js("URL.createObjectURL(blob)") as String
}

fun revokeObjectURL(url: String) {
    js("URL.revokeObjectURL(url)")
}