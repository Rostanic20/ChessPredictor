package com.chesspredictor.web.utils

external interface Console {
    fun log(vararg messages: Any?)
    fun error(vararg messages: Any?)
    fun warn(vararg messages: Any?)
    fun info(vararg messages: Any?)
}

external val console: Console