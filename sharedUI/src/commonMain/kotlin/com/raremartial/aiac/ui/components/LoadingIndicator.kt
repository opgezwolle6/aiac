package com.raremartial.aiac.ui.components

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    CircularProgressIndicator(
        modifier = modifier,
        strokeWidth = 3.dp
    )
}

