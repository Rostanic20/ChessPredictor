package com.chesspredictor.presentation.viewmodels

import com.chesspredictor.di.AppModule

private val iosAppModule: AppModule by lazy { AppModule() }

actual fun getOrCreateAppModule(): AppModule = iosAppModule
