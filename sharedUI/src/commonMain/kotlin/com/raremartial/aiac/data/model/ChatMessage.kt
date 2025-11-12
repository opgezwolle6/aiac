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
    val isPending: Boolean = false,
    val structuredData: StructuredResponse? = null,
    val solutionResults: List<SolutionResult> = emptyList(),
    val comparisonAnalysis: ComparisonAnalysis? = null,
    val selectedMethods: List<SolutionMethod> = emptyList(),
    val tokenUsage: TokenUsage? = null
)

enum class MessageRole {
    USER,
    ASSISTANT
}

