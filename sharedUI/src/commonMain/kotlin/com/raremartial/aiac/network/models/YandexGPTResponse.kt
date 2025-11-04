package com.raremartial.aiac.network.models

import kotlinx.serialization.Serializable

@Serializable
data class YandexGPTResponse(
    val result: Result? = null,
    val error: ApiError? = null
)

@Serializable
data class Result(
    val alternatives: List<Alternative>,
    val usage: Usage? = null
)

@Serializable
data class Alternative(
    val message: ResponseMessage,
    val status: String = "ALTERNATIVE_STATUS_UNSPECIFIED"
)

@Serializable
data class ResponseMessage(
    val role: String,
    val text: String
)

@Serializable
data class Usage(
    val inputTextTokens: String,
    val completionTokens: String,
    val totalTokens: String
)

