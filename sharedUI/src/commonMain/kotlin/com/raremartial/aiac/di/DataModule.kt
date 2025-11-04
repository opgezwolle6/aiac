package com.raremartial.aiac.di

import com.raremartial.aiac.data.repository.ChatRepository
import com.raremartial.aiac.data.repository.ChatRepositoryImpl
import org.koin.dsl.module

val dataModule = module {
    
    single<ChatRepository> {
        ChatRepositoryImpl(
            api = get(),
            folderId = YandexConfig.FOLDER_ID
        )
    }
}

