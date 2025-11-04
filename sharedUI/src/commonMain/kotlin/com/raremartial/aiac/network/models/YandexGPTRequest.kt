package com.raremartial.aiac.network.models

import kotlinx.serialization.Serializable

@Serializable
data class YandexGPTRequest(
    val modelUri: String,
    val completionOptions: CompletionOptions,
    val messages: List<Message>
)

@Serializable
data class CompletionOptions(
    val stream: Boolean = false,
    val temperature: Double = 0.6,
    val maxTokens: String = "2000"
)

@Serializable
data class Message(
    val role: String,
    val text: String
)

