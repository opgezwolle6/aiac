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

/**
 * Конфигурация Yandex Cloud
 * Использует значения из Secrets.kt
 */
object YandexConfig {
    const val API_KEY = Secrets.YANDEX_API_KEY
    const val FOLDER_ID = Secrets.YANDEX_FOLDER_ID
    const val HUGGINGFACE_API_KEY = Secrets.HUGGINGFACE_API_KEY
}

/**
 * Конфигурация GitHub
 * Использует значения из Secrets.kt
 */
object GitHubConfig {
    const val PERSONAL_ACCESS_TOKEN = Secrets.GITHUB_PERSONAL_ACCESS_TOKEN
}