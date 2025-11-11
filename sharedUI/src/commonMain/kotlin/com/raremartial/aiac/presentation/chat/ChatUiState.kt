package com.raremartial.aiac.presentation.chat

import com.raremartial.aiac.data.model.ChatMessage
import com.raremartial.aiac.data.model.ModelComparisonResult
import com.raremartial.aiac.data.model.ModelInfo
import com.raremartial.aiac.data.model.SolutionMethod
import com.raremartial.aiac.data.model.Temperature
import com.raremartial.aiac.data.model.HuggingFaceModels

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = "",
    val selectedMethods: Set<SolutionMethod> = setOf(SolutionMethod.DIRECT),
    val selectedTemperature: Temperature = Temperature.MEDIUM,
    val firstModel: ModelInfo = if (HuggingFaceModels.POPULAR_MODELS.isNotEmpty()) HuggingFaceModels.POPULAR_MODELS[0] else ModelInfo("", ""),
    val secondModel: ModelInfo = if (HuggingFaceModels.POPULAR_MODELS.size > 1) HuggingFaceModels.POPULAR_MODELS[1] else if (HuggingFaceModels.POPULAR_MODELS.isNotEmpty()) HuggingFaceModels.POPULAR_MODELS[0] else ModelInfo("", ""),
    val thirdModel: ModelInfo = if (HuggingFaceModels.POPULAR_MODELS.size > 2) HuggingFaceModels.POPULAR_MODELS[2] else if (HuggingFaceModels.POPULAR_MODELS.size > 1) HuggingFaceModels.POPULAR_MODELS[1] else if (HuggingFaceModels.POPULAR_MODELS.isNotEmpty()) HuggingFaceModels.POPULAR_MODELS[0] else ModelInfo("", ""),
    val modelComparisonResult: ModelComparisonResult? = null,
    val isComparingModels: Boolean = false
)

