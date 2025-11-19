package com.raremartial.aiac.di

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

expect fun initKoin()

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

/**
 * Конфигурация Яндекс.Трекера
 * Использует значения из Secrets.kt
 */
object YandexTrackerConfig {
    const val IAM_TOKEN = Secrets.YANDEX_TRACKER_IAM_TOKEN
    const val ORG_ID = Secrets.YANDEX_TRACKER_ORG_ID
    const val DEFAULT_QUEUE = Secrets.YANDEX_TRACKER_DEFAULT_QUEUE
}

