package com.raremartial.aiac.network

import co.touchlab.kermit.Logger
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * API клиент для Яндекс.Трекера
 * Документация: https://yandex.ru/support/tracker/ru/common-format
 */
interface YandexTrackerApi {
    /**
     * Получить количество задач в очереди
     * @param queue Ключ очереди (опционально)
     * @return Количество задач
     */
    suspend fun getTaskCount(queue: String? = null): Result<Int>
    
    /**
     * Создать задачу в очереди
     * @param queue Ключ очереди
     * @param summary Название задачи
     * @param description Описание задачи (опционально)
     * @return Ключ созданной задачи (например, "AIAC-123")
     */
    suspend fun createTask(
        queue: String,
        summary: String,
        description: String? = null
    ): Result<String>
}

/**
 * Запрос на поиск задач
 */
@Serializable
data class IssuesSearchRequest(
    val filter: IssuesFilter? = null
)

@Serializable
data class IssuesFilter(
    val queue: String? = null
)

/**
 * Ответ на поиск задач
 */
@Serializable
data class IssuesSearchResponse(
    val total: Int? = null
)

/**
 * Запрос на создание задачи
 */
@Serializable
data class CreateIssueRequest(
    val queue: String,
    val summary: String,
    val description: String? = null,
    val type: String = "task" // task, bug, feature и т.д.
)

/**
 * Ответ на создание задачи
 */
@Serializable
data class CreateIssueResponse(
    val key: String? = null,
    val summary: String? = null,
    val description: String? = null
)

/**
 * Ошибка API Яндекс.Трекера
 */
@Serializable
data class TrackerApiError(
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val errorDetails: String? = null
)

class YandexTrackerApiImpl(
    private val httpClient: io.ktor.client.HttpClient,
    private val iamToken: String,
    private val orgId: String,
    private val baseUrl: String = "https://api.tracker.yandex.net/v3"
) : YandexTrackerApi {

    private val logger = Logger.withTag("YandexTrackerApi")
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    /**
     * Определяет тип токена и возвращает правильный заголовок Authorization
     * IAM токены начинаются с "t1.", OAuth токены обычно короче и имеют другой формат
     */
    private fun getAuthorizationHeader(): String {
        return if (iamToken.startsWith("t1.")) {
            // IAM токен - используем Bearer
            "Bearer $iamToken"
        } else {
            // OAuth токен - используем OAuth
            "OAuth $iamToken"
        }
    }

    override suspend fun getTaskCount(queue: String?): Result<Int> {
        val url = "$baseUrl/issues/_search"
        
        val request = IssuesSearchRequest(
            filter = if (queue != null) IssuesFilter(queue = queue) else null
        )
        
        logger.d { "Getting task count from Yandex Tracker: queue=$queue" }
        
        return try {
            val httpResponse: HttpResponse = httpClient.post(url) {
                headers {
                    append(HttpHeaders.Authorization, getAuthorizationHeader())
                    append("X-Cloud-Org-ID", orgId)
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                setBody(request)
            }
            
            val responseBody = httpResponse.bodyAsText()
            logger.d { "Yandex Tracker response: ${responseBody.take(500)}" }
            
            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    // Проверяем заголовок X-Total-Count, если он есть (предпочтительный способ)
                    val totalCountHeader = httpResponse.headers["X-Total-Count"]?.toIntOrNull()
                    
                    if (totalCountHeader != null) {
                        logger.d { "Successfully retrieved task count from header: $totalCountHeader" }
                        Result.success(totalCountHeader)
                    } else {
                        // Если заголовка нет, считаем количество задач в массиве
                        try {
                            val issues = Json.parseToJsonElement(responseBody)
                            val count = if (issues is kotlinx.serialization.json.JsonArray) {
                                issues.size
                            } else {
                                logger.w { "Unexpected response format, expected array" }
                                0
                            }
                            logger.d { "Successfully retrieved task count from array: $count" }
                            Result.success(count)
                        } catch (e: Exception) {
                            logger.e(e) { "Failed to parse response: ${responseBody.take(200)}" }
                            Result.failure(Exception("Failed to parse response: ${e.message}"))
                        }
                    }
                }
                else -> {
                    logger.w { "Yandex Tracker API error: ${httpResponse.status}, body: $responseBody" }
                    val errorMessage = try {
                        val error = json.decodeFromString<TrackerApiError>(responseBody)
                        error.errorMessage ?: "HTTP ${httpResponse.status}"
                    } catch (e: Exception) {
                        "HTTP ${httpResponse.status}: $responseBody"
                    }
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to get task count from Yandex Tracker" }
            Result.failure(e)
        }
    }

    override suspend fun createTask(
        queue: String,
        summary: String,
        description: String?
    ): Result<String> {
        val url = "$baseUrl/issues/"
        
        val request = CreateIssueRequest(
            queue = queue,
            summary = summary,
            description = description,
            type = "task"
        )
        
        logger.d { "Creating task in Yandex Tracker: queue=$queue, summary=$summary" }
        
        return try {
            val httpResponse: HttpResponse = httpClient.post(url) {
                headers {
                    append(HttpHeaders.Authorization, getAuthorizationHeader())
                    append("X-Cloud-Org-ID", orgId)
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                setBody(request)
            }
            
            val responseBody = httpResponse.bodyAsText()
            logger.d { "Yandex Tracker create task response: ${responseBody.take(500)}" }
            
            when (httpResponse.status) {
                HttpStatusCode.Created, HttpStatusCode.OK -> {
                    val response = json.decodeFromString<CreateIssueResponse>(responseBody)
                    val taskKey = response.key
                    
                    if (taskKey == null) {
                        logger.w { "Task created but key is null in response: $responseBody" }
                        return Result.failure(Exception("Task created but key is missing in response"))
                    }
                    
                    logger.d { "Successfully created task: $taskKey" }
                    Result.success(taskKey)
                }
                else -> {
                    logger.w { "Yandex Tracker API error: ${httpResponse.status}, body: $responseBody" }
                    val errorMessage = try {
                        val error = json.decodeFromString<TrackerApiError>(responseBody)
                        error.errorMessage ?: "HTTP ${httpResponse.status}"
                    } catch (e: Exception) {
                        "HTTP ${httpResponse.status}: $responseBody"
                    }
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to create task in Yandex Tracker" }
            Result.failure(e)
        }
    }
}

