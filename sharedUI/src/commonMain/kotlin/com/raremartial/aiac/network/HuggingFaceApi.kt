package com.raremartial.aiac.network

import com.raremartial.aiac.network.models.HuggingFaceRequest
import com.raremartial.aiac.network.models.HuggingFaceResponse
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

/**
 * API клиент для HuggingFace Inference Providers
 * Использует OpenAI-совместимый endpoint
 * Документация: https://huggingface.co/docs/inference-providers/index
 */
interface HuggingFaceApi {
    suspend fun sendMessage(request: HuggingFaceRequest): Result<HuggingFaceResponse>
}

class HuggingFaceApiImpl(
    private val httpClient: io.ktor.client.HttpClient,
    private val apiKey: String,
    private val baseUrl: String = "https://router.huggingface.co"
) : HuggingFaceApi {

    private val logger = Logger.withTag("HuggingFaceApi")
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    override suspend fun sendMessage(request: HuggingFaceRequest): Result<HuggingFaceResponse> {
        val url = "$baseUrl/v1/chat/completions"

        logger.d {
            "Sending request to HuggingFace: url=$url, model=${request.model}, " +
            "messagesCount=${request.messages.size}, temperature=${request.temperature}"
        }

        return try {
            val httpResponse: HttpResponse = httpClient.post(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val responseBody = httpResponse.bodyAsText()
            logger.d { "Raw response body: ${responseBody.take(500)}..." }

            val statusCode = httpResponse.status.value
            if (statusCode < 200 || statusCode >= 300) {
                logger.e { "HTTP error: ${httpResponse.status}, body: $responseBody" }
                
                val error = try {
                    json.decodeFromString<HuggingFaceResponse>(responseBody).error
                } catch (e: Exception) {
                    com.raremartial.aiac.network.models.HuggingFaceError(
                        message = "HTTP ${httpResponse.status.value}: ${httpResponse.status.description}"
                    )
                }

                val errorMessage = error?.message ?: "HTTP ${httpResponse.status.value}"
                return Result.failure(Exception(errorMessage))
            }

            val response = try {
                json.decodeFromString<HuggingFaceResponse>(responseBody)
            } catch (e: Exception) {
                logger.e(e) { "Failed to decode response: ${responseBody.take(500)}" }
                return Result.failure(Exception("Failed to parse API response: ${e.message}"))
            }

            if (response.error != null) {
                logger.e { "API error: ${response.error.message}, type: ${response.error.type}" }
                return Result.failure(Exception(response.error.message))
            }

            if (response.choices.isEmpty()) {
                logger.e { "Response has no choices: $responseBody" }
                return Result.failure(Exception("Invalid response: no choices field. Response: $responseBody"))
            }

            logger.d {
                "Response received: choicesCount=${response.choices.size}, " +
                "firstMessageContent=${response.choices.firstOrNull()?.message?.content?.take(50) ?: "null"}..., " +
                "usage=${response.usage}"
            }

            Result.success(response)
        } catch (e: io.ktor.serialization.JsonConvertException) {
            logger.e(e) { "JSON deserialization error: ${e.message}" }
            Result.failure(Exception("Failed to parse API response: ${e.message}"))
        } catch (e: Exception) {
            logger.e(e) { "Failed to send request to HuggingFace: ${e.message}" }
            Result.failure(e)
        }
    }
}

