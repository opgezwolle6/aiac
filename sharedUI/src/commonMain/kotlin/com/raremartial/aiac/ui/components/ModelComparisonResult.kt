package com.raremartial.aiac.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.raremartial.aiac.data.model.ComparisonAnalysis
import com.raremartial.aiac.data.model.ModelComparisonResult

@Composable
fun ModelComparisonResultCard(
    comparisonResult: ModelComparisonResult,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Заголовок
        Text(
            text = "Сравнение моделей",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Метрики сравнения
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Первая модель
            ModelResultCard(
                modelName = comparisonResult.firstModel.modelInfo.name,
                responseTime = comparisonResult.firstModel.responseTimeMs,
                inputTokens = comparisonResult.firstModel.inputTokens,
                outputTokens = comparisonResult.firstModel.outputTokens,
                totalTokens = comparisonResult.firstModel.totalTokens,
                answer = comparisonResult.firstModel.answer,
                error = comparisonResult.firstModel.error,
                modifier = Modifier.weight(1f)
            )

            // Вторая модель
            ModelResultCard(
                modelName = comparisonResult.secondModel.modelInfo.name,
                responseTime = comparisonResult.secondModel.responseTimeMs,
                inputTokens = comparisonResult.secondModel.inputTokens,
                outputTokens = comparisonResult.secondModel.outputTokens,
                totalTokens = comparisonResult.secondModel.totalTokens,
                answer = comparisonResult.secondModel.answer,
                error = comparisonResult.secondModel.error,
                modifier = Modifier.weight(1f)
            )

            // Третья модель
            ModelResultCard(
                modelName = comparisonResult.thirdModel.modelInfo.name,
                responseTime = comparisonResult.thirdModel.responseTimeMs,
                inputTokens = comparisonResult.thirdModel.inputTokens,
                outputTokens = comparisonResult.thirdModel.outputTokens,
                totalTokens = comparisonResult.thirdModel.totalTokens,
                answer = comparisonResult.thirdModel.answer,
                error = comparisonResult.thirdModel.error,
                modifier = Modifier.weight(1f)
            )
        }

        // Анализ сравнения
        ComparisonAnalysisCard(
            comparison = comparisonResult.comparison,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ModelResultCard(
    modelName: String,
    responseTime: Long,
    inputTokens: Int,
    outputTokens: Int,
    totalTokens: Int,
    answer: String,
    error: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = modelName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (error != null) {
                Text(
                    text = "Ошибка: $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                // Метрики
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MetricRow("Время ответа", "${responseTime}мс")
                    MetricRow("Входные токены", inputTokens.toString())
                    MetricRow("Выходные токены", outputTokens.toString())
                    MetricRow("Всего токенов", totalTokens.toString())
                }

                // Ответ
                if (answer.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Ответ:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = answer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 10,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ComparisonAnalysisCard(
    comparison: ComparisonAnalysis,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Анализ сравнения",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Резюме
            if (comparison.summary.isNotEmpty()) {
                Text(
                    text = comparison.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Преимущества и недостатки для каждой модели
            if (comparison.prosAndCons.isNotEmpty()) {
                comparison.prosAndCons.forEach { (modelName, prosAndCons) ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = modelName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        prosAndCons.forEach { item ->
                            Text(
                                text = "• $item",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Рекомендация
            if (comparison.recommendation.isNotEmpty()) {
                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                )
                Text(
                    text = "Рекомендация:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = comparison.recommendation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

