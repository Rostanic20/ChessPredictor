package com.chesspredictor.presentation.viewmodels

import com.chesspredictor.di.AppModule

private var jsAppModule: AppModule? = null

actual fun getOrCreateAppModule(): AppModule {
    if (jsAppModule == null) {
        jsAppModule = AppModule()
    }
    return jsAppModule!!
}