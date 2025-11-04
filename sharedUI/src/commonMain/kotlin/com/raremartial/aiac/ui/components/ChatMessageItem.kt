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

