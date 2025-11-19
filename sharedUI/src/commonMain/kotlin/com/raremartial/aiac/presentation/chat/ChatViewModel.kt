package com.raremartial.aiac.presentation.chat

import com.raremartial.aiac.data.repository.ChatRepository
import com.raremartial.aiac.network.CustomMcpApi
import com.raremartial.aiac.presentation.BaseViewModel
import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch

class ChatViewModel(
    private val repository: ChatRepository,
    private val customMcpApi: CustomMcpApi? = null
) : BaseViewModel<ChatUiState, ChatEvent, ChatAction>(
    initialState = ChatUiState()
) {

    private val logger = Logger.withTag("ChatViewModel")

    init {
        logger.d { "ChatViewModel initialized" }
        
        // Загружаем MCP инструменты при инициализации ViewModel (отдельно от потока сообщений)
        withViewModelScope {
            loadMcpTools()
        }
        
        // Отдельный scope для потока сообщений
        withViewModelScope {
            repository.messages
                .catch { e ->
                    logger.e(e) { "Error in messages flow: ${e.message}" }
                    viewState = viewState.copy(error = e.message)
                }
                .collect { messages ->
                    logger.d { "Messages updated: count=${messages.size}" }
                    viewState = viewState.copy(messages = messages)
                }
        }
    }
    
    private suspend fun loadMcpTools() {
        if (customMcpApi == null) {
            logger.d { "CustomMcpApi is not available, skipping MCP tools loading" }
            return
        }
        
        logger.d { "Loading MCP tools from http://localhost:8080..." }
        viewState = viewState.copy(isLoadingMcpTools = true, mcpToolsError = null)
        
        // Начальная задержка, чтобы дать серверу время запуститься
        // Сервер запускается в McpServerModule при инициализации Koin
        delay(2000L) // Увеличена задержка до 2 секунд
        
        var retryCount = 0
        val maxRetries = 10
        val retryDelayMs = 500L
        
        while (retryCount < maxRetries) {
            if (retryCount > 0) {
                // Задержка перед повторной попыткой (экспоненциальная)
                delay(retryDelayMs * retryCount)
            }
            
            logger.d { "Attempting to load MCP tools (attempt ${retryCount + 1}/$maxRetries)..." }
            val result = customMcpApi.getTools()
            result.fold(
                onSuccess = { tools ->
                    logger.i { "Successfully loaded ${tools.size} MCP tools: ${tools.map { it.name }}" }
                    viewState = viewState.copy(
                        customMcpTools = tools,
                        isLoadingMcpTools = false,
                        mcpToolsError = null
                    )
                    retryCount = maxRetries // Успешно, выходим из цикла
                },
                onFailure = { error ->
                    retryCount++
                    logger.w { "Failed to load MCP tools (attempt $retryCount/$maxRetries): ${error.message}" }
                    logger.w { "Error type: ${error::class.simpleName}, cause: ${error.cause?.message}" }
                    if (retryCount >= maxRetries) {
                        // После всех попыток - логируем ошибку для диагностики
                        logger.e { "Failed to load MCP tools after $maxRetries attempts. Last error: ${error.message}" }
                        logger.e { "MCP Server may not be running. Check logs for 'McpServer' tag." }
                        viewState = viewState.copy(
                            isLoadingMcpTools = false,
                            mcpToolsError = "MCP сервер недоступен. Проверьте, что сервер запущен на порту 8080."
                        )
                    }
                }
            )
            
            if (viewState.customMcpTools.isNotEmpty()) {
                logger.d { "MCP tools loaded successfully, breaking retry loop" }
                break // Успешно загрузили, выходим
            }
        }
    }

    override fun obtainEvent(viewEvent: ChatAction) {
        when (viewEvent) {
            is ChatAction.SendMessage -> {
                val text = viewEvent.text.trim()
                if (text.isNotEmpty() && viewEvent.methods.isNotEmpty()) {
                    sendMessage(text, viewEvent.methods, viewEvent.temperature)
                }
            }
            is ChatAction.SetTemperature -> {
                viewState = viewState.copy(selectedTemperature = viewEvent.temperature)
            }
            is ChatAction.UpdateInputText -> {
                viewState = viewState.copy(inputText = viewEvent.text)
            }
            is ChatAction.ToggleSolutionMethod -> {
                val currentMethods = viewState.selectedMethods.toMutableSet()
                if (currentMethods.contains(viewEvent.method)) {
                    // Не позволяем убрать последний способ
                    if (currentMethods.size > 1) {
                        currentMethods.remove(viewEvent.method)
                    }
                } else {
                    currentMethods.add(viewEvent.method)
                }
                viewState = viewState.copy(selectedMethods = currentMethods)
            }
            is ChatAction.CompareModels -> {
                val text = viewEvent.text.trim()
                if (text.isNotEmpty()) {
                    compareModels(
                        text,
                        viewEvent.firstModel,
                        viewEvent.secondModel,
                        viewEvent.thirdModel,
                        viewEvent.temperature
                    )
                }
            }
            is ChatAction.SetFirstModel -> {
                viewState = viewState.copy(firstModel = viewEvent.model)
            }
            is ChatAction.SetSecondModel -> {
                viewState = viewState.copy(secondModel = viewEvent.model)
            }
            is ChatAction.SetThirdModel -> {
                viewState = viewState.copy(thirdModel = viewEvent.model)
            }
            is ChatAction.ClearError -> {
                viewState = viewState.copy(error = null)
            }
            is ChatAction.ClearHistory -> {
                clearHistory()
            }
        }
    }

    fun handleAction(action: ChatAction) {
        obtainEvent(action)
    }

    private fun sendMessage(
        text: String,
        methods: Set<com.raremartial.aiac.data.model.SolutionMethod>,
        temperature: com.raremartial.aiac.data.model.Temperature
    ) {
        logger.d { "Sending message: text=${text.take(50)}..., methods=${methods}, temperature=${temperature.name}" }
        
        viewState = viewState.copy(
            inputText = "",
            isLoading = true,
            error = null
        )
        
        withViewModelScope {
            val result = repository.sendMessage(text, methods, temperature)
            
            viewState = viewState.copy(isLoading = false)
            
            result.fold(
                onSuccess = { message ->
                    logger.d { "Message sent successfully: id=${message.id}, contentLength=${message.content.length}" }
                    viewAction = ChatEvent.ScrollToBottom
                },
                onFailure = { e ->
                    logger.e(e) { "Failed to send message: ${e.message}" }
                    val errorMessage = e.message ?: "Unknown error occurred"
                    viewState = viewState.copy(error = errorMessage)
                    viewAction = ChatEvent.ShowError(errorMessage)
                }
            )
        }
    }

    private fun compareModels(
        text: String,
        firstModel: com.raremartial.aiac.data.model.ModelInfo,
        secondModel: com.raremartial.aiac.data.model.ModelInfo,
        thirdModel: com.raremartial.aiac.data.model.ModelInfo,
        temperature: com.raremartial.aiac.data.model.Temperature
    ) {
        logger.d {
            "Comparing models: firstModel=${firstModel.name}, secondModel=${secondModel.name}, thirdModel=${thirdModel.name}, " +
            "temperature=${temperature.name}, text=${text.take(50)}..."
        }

        viewState = viewState.copy(
            inputText = "",
            isComparingModels = true,
            isLoading = true,
            error = null,
            modelComparisonResult = null
        )

        withViewModelScope {
            val result = repository.compareModels(text, firstModel, secondModel, thirdModel, temperature)

            viewState = viewState.copy(
                isLoading = false,
                isComparingModels = false
            )

            result.fold(
                onSuccess = { comparisonResult ->
                    logger.d {
                        "Model comparison completed: " +
                        "firstModel=${comparisonResult.firstModel.modelInfo.name} (${comparisonResult.firstModel.responseTimeMs}ms), " +
                        "secondModel=${comparisonResult.secondModel.modelInfo.name} (${comparisonResult.secondModel.responseTimeMs}ms)"
                    }
                    viewState = viewState.copy(modelComparisonResult = comparisonResult)
                    viewAction = ChatEvent.ScrollToBottom
                },
                onFailure = { e ->
                    logger.e(e) { "Failed to compare models: ${e.message}" }
                    val errorMessage = e.message ?: "Unknown error occurred"
                    viewState = viewState.copy(error = errorMessage)
                    viewAction = ChatEvent.ShowError(errorMessage)
                }
            )
        }
    }

    private fun clearHistory() {
        withViewModelScope {
            repository.clearHistory()
        }
    }
}

