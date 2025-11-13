package com.raremartial.aiac.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raremartial.aiac.data.model.ChatMessage
import com.raremartial.aiac.data.model.TokenUsage

/**
 * Компонент для отображения статистики использования токенов
 */
@Composable
fun TokenStatisticsCard(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    val statistics = calculateTokenStatistics(messages)
    
    if (statistics.totalTokens == 0 && statistics.summaryCount == 0) {
        // Не показываем карточку, если нет статистики
        return
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Заголовок
            Text(
                text = "📊 Статистика токенов",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            
            // Основная статистика
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Входные токены
                StatisticItem(
                    label = "Входные",
                    value = statistics.inputTokens,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Выходные токены
                StatisticItem(
                    label = "Выходные",
                    value = statistics.outputTokens,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                // Всего токенов
                StatisticItem(
                    label = "Всего",
                    value = statistics.totalTokens,
                    color = MaterialTheme.colorScheme.tertiary,
                    isTotal = true
                )
            }
            
            // Информация о компрессии
            if (statistics.summaryCount > 0) {
                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📋 Сжатий истории:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "${statistics.summaryCount}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Индикатор эффективности компрессии
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Экономия токенов:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "~${formatNumber(statistics.estimatedSavedTokens)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: Int,
    color: androidx.compose.ui.graphics.Color,
    isTotal: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = formatNumber(value),
            style = if (isTotal) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * Вычисляет общую статистику токенов из списка сообщений
 */
private fun calculateTokenStatistics(messages: List<ChatMessage>): TokenStatistics {
    val totalUsage = messages
        .filter { !it.isPending }
        .mapNotNull { it.tokenUsage }
        .fold(TokenUsage()) { acc, usage ->
            TokenUsage(
                inputTokens = acc.inputTokens + usage.inputTokens,
                outputTokens = acc.outputTokens + usage.outputTokens,
                totalTokens = acc.totalTokens + usage.totalTokens
            )
        }
    
    val summaryCount = messages.count { it.isSummary }
    
    // Приблизительная оценка сохраненных токенов
    // Предполагаем, что каждое summary заменяет ~10 сообщений, 
    // и каждое сообщение в среднем ~500 токенов
    val estimatedSavedTokens = summaryCount * 10 * 500
    
    return TokenStatistics(
        inputTokens = totalUsage.inputTokens,
        outputTokens = totalUsage.outputTokens,
        totalTokens = totalUsage.totalTokens,
        summaryCount = summaryCount,
        estimatedSavedTokens = estimatedSavedTokens
    )
}

private data class TokenStatistics(
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val summaryCount: Int,
    val estimatedSavedTokens: Int
)

private fun formatNumber(number: Int): String {
    return when {
        number >= 1_000_000 -> {
            val millions = number / 1_000_000.0
            val formatted = if (millions % 1.0 == 0.0) {
                millions.toInt().toString()
            } else {
                String.format("%.1f", millions)
            }
            "${formatted}M"
        }
        number >= 1_000 -> {
            val thousands = number / 1_000.0
            val formatted = if (thousands % 1.0 == 0.0) {
                thousands.toInt().toString()
            } else {
                String.format("%.1f", thousands)
            }
            "${formatted}K"
        }
        else -> number.toString()
    }
}

