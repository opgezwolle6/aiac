package com.raremartial.aiac.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

actual fun createHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        configureCommon()
        
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 60000
        }
    }
}

