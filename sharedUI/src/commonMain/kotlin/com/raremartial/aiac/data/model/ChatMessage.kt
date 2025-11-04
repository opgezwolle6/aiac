package com.raremartial.aiac.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@OptIn(kotlin.time.ExperimentalTime::class)
@Serializable
data class ChatMessage(
    val id: String,
    val content: String,
    val role: MessageRole,
    val timestamp: Instant,
    val isPending: Boolean = false
)

enum class MessageRole {
    USER,
    ASSISTANT
}

