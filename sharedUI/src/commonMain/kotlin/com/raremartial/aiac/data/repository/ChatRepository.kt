package com.raremartial.aiac.data.repository

import com.raremartial.aiac.data.model.ChatMessage
import com.raremartial.aiac.network.YandexGPTApi
import com.raremartial.aiac.network.models.CompletionOptions
import com.raremartial.aiac.network.models.Message
import com.raremartial.aiac.network.models.YandexGPTRequest
import com.raremartial.aiac.data.mapper.ChatMessageMapper
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.raremartial.aiac.util.currentTime
import kotlin.time.ExperimentalTime

interface ChatRepository {
    val messages: Flow<List<ChatMessage>>
    
    suspend fun sendMessage(text: String): Result<ChatMessage>
    
    suspend fun clearHistory()
}

@OptIn(ExperimentalTime::class)
class ChatRepositoryImpl(
    private val api: YandexGPTApi,
    private val folderId: String
) : ChatRepository {

    private val logger = Logger.withTag("ChatRepository")
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()

    override suspend fun sendMessage(text: String): Result<ChatMessage> {
        val userMessage = ChatMessage(
            id = ChatMessageMapper.generateId(),
            content = text,
            role = com.raremartial.aiac.data.model.MessageRole.USER,
            timestamp = currentTime()
        )
        
        _messages.value = _messages.value + userMessage
        
        val pendingMessage = ChatMessage(
            id = ChatMessageMapper.generateId(),
            content = "",
            role = com.raremartial.aiac.data.model.MessageRole.ASSISTANT,
            timestamp = currentTime(),
            isPending = true
        )
        _messages.value = _messages.value + pendingMessage
        
        val networkMessages = _messages.value
            .filter { !it.isPending }
            .map { ChatMessageMapper.toNetwork(it) }
        
        val request = YandexGPTRequest(
            modelUri = "",
            completionOptions = CompletionOptions(),
            messages = networkMessages
        )
        
        logger.d { "Sending message to API: text=${text.take(50)}..., messagesCount=${networkMessages.size}" }
        
        val result = api.sendMessage(request)
        
        return result.fold(
            onSuccess = { response ->
                logger.d { "API response received: result=${response.result != null}, alternativesCount=${response.result?.alternatives?.size ?: 0}" }
                
                val responseMessage = response.result?.alternatives?.firstOrNull()?.message
                
                if (responseMessage == null) {
                    logger.e { "Empty response from API: result=${response.result}, alternatives=${response.result?.alternatives}" }
                    _messages.value = _messages.value.filter { it.id != pendingMessage.id }
                    return Result.failure(Exception("Empty response from YandexGPT API"))
                }
                
                logger.d { "Response message extracted: role=${responseMessage.role}, textLength=${responseMessage.text.length}" }
                
                val assistantMessage = ChatMessageMapper.toDomain(
                    responseMessage,
                    timestamp = currentTime()
                )
                
                _messages.value = _messages.value
                    .filter { it.id != pendingMessage.id } + assistantMessage
                
                logger.d { "Message successfully processed and added to chat" }
                Result.success(assistantMessage)
            },
            onFailure = { exception ->
                logger.e(exception) { "Failed to send message: ${exception.message}" }
                _messages.value = _messages.value.filter { it.id != pendingMessage.id }
                Result.failure(exception)
            }
        )
    }

    override suspend fun clearHistory() {
        _messages.value = emptyList()
    }
}

