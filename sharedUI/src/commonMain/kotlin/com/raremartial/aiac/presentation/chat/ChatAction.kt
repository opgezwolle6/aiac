package com.raremartial.aiac.presentation.chat

import com.raremartial.aiac.data.model.ModelInfo
import com.raremartial.aiac.data.model.SolutionMethod
import com.raremartial.aiac.data.model.Temperature

sealed interface ChatAction {
    data class SendMessage(val text: String, val methods: Set<SolutionMethod>, val temperature: Temperature) : ChatAction
    data class UpdateInputText(val text: String) : ChatAction
    data class ToggleSolutionMethod(val method: SolutionMethod) : ChatAction
    data class SetTemperature(val temperature: Temperature) : ChatAction
    data class CompareModels(
        val text: String,
        val firstModel: ModelInfo,
        val secondModel: ModelInfo,
        val thirdModel: ModelInfo,
        val temperature: Temperature
    ) : ChatAction
    data class SetFirstModel(val model: ModelInfo) : ChatAction
    data class SetSecondModel(val model: ModelInfo) : ChatAction
    data class SetThirdModel(val model: ModelInfo) : ChatAction
    object ClearError : ChatAction
    object ClearHistory : ChatAction
}

