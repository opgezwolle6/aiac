package com.raremartial.aiac.data.model

import kotlinx.serialization.Serializable

/**
 * Типы способов решения задачи
 */
@Serializable
enum class SolutionMethod {
    /**
     * Способ №1: Прямой ответ от YandexGPT (текущий способ)
     */
    DIRECT,
    
    /**
     * Способ №2: Пошаговое решение с разбиением на шаги
     */
    STEP_BY_STEP,
    
    /**
     * Способ №3: Группа экспертов (каждый эксперт дает свое решение)
     */
    EXPERT_PANEL
}

