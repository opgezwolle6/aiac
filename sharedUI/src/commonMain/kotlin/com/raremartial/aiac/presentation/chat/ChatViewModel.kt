package com.raremartial.aiac.presentation.chat

import com.raremartial.aiac.data.repository.ChatRepository
import com.raremartial.aiac.presentation.BaseViewModel
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.catch

class ChatViewModel(
    private val repository: ChatRepository
) : BaseViewModel<ChatUiState, ChatEvent, ChatAction>(
    initialState = ChatUiState()
) {

    private val logger = Logger.withTag("ChatViewModel")

    init {
        logger.d { "ChatViewModel initialized" }
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

    private fun clearHistory() {
        withViewModelScope {
            repository.clearHistory()
        }
    }
}

