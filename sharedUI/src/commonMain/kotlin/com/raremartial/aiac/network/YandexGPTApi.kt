package com.raremartial.aiac.network

import com.raremartial.aiac.network.models.YandexGPTRequest
import com.raremartial.aiac.network.models.YandexGPTResponse
import com.raremartial.aiac.network.models.ApiError
import co.touchlab.kermit.Logger
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

interface YandexGPTApi {
    suspend fun sendMessage(request: YandexGPTRequest): Result<YandexGPTResponse>
}

class YandexGPTApiImpl(
    private val httpClient: io.ktor.client.HttpClient,
    private val apiKey: String,
    private val folderId: String,
    private val baseUrl: String = "https://llm.api.cloud.yandex.net"
) : YandexGPTApi {

    private val logger = Logger.withTag("YandexGPTApi")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun sendMessage(request: YandexGPTRequest): Result<YandexGPTResponse> {
        val url = "$baseUrl/foundationModels/v1/completion"
        val fullRequest = request.copy(modelUri = "gpt://$folderId/yandexgpt-lite")
        
        logger.d { 
            "Sending request to YandexGPT: url=$url, modelUri=${fullRequest.modelUri}, " +
            "messagesCount=${fullRequest.messages.size}, maxTokens=${fullRequest.completionOptions.maxTokens}"
        }
        
        return try {
            val httpResponse: HttpResponse = httpClient.post(url) {
                headers {
                    append(HttpHeaders.Authorization, "Api-Key $apiKey")
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                contentType(ContentType.Application.Json)
                setBody(fullRequest)
            }
            
            val responseBody = httpResponse.bodyAsText()
            logger.d { "Raw response body: $responseBody" }
            
            val statusCode = httpResponse.status.value
            if (statusCode < 200 || statusCode >= 300) {
                logger.e { "HTTP error: ${httpResponse.status}, body: $responseBody" }
                val error = try {
                    json.decodeFromString<YandexGPTResponse>(responseBody).error
                } catch (e: Exception) {
                    ApiError("HTTP ${httpResponse.status.value}: ${httpResponse.status.description}")
                }
                
                val errorMessage = error?.message ?: "HTTP ${httpResponse.status.value}"
                val enhancedMessage = if (errorMessage.contains("Permission", ignoreCase = true) || 
                                          errorMessage.contains("denied", ignoreCase = true)) {
                    buildString {
                        appendLine(errorMessage)
                        appendLine()
                        appendLine("Для решения проблемы необходимо:")
                        appendLine("1. Откройте консоль Yandex Cloud: https://console.cloud.yandex.ru/")
                        appendLine("2. Перейдите в раздел 'Сервисные аккаунты'")
                        appendLine("3. Найдите сервисный аккаунт, к которому привязан API-ключ")
                        appendLine("4. Назначьте сервисному аккаунту роль: ai.languageModels.user")
                        appendLine("5. Убедитесь, что сервисный аккаунт имеет доступ к папке: $folderId")
                        appendLine()
                        appendLine("Подробная инструкция: https://yandex.cloud/ru/docs/ai-studio/operations/get-api-key")
                    }
                } else {
                    errorMessage
                }
                
                return Result.failure(Exception(enhancedMessage))
            }
            
            val response = try {
                json.decodeFromString<YandexGPTResponse>(responseBody)
            } catch (e: Exception) {
                logger.e(e) { "Failed to decode response: $responseBody" }
                return Result.failure(Exception("Failed to parse API response: ${e.message}"))
            }
            
            if (response.error != null) {
                logger.e { "API error: ${response.error.message}, code: ${response.error.code}" }
                return Result.failure(Exception(response.error.message))
            }
            
            if (response.result == null) {
                logger.e { "Response has no result and no error: $responseBody" }
                return Result.failure(Exception("Invalid response: no result field. Response: $responseBody"))
            }
            
            logger.d { 
                "Response received: alternativesCount=${response.result.alternatives.size}, " +
                "firstMessageText=${response.result.alternatives.firstOrNull()?.message?.text?.take(50) ?: "null"}..."
            }
            
            Result.success(response)
        } catch (e: io.ktor.serialization.JsonConvertException) {
            logger.e(e) { "JSON deserialization error: ${e.message}" }
            Result.failure(Exception("Failed to parse API response: ${e.message}"))
        } catch (e: Exception) {
            logger.e(e) { "Failed to send request to YandexGPT: ${e.message}" }
            Result.failure(e)
        }
    }
}

