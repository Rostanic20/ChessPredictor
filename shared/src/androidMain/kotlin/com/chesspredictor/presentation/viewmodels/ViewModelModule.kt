package com.chesspredictor.presentation.viewmodels

import com.chesspredictor.di.AppModule
import com.chesspredictor.di.getAppModule

actual fun getOrCreateAppModule(): AppModule = getAppModule()