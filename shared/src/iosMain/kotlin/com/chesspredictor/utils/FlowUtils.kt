package com.chesspredictor.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class Closeable(private val job: Job) {
    fun close() {
        job.cancel()
    }
}

fun <T> StateFlow<T>.observe(onChange: (T) -> Unit): Closeable {
    val supervisorJob = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.Main + supervisorJob)
    scope.launch {
        collect { value -> onChange(value) }
    }
    return Closeable(supervisorJob)
}
