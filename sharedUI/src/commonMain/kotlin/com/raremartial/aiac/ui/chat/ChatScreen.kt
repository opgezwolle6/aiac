package com.raremartial.aiac.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raremartial.aiac.presentation.chat.ChatAction
import com.raremartial.aiac.presentation.chat.ChatEvent
import com.raremartial.aiac.presentation.chat.ChatViewModel
import com.raremartial.aiac.theme.LocalThemeIsDark
import com.raremartial.aiac.ui.components.ChatInput
import com.raremartial.aiac.ui.components.ChatMessageItem
import com.raremartial.aiac.ui.components.ErrorSnackbar
import com.raremartial.aiac.ui.components.TokenStatisticsCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.viewStates().collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isDark by LocalThemeIsDark.current

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            coroutineScope.launch {
                // Скроллим к последнему элементу
                val itemCount = state.messages.size
                val targetIndex = itemCount - 1
                if (targetIndex >= 0 && itemCount > 0) {
                    listState.animateScrollToItem(
                        index = targetIndex,
                        scrollOffset = 0
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.viewActions().collect { event ->
            when (event) {
                is ChatEvent.ScrollToBottom -> {
                    coroutineScope.launch {
                        val targetIndex = state.messages.size - 1
                        if (targetIndex >= 0 && state.messages.isNotEmpty()) {
                            listState.animateScrollToItem(
                                index = targetIndex,
                                scrollOffset = 0
                            )
                        }
                    }
                    viewModel.clearAction()
                }

                is ChatEvent.ShowError -> {
                    viewModel.clearAction()
                }

                null -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "YandexGPT Чат",
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                actions = {
                    // Кнопка очистки истории
                    IconButton(
                        onClick = {
                            viewModel.handleAction(ChatAction.ClearHistory)
                        },
                        enabled = state.messages.isNotEmpty() && !state.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Очистить историю",
                            tint = if (state.messages.isNotEmpty() && !state.isLoading) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }
                    
                    // Кнопка переключения темы
                    val themeState = LocalThemeIsDark.current
                    IconButton(
                        onClick = {
                            themeState.value = !themeState.value
                        }
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (isDark) "Переключить на светлую тему" else "Переключить на тёмную тему",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column {
            // Статистика токенов
            TokenStatisticsCard(messages = state.messages)
            
            LazyColumn(
                state = listState,
                    modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(
                    items = state.messages,
                    key = { it.id }
                ) { message ->
                    ChatMessageItem(message = message)
                }
                
                if (state.messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                        text = "Начните диалог с YandexGPT",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "Введите сообщение и нажмите Enter или отправьте",
                                        style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                                }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surface,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp
                        )
                    )
            ) {
                ChatInput(
                    text = state.inputText,
                    onTextChange = { viewModel.handleAction(ChatAction.UpdateInputText(it)) },
                    onSendClick = {
                            viewModel.handleAction(
                                ChatAction.SendMessage(
                                    state.inputText,
                                    state.selectedMethods,
                                    state.selectedTemperature
                                )
                            )
                    },
                    enabled = !state.isLoading
                )
                }
            }
        }

        if (state.error != null) {
            ErrorSnackbar(
                message = state.error,
                onDismiss = { viewModel.handleAction(ChatAction.ClearError) }
            )
        }
    }
}

