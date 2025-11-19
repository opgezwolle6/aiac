package com.raremartial.aiac.di

import com.raremartial.aiac.database.ChatDatabase
import com.raremartial.aiac.database.ChatMessageDao
import com.raremartial.aiac.database.DatabaseDriverFactory
import com.raremartial.aiac.data.repository.ChatRepository
import com.raremartial.aiac.data.repository.ChatRepositoryImpl
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val dataModule = module {
    
    single<DatabaseDriverFactory> {
        DatabaseDriverFactory()
    }
    
    single<ChatDatabase> {
        val driverFactory = get<DatabaseDriverFactory>()
        ChatDatabase(driverFactory.createDriver())
    }
    
    single<ChatMessageDao> {
        val database = get<ChatDatabase>()
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
        ChatMessageDao(database, json)
    }
    
    single<ChatRepository> {
        ChatRepositoryImpl(
            api = get(),
            huggingFaceApi = get(),
            folderId = YandexConfig.FOLDER_ID,
            messageDao = get(),
            customMcpApi = try {
                get<com.raremartial.aiac.network.CustomMcpApi>()
            } catch (e: Exception) {
                null // CustomMcpApi может быть недоступен на некоторых платформах
            }
        )
    }
}

