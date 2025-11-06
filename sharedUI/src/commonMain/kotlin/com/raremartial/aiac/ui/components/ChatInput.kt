package com.raremartial.aiac.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val isButtonEnabled = enabled && text.isNotBlank()
    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonEnabled) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_scale"
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp, max = 120.dp),
            enabled = enabled,
            placeholder = { 
                Text(
                    "Введите сообщение...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ) 
            },
            shape = RoundedCornerShape(28.dp),
            singleLine = false,
            maxLines = 4,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Default
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = {
                    // На мобильных устройствах кнопка "Готово" отправляет сообщение
                    if (isButtonEnabled) {
                        onSendClick()
                        keyboardController?.hide()
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        
        AnimatedVisibility(
            visible = isButtonEnabled,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            FloatingActionButton(
                onClick = {
                    onSendClick()
                    keyboardController?.hide()
                },
                modifier = Modifier
                    .size(56.dp)
                    .scale(buttonScale),
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Text(
                    text = "→",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

