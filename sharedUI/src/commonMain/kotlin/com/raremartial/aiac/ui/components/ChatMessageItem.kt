package com.raremartial.aiac.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.raremartial.aiac.data.model.ChatMessage
import com.raremartial.aiac.data.model.MessageRole
import com.raremartial.aiac.data.model.SolutionMethod

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    
    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    AnimatedContent(
        targetState = message,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) + 
            slideInVertically(
                animationSpec = tween(300),
                initialOffsetY = { 20 }
            ) togetherWith fadeOut(animationSpec = tween(100))
        },
        label = "message_animation"
    ) { currentMessage ->
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            val maxMessageWidth = if (maxWidth > 600.dp) {
                (maxWidth * 0.6f).coerceAtMost(800.dp)
            } else {
                300.dp
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                        .widthIn(max = maxMessageWidth)
                        .animateContentSize(
                            animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                            )
                        )
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                if (currentMessage.isPending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.5.dp,
                        color = contentColor
                    )
                } else {
                    when {
                        // Если есть множественные способы решения
                        currentMessage.solutionResults.isNotEmpty() && !isUser -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Отображаем каждый способ решения
                                currentMessage.solutionResults.forEach { result ->
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = getMethodDisplayName(result.method),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = contentColor.copy(alpha = 0.9f)
                                        )
                                        Text(
                                            text = result.answer,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = contentColor,
                                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                                        )
                                    }
                                }
                                
                                // Отображаем итоговый анализ, если есть
                                currentMessage.comparisonAnalysis?.let { analysis ->
                                    Divider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = contentColor.copy(alpha = 0.3f)
                                    )
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Итоговый анализ",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = contentColor.copy(alpha = 0.9f)
                                        )
                                        Text(
                                            text = analysis.summary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = contentColor,
                                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                                        )
                                        if (analysis.recommendation.isNotBlank()) {
                                            Text(
                                                text = "Рекомендация: ${analysis.recommendation}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = contentColor.copy(alpha = 0.8f),
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                                
                                // Статистика токенов для множественных способов
                                currentMessage.tokenUsage?.let { usage ->
                                    if (usage.totalTokens > 0) {
                                        Divider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = contentColor.copy(alpha = 0.3f)
                                        )
                                        Text(
                                            text = "Токены: входные ${usage.inputTokens}, выходные ${usage.outputTokens}, всего ${usage.totalTokens}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = contentColor.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                        // Если есть структурированные данные (один способ)
                        currentMessage.structuredData != null && !isUser -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (currentMessage.structuredData.title.isNotBlank()) {
                                    Text(
                                        text = currentMessage.structuredData.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = contentColor,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                Text(
                                    text = currentMessage.structuredData.answer,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = contentColor,
                                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                                )
                                // Статистика токенов
                                currentMessage.tokenUsage?.let { usage ->
                                    if (usage.totalTokens > 0) {
                                        Text(
                                            text = "Токены: входные ${usage.inputTokens}, выходные ${usage.outputTokens}, всего ${usage.totalTokens}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = contentColor.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        // Обычный контент
                        else -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Показываем метку для summary сообщений
                                if (currentMessage.isSummary) {
                                    Text(
                                        text = "📋 Резюме предыдущих сообщений",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = contentColor.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                Text(
                                    text = currentMessage.content,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = contentColor,
                                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                                )
                            }
                }
            }
        }
                }
            }
        }
    }
}

private fun getMethodDisplayName(method: SolutionMethod): String {
    return when (method) {
        SolutionMethod.DIRECT -> "Способ 1: Прямой ответ"
        SolutionMethod.STEP_BY_STEP -> "Способ 2: Пошаговое решение"
        SolutionMethod.EXPERT_PANEL -> "Способ 3: Группа экспертов"
    }
}

