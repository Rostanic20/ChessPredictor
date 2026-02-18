package com.chesspredictor.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class ChessError {
    abstract val message: String
    abstract val isRecoverable: Boolean
    
    data class SaveGameError(
        override val message: String = "Failed to save game state",
        override val isRecoverable: Boolean = true
    ) : ChessError()
    
    data class LoadGameError(
        override val message: String = "Failed to load saved game",
        override val isRecoverable: Boolean = true
    ) : ChessError()
    
    data class EngineInitError(
        override val message: String = "Failed to initialize chess engine",
        override val isRecoverable: Boolean = false
    ) : ChessError()
    
    data class MoveValidationError(
        override val message: String = "Invalid move",
        override val isRecoverable: Boolean = true
    ) : ChessError()
    
    data class NetworkError(
        override val message: String = "Network connection error",
        override val isRecoverable: Boolean = true
    ) : ChessError()
    
    data class UnknownError(
        override val message: String = "An unexpected error occurred",
        val cause: Throwable? = null,
        override val isRecoverable: Boolean = true
    ) : ChessError()
    
    data class ImportGameError(
        override val message: String = "Failed to import game",
        override val isRecoverable: Boolean = true
    ) : ChessError()
    
    data class ExportGameError(
        override val message: String = "Failed to export game",
        override val isRecoverable: Boolean = true
    ) : ChessError()
}

class ErrorStateManager {
    private val _currentError = MutableStateFlow<ChessError?>(null)
    val currentError: StateFlow<ChessError?> = _currentError.asStateFlow()
    
    private val _errorHistory = MutableStateFlow<List<ChessError>>(emptyList())
    val errorHistory: StateFlow<List<ChessError>> = _errorHistory.asStateFlow()
    
    fun reportError(error: ChessError) {
        _currentError.value = error
        _errorHistory.value = _errorHistory.value + error
    }
    
    fun clearError() {
        _currentError.value = null
    }
    
    fun clearHistory() {
        _errorHistory.value = emptyList()
    }
}

inline fun <T> ErrorStateManager.safeExecute(
    errorFactory: (Throwable) -> ChessError = { ChessError.UnknownError(cause = it) },
    block: () -> T
): T? {
    return try {
        block()
    } catch (e: Exception) {
        reportError(errorFactory(e))
        null
    }
}

suspend inline fun <T> ErrorStateManager.safeExecuteSuspend(
    errorFactory: (Throwable) -> ChessError = { ChessError.UnknownError(cause = it) },
    block: () -> T
): T? {
    return try {
        block()
    } catch (e: Exception) {
        reportError(errorFactory(e))
        null
    }
}