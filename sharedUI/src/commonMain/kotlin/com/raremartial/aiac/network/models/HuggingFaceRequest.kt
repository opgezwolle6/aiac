package com.raremartial.aiac.network.models

import kotlinx.serialization.Serializable

/**
 * OpenAI-совместимый запрос для HuggingFace Inference Providers
 * Документация: https://huggingface.co/docs/inference-providers/index
 */
@Serializable
data class HuggingFaceRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 2000,
    val stream: Boolean = false
)

@Serializable
data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

