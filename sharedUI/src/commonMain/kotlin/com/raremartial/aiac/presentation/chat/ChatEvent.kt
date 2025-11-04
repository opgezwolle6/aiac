package com.raremartial.aiac.presentation.chat

sealed interface ChatEvent {
    data class ShowError(val message: String) : ChatEvent
    object ScrollToBottom : ChatEvent
}

