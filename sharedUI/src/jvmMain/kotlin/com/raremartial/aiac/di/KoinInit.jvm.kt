package com.raremartial.aiac.di

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

actual fun initKoin() {
    try {
        stopKoin()
    } catch (e: Exception) {
    }
    
    startKoin {
        modules(
            appModule,
            networkModule,
            dataModule,
            viewModelModule,
            mcpServerModule // MCP сервер только для JVM
        )
    }
}

