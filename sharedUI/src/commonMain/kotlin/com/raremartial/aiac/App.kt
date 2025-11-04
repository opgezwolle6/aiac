package com.raremartial.aiac

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.raremartial.aiac.presentation.chat.ChatViewModel
import com.raremartial.aiac.theme.AppTheme
import com.raremartial.aiac.ui.chat.ChatScreen
import org.koin.compose.koinInject

@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) {
    AppTheme(onThemeChanged) {
        val viewModel: ChatViewModel = koinInject()
        ChatScreen(viewModel = viewModel)
    }
}
