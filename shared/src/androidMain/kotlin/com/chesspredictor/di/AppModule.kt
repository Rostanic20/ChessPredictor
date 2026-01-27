package com.chesspredictor.di

import android.content.Context
import com.chesspredictor.data.datasources.ChessEngineDataSource
import com.chesspredictor.data.datasources.StockfishDataSource

private lateinit var applicationContext: Context
private var appModuleInstance: AppModule? = null

fun initializeAndroid(context: Context) {
    applicationContext = context.applicationContext
}

fun getAppModule(): AppModule = getOrCreateAndroidAppModule()

fun getOrCreateAndroidAppModule(): AppModule {
    if (appModuleInstance == null) {
        appModuleInstance = AppModule()
    }
    return appModuleInstance!!
}

actual fun createStockfishDataSource(): StockfishDataSource {
    // Use WebView implementation
    return StockfishDataSource(applicationContext)
}