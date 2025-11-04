package com.raremartial.aiac.di

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

fun initKoin() {
    try {
        stopKoin()
    } catch (e: Exception) {
    }
    
    startKoin {
        modules(
            appModule,
            networkModule,
            dataModule,
            viewModelModule
        )
    }
}

object YandexConfig {
    const val API_KEY = "AQVNxWYboO9rXcL-UJaOfUw5Zac_Q-4H9lu4kqHx"
    const val FOLDER_ID = "b1gp6hvn5r1h3u5hmph1"
}