package com.raremartial.aiac.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StructuredResponse(
    val title: String,
    val answer: String,
    val uncertainty_value: Double = 0.0
)

