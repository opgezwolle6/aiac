package com.raremartial.aiac.data.repository

import co.touchlab.kermit.Logger
import com.raremartial.aiac.data.mapper.ChatMessageMapper
import com.raremartial.aiac.data.model.ChatMessage
import com.raremartial.aiac.data.model.MessageRole
import com.raremartial.aiac.data.model.StructuredResponse
import com.raremartial.aiac.network.YandexGPTApi
import com.raremartial.aiac.network.models.CompletionOptions
import com.raremartial.aiac.network.models.Message
import com.raremartial.aiac.network.models.YandexGPTRequest
import com.raremartial.aiac.util.currentTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
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
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()

    companion object {
        private const val SYSTEM_PROMPT =
            """Ты - помощник, который отвечает на вопросы в строго заданном формате JSON. 
Твой ответ должен быть ТОЛЬКО валидным JSON объектом без дополнительного текста, без markdown разметки, без обёрток ```json```.

Формат ответа:
{
    "title": "краткий заголовок вопроса",
    "answer": "полный развернутый ответ на вопрос",
    "urls": ["url1", "url2", "url3"],
    "uncertainty_value": 0.5
}

Где:
- title: краткий заголовок заданного вопроса
- answer: полный развернутый текст ответа на вопрос
- urls: массив строк с URL источниками информации (если есть)
- uncertainty_value: число от 0 до 1, где 1 означает высокую неопределенность ответа, а 0 - низкую неопределенность

Важно: отвечай ТОЛЬКО валидным JSON объектом, без дополнительного текста."""

        private const val MAX_RETRIES = 3
        private const val UNCERTAINTY_THRESHOLD = 0.7
    }

    override suspend fun sendMessage(text: String): Result<ChatMessage> {
        val userMessage = ChatMessage(
            id = ChatMessageMapper.generateId(),
            content = text,
            role = MessageRole.USER,
            timestamp = currentTime()
        )

        _messages.value = _messages.value + userMessage

        return sendMessageWithRetries(text, userMessage, retryCount = 0)
    }

    private suspend fun sendMessageWithRetries(
        originalText: String,
        userMessage: ChatMessage,
        retryCount: Int
    ): Result<ChatMessage> {
        val pendingMessage = ChatMessage(
            id = ChatMessageMapper.generateId(),
            content = "",
            role = MessageRole.ASSISTANT,
            timestamp = currentTime(),
            isPending = true
        )
        _messages.value = _messages.value + pendingMessage

        // Получаем историю сообщений без pending сообщений
        val conversationHistory = _messages.value
            .filter { !it.isPending && it.id != pendingMessage.id }
            .map { ChatMessageMapper.toNetwork(it) }

        // Добавляем системный промпт только в первый запрос (retryCount == 0)
        // YandexGPT сохраняет контекст системного сообщения в разговоре
        val networkMessages = if (retryCount == 0) {
            listOf(Message(role = "system", text = SYSTEM_PROMPT)) + conversationHistory
        } else {
            // При повторных запросах системный промпт уже был отправлен ранее
            // и YandexGPT сохраняет его в контексте, поэтому отправляем только историю
            conversationHistory
        }

        val request = YandexGPTRequest(
            modelUri = "",
            completionOptions = CompletionOptions(),
            messages = networkMessages
        )

        logger.d { "Sending message to API: text=${originalText.take(50)}..., messagesCount=${networkMessages.size}, retryCount=$retryCount" }

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

                // Логируем сырой JSON ответ от YandexGPT
                logger.d { "Raw JSON response from YandexGPT:\n${formatJsonForLogging(responseMessage.text)}" }

                // Парсим JSON ответ
                val structuredResponse = parseStructuredResponse(responseMessage.text)

                if (structuredResponse == null) {
                    logger.e {
                        "Failed to parse structured response from: ${
                            responseMessage.text.take(
                                200
                            )
                        }"
                    }
                    _messages.value = _messages.value.filter { it.id != pendingMessage.id }
                    return Result.failure(Exception("Failed to parse structured response from YandexGPT"))
                }

                logger.d {
                    "Parsed structured response: title=${structuredResponse.title.take(50)}, " +
                            "uncertainty=${structuredResponse.uncertainty_value}, urlsCount=${structuredResponse.urls.size}"
                }

                // Если неопределенность высокая и мы еще не достигли лимита попыток, делаем повторный запрос
                if (structuredResponse.uncertainty_value >= UNCERTAINTY_THRESHOLD && retryCount < MAX_RETRIES) {
                    logger.d {
                        "High uncertainty detected (${structuredResponse.uncertainty_value}), " +
                                "requesting clarification. Retry ${retryCount + 1}/$MAX_RETRIES"
                    }

                    // Удаляем pending сообщение
                    _messages.value = _messages.value.filter { it.id != pendingMessage.id }

                    // Добавляем уточняющий вопрос в историю
                    val clarificationQuestion = "Неопределенность ответа высокая (${structuredResponse.uncertainty_value}). Пожалуйста, уточни ответ и предоставь более точную информацию."
                    val clarificationMessage = ChatMessage(
                        id = ChatMessageMapper.generateId(),
                        content = clarificationQuestion,
                        role = MessageRole.USER,
                        timestamp = currentTime()
                    )
                    _messages.value = _messages.value + clarificationMessage

                    // Повторяем запрос
                    return sendMessageWithRetries(originalText, userMessage, retryCount + 1)
                }

                // Создаем финальное сообщение с структурированными данными
                val assistantMessage = ChatMessage(
                    id = pendingMessage.id,
                    content = structuredResponse.answer,
                    role = MessageRole.ASSISTANT,
                    timestamp = currentTime(),
                    isPending = false,
                    structuredData = structuredResponse
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

    private fun parseStructuredResponse(text: String): StructuredResponse? {
        return try {
            // Пытаемся найти JSON в тексте (может быть обёрнут в markdown)
            var jsonText = text
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            // Если текст не начинается с "{", пытаемся найти JSON объект в тексте
            if (!jsonText.startsWith("{")) {
                val jsonStart = jsonText.indexOf("{")
                val jsonEnd = jsonText.lastIndexOf("}")
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    jsonText = jsonText.substring(jsonStart, jsonEnd + 1)
                }
            }

            json.decodeFromString<StructuredResponse>(jsonText)
        } catch (e: Exception) {
            logger.e(e) { "Failed to parse structured response: ${text.take(200)}" }
            null
        }
    }

    /**
     * Форматирует JSON текст для красивого вывода в логах
     */
    private fun formatJsonForLogging(jsonText: String): String {
        return try {
            // Пытаемся найти JSON в тексте
            var cleanedText = jsonText
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            // Если текст не начинается с "{", пытаемся найти JSON объект
            if (!cleanedText.startsWith("{")) {
                val jsonStart = cleanedText.indexOf("{")
                val jsonEnd = cleanedText.lastIndexOf("}")
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    cleanedText = cleanedText.substring(jsonStart, jsonEnd + 1)
                }
            }

            // Парсим и форматируем JSON
            val parsed = json.decodeFromString<StructuredResponse>(cleanedText)
            json.encodeToString(StructuredResponse.serializer(), parsed)
        } catch (e: Exception) {
            // Если не удалось распарсить, возвращаем исходный текст
            jsonText
        }
    }

    /**
     * Форматирует структурированный ответ для вывода в логах
     */
    private fun formatStructuredResponse(response: StructuredResponse): String {
        return try {
            json.encodeToString(StructuredResponse.serializer(), response)
        } catch (e: Exception) {
            "Failed to format structured response: ${e.message}"
        }
    }

    override suspend fun clearHistory() {
        _messages.value = emptyList()
    }
}

