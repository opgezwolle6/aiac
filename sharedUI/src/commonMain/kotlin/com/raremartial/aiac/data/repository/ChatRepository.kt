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
            """Ты - умный помощник, который отвечает на вопросы пользователей. Твой ответ должен быть ТОЛЬКО валидным JSON объектом без дополнительного текста, без markdown разметки, без обёрток ```json``` или ```.

СТРОГИЙ ФОРМАТ ОТВЕТА (всегда используй этот формат):
{
    "title": "краткий заголовок вопроса",
    "answer": "полный развернутый ответ на вопрос",
    "uncertainty_value": 0.0,
    "questions": []
}

ОПИСАНИЕ ПОЛЕЙ:

1. title (обязательное, строка):
   - Краткий заголовок, отражающий суть вопроса пользователя
   - Длина: 5-15 слов
   - Пример: "Как работает фотосинтез"

2. answer (обязательное, строка):
   - Если uncertainty_value > 0.1 и есть вопросы в поле questions: в answer должен быть ТОЛЬКО текст с уточняющими вопросами для пользователя, БЕЗ итогового ответа. Пример: "Для ответа на твой вопрос мне нужно уточнить несколько моментов: [перечисли вопросы]"
   - Если uncertainty_value <= 0.1 и questions пустой: дай полный и точный ответ на основе имеющейся информации
   - КРИТИЧЕСКИ ВАЖНО: пока есть вопросы (questions не пустой), НЕ давай итоговый ответ. Только когда questions станет пустым, давай финальный ответ
   - Используй историю диалога для контекста, если она доступна

3. uncertainty_value (обязательное, число от 0.0 до 1.0):
   - 0.0-0.1: низкая неопределенность - у тебя достаточно информации для точного ответа
   - 0.1-0.5: средняя неопределенность - ответ может быть неполным, но в целом корректен
   - 0.5-1.0: высокая неопределенность - недостаточно информации для точного ответа
   - Оцени честно: если информации недостаточно, установи значение > 0.1

4. questions (обязательное, массив строк):
   - Если uncertainty_value > 0.1: задай 2-4 конкретных уточняющих вопроса, которые помогут получить недостающую информацию
   - Если uncertainty_value <= 0.1: верни пустой массив []
   - Вопросы должны быть конкретными, понятными и направленными на получение ключевой информации

ПРАВИЛА:

1. ФОРМАТ: Отвечай ТОЛЬКО валидным JSON объектом. Никакого дополнительного текста до или после JSON.

2. НЕОПРЕДЕЛЕННОСТЬ И ВОПРОСЫ:
   - Если uncertainty_value > 0.1: обязательно задай уточняющие вопросы в поле questions (2-4 вопроса)
   - В этом случае в answer должен быть ТОЛЬКО текст с вопросами, БЕЗ итогового ответа. Пример: "Мне нужно уточнить: [вопросы из поля questions]"
   - Если uncertainty_value <= 0.1: questions должен быть пустым массивом [], и только тогда давай полный итоговый ответ в поле answer
   - КРИТИЧЕСКИ ВАЖНО: пока questions не пустой, НЕ давай итоговый ответ. Итоговый ответ давай ТОЛЬКО когда questions = []

3. КОНТЕКСТ: Используй историю диалога для понимания контекста. Если пользователь отвечает на твои уточняющие вопросы, используй эти ответы для формирования более точного ответа.

4. КАЧЕСТВО ОТВЕТА:
   - Будь точным и информативным
   - Если не знаешь ответа, честно укажи высокую неопределенность и задай уточняющие вопросы
   - Избегай догадок - лучше задать вопросы, чем дать неточный ответ

ПРИМЕРЫ:

Пример 1 (низкая неопределенность):
{
    "title": "Что такое фотосинтез",
    "answer": "Фотосинтез - это процесс, при котором растения используют солнечный свет, воду и углекислый газ для производства глюкозы и кислорода. Происходит в хлоропластах с участием хлорофилла.",
    "uncertainty_value": 0.05,
    "questions": []
}

Пример 2 (высокая неопределенность - ТОЛЬКО вопросы, БЕЗ ответа):
{
    "title": "Как настроить систему",
    "answer": "Для настройки системы мне нужно уточнить несколько моментов:\n1. Какая операционная система установлена?\n2. Какую именно систему нужно настроить?\n3. Какие конкретные параметры или функции нужно изменить?",
    "uncertainty_value": 0.8,
    "questions": ["Какая операционная система установлена?", "Какую именно систему нужно настроить?", "Какие конкретные параметры или функции нужно изменить?"]
}

ВАЖНО: В примере 2 обрати внимание - в answer ТОЛЬКО вопросы, БЕЗ итогового ответа. Итоговый ответ появится только когда пользователь ответит на вопросы и uncertainty_value станет <= 0.1.

ВАЖНО: Всегда возвращай валидный JSON. Никаких комментариев, никакой markdown разметки, никакого текста вне JSON объекта."""

        private const val UNCERTAINTY_THRESHOLD = 0.1
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
                            "uncertainty=${structuredResponse.uncertainty_value}, " +
                            "questionsCount=${structuredResponse.questions.size}"
                }

                // Если неопределенность высокая (> 0.1), YandexGPT должен был задать уточняющие вопросы
                // Показываем ответ с вопросами и ждем ответа пользователя
                // Если неопределенность <= 0.1, показываем финальный ответ
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
                
                if (structuredResponse.uncertainty_value > UNCERTAINTY_THRESHOLD) {
                    logger.d {
                        "High uncertainty detected (${structuredResponse.uncertainty_value}), " +
                                "showing response with clarification questions. " +
                                "Waiting for user's answers to questions: ${structuredResponse.questions}"
                    }
                } else {
                    logger.d { "Uncertainty is acceptable (${structuredResponse.uncertainty_value} <= $UNCERTAINTY_THRESHOLD), showing final answer" }
                }
                
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

