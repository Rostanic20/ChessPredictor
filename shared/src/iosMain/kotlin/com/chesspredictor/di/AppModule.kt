package com.chesspredictor.di

import com.chesspredictor.data.datasources.StockfishDataSource

actual fun createStockfishDataSource(): StockfishDataSource {
    return StockfishDataSource()
}
