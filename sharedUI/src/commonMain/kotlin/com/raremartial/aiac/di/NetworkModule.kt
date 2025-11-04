package com.raremartial.aiac.di

import com.raremartial.aiac.network.YandexGPTApi
import com.raremartial.aiac.network.YandexGPTApiImpl
import io.ktor.client.HttpClient
import org.koin.dsl.module

val networkModule = module {
    
    single<HttpClient> {
        createHttpClient()
    }
    
    single<YandexGPTApi> {
        YandexGPTApiImpl(
            httpClient = get(),
            apiKey = YandexConfig.API_KEY,
            folderId = YandexConfig.FOLDER_ID
        )
    }
}

