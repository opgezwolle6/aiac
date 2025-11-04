package com.raremartial.aiac.network.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val message: String,
    val code: Int? = null,
    val details: List<ErrorDetail>? = null
)

@Serializable
data class ErrorDetail(
    val field: String? = null,
    val message: String? = null
)

