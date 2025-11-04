package com.raremartial.aiac.di

import com.raremartial.aiac.presentation.chat.ChatViewModel
import org.koin.dsl.module

val viewModelModule = module {
    
    factory { ChatViewModel(repository = get()) }
}

