package com.raremartial.aiac.network

import com.raremartial.aiac.network.models.McpTool
import co.touchlab.kermit.Logger
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * API клиент для собственного MCP сервера
 */
interface CustomMcpApi {
    /**
     * Получить список всех доступных инструментов MCP
     */
    suspend fun getTools(): Result<List<McpTool>>
    
    /**
     * Вызвать инструмент MCP
     */
    suspend fun callTool(toolName: String, arguments: Map<String, String> = emptyMap()): Result<String>
}

/**
 * MCP JSON-RPC запрос для Custom MCP
 */
@Serializable
data class CustomMcpRequest(
    val jsonrpc: String = "2.0",
    val id: Int = 1,
    val method: String,
    val params: Map<String, String>? = null
)

/**
 * MCP JSON-RPC ответ для Custom MCP
 */
@Serializable
data class CustomMcpResponse(
    val jsonrpc: String? = null,
    val id: Int? = null,
    val result: CustomMcpResult? = null,
    val error: CustomMcpError? = null
)

@Serializable
data class CustomMcpResult(
    val tools: List<McpTool>? = null,
    val content: List<CustomMcpContent>? = null
)

@Serializable
data class CustomMcpContent(
    val type: String,
    val text: String
)

@Serializable
data class CustomMcpError(
    val code: Int? = null,
    val message: String? = null
)

class CustomMcpApiImpl(
    private val httpClient: io.ktor.client.HttpClient,
    private val baseUrl: String = "http://localhost:8080"
) : CustomMcpApi {

    private val logger = Logger.withTag("CustomMcpApi")
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    override suspend fun getTools(): Result<List<McpTool>> {
        val mcpRequest = CustomMcpRequest(
            method = "tools/list",
            params = null
        )
        
        logger.d { "Sending MCP request to $baseUrl/mcp: method=${mcpRequest.method}, id=${mcpRequest.id}" }
        
        return try {
            val httpResponse: HttpResponse = httpClient.post("$baseUrl/mcp") {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                setBody(mcpRequest)
            }
            
            val responseBody = httpResponse.bodyAsText()
            logger.d { "MCP response status: ${httpResponse.status}, body: $responseBody" }
            
            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    try {
                        val response = json.decodeFromString<CustomMcpResponse>(responseBody)
                        logger.d { "Parsed MCP response: jsonrpc=${response.jsonrpc}, id=${response.id}, error=${response.error}, tools count=${response.result?.tools?.size ?: 0}" }
                        
                        if (response.error != null) {
                            logger.w { "MCP API error: ${response.error.message}, code=${response.error.code}" }
                            return Result.failure(Exception(response.error.message ?: "Unknown error"))
                        }
                        val tools = response.result?.tools ?: emptyList()
                        logger.i { "Successfully retrieved ${tools.size} tools from MCP Server: ${tools.map { it.name }}" }
                        Result.success(tools)
                    } catch (e: Exception) {
                        logger.e(e) { "Failed to parse MCP response: $responseBody" }
                        Result.failure(Exception("Failed to parse response: ${e.message}"))
                    }
                }
                else -> {
                    logger.w { "MCP API error: ${httpResponse.status}, body: $responseBody" }
                    Result.failure(Exception("HTTP ${httpResponse.status}: $responseBody"))
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to get tools from MCP Server: ${e.message}" }
            logger.e(e) { "Exception type: ${e::class.simpleName}, cause: ${e.cause?.message}" }
            Result.failure(e)
        }
    }

    override suspend fun callTool(toolName: String, arguments: Map<String, String>): Result<String> {
        val params = mutableMapOf<String, String>()
        params["name"] = toolName
        // Сериализуем arguments в JSON строку
        val argumentsJson = if (arguments.isNotEmpty()) {
            val jsonObject = kotlinx.serialization.json.buildJsonObject {
                arguments.forEach { (key, value) ->
                    put(key, JsonPrimitive(value))
                }
            }
            jsonObject.toString()
        } else {
            "{}"
        }
        params["arguments"] = argumentsJson
        
        val mcpRequest = CustomMcpRequest(
            method = "tools/call",
            params = params
        )
        
        return try {
            val httpResponse: HttpResponse = httpClient.post("$baseUrl/mcp") {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                setBody(mcpRequest)
            }
            
            val responseBody = httpResponse.bodyAsText()
            logger.d { "MCP tool call response: $responseBody" }
            
            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    val response = json.decodeFromString<CustomMcpResponse>(responseBody)
                    if (response.error != null) {
                        logger.w { "MCP API error: ${response.error.message}" }
                        return Result.failure(Exception(response.error.message ?: "Unknown error"))
                    }
                    val content = response.result?.content?.firstOrNull()?.text ?: ""
                    logger.d { "Successfully called tool: $toolName" }
                    Result.success(content)
                }
                else -> {
                    logger.w { "MCP API error: ${httpResponse.status}" }
                    Result.failure(Exception("HTTP ${httpResponse.status}: $responseBody"))
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to call tool: $toolName" }
            Result.failure(e)
        }
    }
}


