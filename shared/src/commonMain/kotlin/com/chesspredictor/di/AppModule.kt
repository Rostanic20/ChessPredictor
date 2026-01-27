package com.chesspredictor.di

import com.chesspredictor.data.datasources.ChessEngineDataSource
import com.chesspredictor.data.datasources.StockfishDataSource
import com.chesspredictor.data.repositories.ChessEngineRepositoryImpl
import com.chesspredictor.data.utils.MoveParserImpl
import com.chesspredictor.domain.repositories.ChessEngineRepository
import com.chesspredictor.domain.usecases.GetBestMoveUseCase

class AppModule {
    private val stockfishDataSource by lazy { createStockfishDataSource() }
    private val moveParser by lazy { MoveParserImpl() }
    
    val chessEngineRepository: ChessEngineRepository by lazy {
        ChessEngineRepositoryImpl(stockfishDataSource as ChessEngineDataSource, moveParser)
    }
    
    val getBestMoveUseCase by lazy {
        GetBestMoveUseCase(chessEngineRepository)
    }
    
    suspend fun initializeEngine() {
        (stockfishDataSource as ChessEngineDataSource).initialize()
    }
}

expect fun createStockfishDataSource(): StockfishDataSource