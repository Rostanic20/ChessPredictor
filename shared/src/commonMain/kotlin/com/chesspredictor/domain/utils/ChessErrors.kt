package com.chesspredictor.domain.utils

sealed class ChessError : Exception() {
    object InvalidMove : ChessError()
    object InvalidPosition : ChessError()
    object EngineNotReady : ChessError()
    object GameOver : ChessError()
    data class EngineError(override val message: String) : ChessError()
    data class ParseError(override val message: String) : ChessError()
}

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: ChessError) : Result<Nothing>()
    
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (ChessError) -> Unit): Result<T> {
        if (this is Error) action(error)
        return this
    }
    
    fun getOrNull(): T? = if (this is Success) data else null
}