package com.raremartial.aiac.presentation.chat

import com.raremartial.aiac.data.model.ChatMessage
import com.raremartial.aiac.data.model.SolutionMethod
import com.raremartial.aiac.data.model.Temperature

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = "",
    val selectedMethods: Set<SolutionMethod> = setOf(SolutionMethod.DIRECT),
    val selectedTemperature: Temperature = Temperature.MEDIUM
)

