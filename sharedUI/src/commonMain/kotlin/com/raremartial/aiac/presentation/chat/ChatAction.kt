package com.raremartial.aiac.presentation.chat

sealed interface ChatAction {
    data class SendMessage(val text: String) : ChatAction
    data class UpdateInputText(val text: String) : ChatAction
    object ClearError : ChatAction
    object ClearHistory : ChatAction
}

