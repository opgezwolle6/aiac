package com.raremartial.aiac.presentation.chat

import com.raremartial.aiac.data.model.SolutionMethod
import com.raremartial.aiac.data.model.Temperature

sealed interface ChatAction {
    data class SendMessage(val text: String, val methods: Set<SolutionMethod>, val temperature: Temperature) : ChatAction
    data class UpdateInputText(val text: String) : ChatAction
    data class ToggleSolutionMethod(val method: SolutionMethod) : ChatAction
    data class SetTemperature(val temperature: Temperature) : ChatAction
    object ClearError : ChatAction
    object ClearHistory : ChatAction
}

