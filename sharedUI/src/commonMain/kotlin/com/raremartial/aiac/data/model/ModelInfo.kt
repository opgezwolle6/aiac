package com.raremartial.aiac.data.model

import kotlinx.serialization.Serializable

/**
 * Информация о модели для сравнения
 */
@Serializable
data class ModelInfo(
    val id: String,
    val name: String,
    val provider: String? = null,
    val description: String? = null
)

/**
 * Популярные модели HuggingFace для сравнения
 * Модели из начала, середины и конца списка популярных моделей
 */
object HuggingFaceModels {
    val POPULAR_MODELS = listOf(
        // Начало списка - крупные модели (9B параметров)
        ModelInfo(
            id = "google/gemma-2-9b-it",
            name = "Gemma 2 9B",
            description = "Google Gemma 2 9B IT - сбалансированная chat-модель"
        ),
        // Середина списка - средние модели (8B параметров)
        ModelInfo(
            id = "meta-llama/Llama-3.1-8B-Instruct",
            name = "Llama 3.1 8B",
            description = "Meta Llama 3.1 8B Instruct - мощная модель для диалогов"
        ),
        // Конец списка - компактные модели (7B параметров)
        ModelInfo(
            id = "Qwen/Qwen2.5-7B-Instruct",
            name = "Qwen 2.5 7B",
            description = "Qwen 2.5 7B Instruct - многоязычная модель"
        )
    )
}

