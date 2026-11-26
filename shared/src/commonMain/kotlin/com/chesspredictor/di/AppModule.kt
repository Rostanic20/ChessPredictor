package com.chesspredictor.di

import com.chesspredictor.data.datasources.ChessEngineDataSource
import com.chesspredictor.data.datasources.StockfishDataSource
import com.chesspredictor.data.repositories.ChessEngineRepositoryImpl
import com.chesspredictor.data.utils.MoveParserImpl
import com.chesspredictor.domain.repositories.ChessEngineRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppModule {
    private val stockfishDataSource by lazy { createStockfishDataSource() }
    private val moveParser by lazy { MoveParserImpl() }
    private val engineInitMutex = Mutex()
    private var engineInitialized = false

    val chessEngineRepository: ChessEngineRepository by lazy {
        ChessEngineRepositoryImpl(stockfishDataSource as ChessEngineDataSource, moveParser)
    }

    suspend fun initializeEngine() {
        engineInitMutex.withLock {
            if (engineInitialized) return
            (stockfishDataSource as ChessEngineDataSource).initialize()
            engineInitialized = true
        }
    }
}

expect fun createStockfishDataSource(): StockfishDataSource