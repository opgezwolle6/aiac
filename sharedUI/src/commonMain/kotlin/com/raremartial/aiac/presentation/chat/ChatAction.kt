package com.raremartial.aiac.presentation.chat

import com.raremartial.aiac.data.model.SolutionMethod

sealed interface ChatAction {
    data class SendMessage(val text: String, val methods: Set<SolutionMethod>) : ChatAction
    data class UpdateInputText(val text: String) : ChatAction
    data class ToggleSolutionMethod(val method: SolutionMethod) : ChatAction
    object ClearError : ChatAction
    object ClearHistory : ChatAction
}

