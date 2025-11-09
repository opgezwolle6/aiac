package com.raremartial.aiac.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StructuredResponse(
    val title: String,
    val answer: String,
    val uncertainty_value: Double = 0.0,
    val questions: List<String> = emptyList(),
    val solutionMethod: SolutionMethod? = null
)

/**
 * Результат решения задачи одним способом
 */
@Serializable
data class SolutionResult(
    val method: SolutionMethod,
    val title: String,
    val answer: String,
    val uncertainty_value: Double = 0.0,
    val questions: List<String> = emptyList()
)

/**
 * Итоговый анализ всех способов решения
 */
@Serializable
data class ComparisonAnalysis(
    val summary: String,
    val prosAndCons: Map<String, List<String>>, // method name -> [pros, cons]
    val recommendation: String,
    val bestMethod: SolutionMethod?
)

