package com.raremartial.aiac.network.models

import kotlinx.serialization.Serializable

/**
 * OpenAI-совместимый ответ от HuggingFace Inference Providers
 */
@Serializable
data class HuggingFaceResponse(
    val id: String? = null,
    @kotlinx.serialization.SerialName("object")
    val objectType: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val choices: List<Choice> = emptyList(),
    val usage: HuggingFaceUsage? = null,
    val error: HuggingFaceError? = null
)

@Serializable
data class Choice(
    val index: Int = 0,
    val message: ChatMessageResponse? = null,
    val finish_reason: String? = null
)

@Serializable
data class ChatMessageResponse(
    val role: String,
    val content: String
)

@Serializable
data class HuggingFaceUsage(
    val prompt_tokens: Int = 0,
    val completion_tokens: Int = 0,
    val total_tokens: Int = 0
)

@Serializable
data class HuggingFaceError(
    val message: String,
    val type: String? = null,
    val code: String? = null
)

