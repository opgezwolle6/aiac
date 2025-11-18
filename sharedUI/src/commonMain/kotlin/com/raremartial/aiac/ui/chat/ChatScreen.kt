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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raremartial.aiac.network.GitHubMcpApi
import com.raremartial.aiac.network.models.McpTool
import com.raremartial.aiac.presentation.chat.ChatAction
import com.raremartial.aiac.presentation.chat.ChatEvent
import com.raremartial.aiac.presentation.chat.ChatViewModel
import com.raremartial.aiac.theme.LocalThemeIsDark
import com.raremartial.aiac.ui.components.ChatInput
import com.raremartial.aiac.ui.components.ChatMessageItem
import com.raremartial.aiac.ui.components.ErrorSnackbar
import com.raremartial.aiac.ui.components.TokenStatisticsCard
import org.koin.compose.koinInject
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
    
    // MCP API для получения инструментов GitHub MCP Server
    val mcpApi: GitHubMcpApi = koinInject()
    var mcpTools by remember { mutableStateOf<List<McpTool>>(emptyList()) }
    var isLoadingTools by remember { mutableStateOf(false) }
    var toolsError by remember { mutableStateOf<String?>(null) }
    
    // Загружаем инструменты при открытии экрана
    LaunchedEffect(Unit) {
        isLoadingTools = true
        toolsError = null
        mcpApi.getTools()
            .fold(
                onSuccess = { tools ->
                    mcpTools = tools
                    isLoadingTools = false
                },
                onFailure = { error ->
                    toolsError = error.message
                    isLoadingTools = false
                }
            )
    }

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
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                // Включаем прокрутку даже для одного элемента
                userScrollEnabled = true
            ) {
                // Статистика токенов как элемент списка
                item {
                    TokenStatisticsCard(messages = state.messages)
                }
                
                // Карточка с инструментами MCP как элемент списка
                item {
                    if (isLoadingTools) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Загрузка инструментов MCP...",
                                    modifier = Modifier.padding(start = 12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (toolsError != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "Ошибка загрузки инструментов: $toolsError",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    } else if (mcpTools.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Инструменты GitHub MCP Server (${mcpTools.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                mcpTools.forEachIndexed { index, tool ->
                                    Column(
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${index + 1}. ${tool.name}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!tool.description.isNullOrBlank()) {
                                            Text(
                                                text = tool.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                            )
                                        }
                                        if (index < mcpTools.size - 1) {
                                            androidx.compose.foundation.layout.Spacer(
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "Инструменты MCP не найдены",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // Сообщения чата
                items(
                    items = state.messages,
                    key = { it.id }
                ) { message ->
                    ChatMessageItem(
                        message = message,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Пустое состояние
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

