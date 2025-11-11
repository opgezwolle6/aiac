package com.raremartial.aiac.data.model

import kotlinx.serialization.Serializable

/**
 * Результат запроса к модели с метриками
 */
@Serializable
data class ModelResponse(
    val modelInfo: ModelInfo,
    val answer: String,
    val title: String = "",
    val responseTimeMs: Long,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val error: String? = null
)

/**
 * Результат сравнения трех моделей
 */
@Serializable
data class ModelComparisonResult(
    val firstModel: ModelResponse,
    val secondModel: ModelResponse,
    val thirdModel: ModelResponse,
    val comparison: ComparisonAnalysis
)

