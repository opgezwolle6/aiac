package com.raremartial.aiac.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorSnackbar(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (message != null) {
        Snackbar(
            modifier = modifier.padding(16.dp),
            action = {
                TextButton(onClick = onDismiss) {
                    Text("Закрыть")
                }
            }
        ) {
            Text(message)
        }
    }
}

