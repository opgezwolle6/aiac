package com.raremartial.aiac.data.model

import kotlinx.serialization.Serializable

/**
 * Статистика использования токенов для запроса к YandexGPT
 */
@Serializable
data class TokenUsage(
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val totalTokens: Int = 0
) {
    companion object {
        /**
         * Создает TokenUsage из Usage объекта YandexGPTResponse
         */
        fun fromUsage(usage: com.raremartial.aiac.network.models.Usage?): TokenUsage {
            if (usage == null) return TokenUsage()
            
            return TokenUsage(
                inputTokens = usage.inputTextTokens.toIntOrNull() ?: 0,
                outputTokens = usage.completionTokens.toIntOrNull() ?: 0,
                totalTokens = usage.totalTokens.toIntOrNull() ?: 0
            )
        }
    }
}

