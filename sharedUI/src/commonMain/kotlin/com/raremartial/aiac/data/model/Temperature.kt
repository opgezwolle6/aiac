package com.raremartial.aiac.data.model

import kotlinx.serialization.Serializable

/**
 * Типы температуры для генерации ответов
 */
@Serializable
enum class Temperature {
    /**
     * Низкая температура (0.0) - детерминированные, точные ответы
     */
    LOW,
    
    /**
     * Средняя температура (0.5) - сбалансированные ответы
     */
    MEDIUM,
    
    /**
     * Высокая температура (1.0) - креативные, разнообразные ответы
     */
    HIGH
}

