package com.raremartial.aiac.mcp

import com.raremartial.aiac.network.models.McpTool
import com.raremartial.aiac.network.models.ToolInputSchema
import com.raremartial.aiac.network.models.ToolProperty
import co.touchlab.kermit.Logger
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * MCP Server для работы с инструментами (например, Яндекс.Трекер)
 * 
 * Сервер предоставляет HTTP API для:
 * - list_tools - получение списка доступных инструментов
 * - call_tool - вызов инструмента с параметрами
 */
class McpServer(
    private val port: Int = 8080,
    private val trackerApi: com.raremartial.aiac.network.YandexTrackerApi? = null,
    private val defaultQueue: String = "AIAC"
) {
    private val logger = Logger.withTag("McpServer")
    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var server: io.ktor.server.engine.EmbeddedServer<io.ktor.server.netty.NettyApplicationEngine, io.ktor.server.netty.NettyApplicationEngine.Configuration>? = null
    
    /**
     * MCP JSON-RPC запрос
     */
    @Serializable
    data class McpRequest(
        val jsonrpc: String = "2.0",
        val id: Int? = null,
        val method: String,
        val params: Map<String, String>? = null
    )
    
    /**
     * MCP JSON-RPC ответ
     */
    @Serializable
    data class McpResponse(
        val jsonrpc: String = "2.0",
        val id: Int? = null,
        val result: McpResult? = null,
        val error: McpError? = null
    )
    
    @Serializable
    data class McpResult(
        val tools: List<McpTool>? = null,
        val content: List<McpContent>? = null
    )
    
    @Serializable
    data class McpContent(
        val type: String,
        val text: String
    )
    
    @Serializable
    data class McpError(
        val code: Int,
        val message: String
    )
    
    /**
     * Запустить MCP сервер
     */
    fun start() {
        if (server != null) {
            logger.w { "Server is already running" }
            return
        }
        
        logger.d { "Starting MCP Server on port $port" }
        
        server = embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
            
            install(CORS) {
                anyHost()
                allowHeader("Content-Type")
                allowMethod(io.ktor.http.HttpMethod.Post)
            }
            
            routing {
                // MCP endpoint для list_tools
                post("/mcp") {
                    try {
                        val request = call.receive<McpRequest>()
                        logger.d { "Received MCP request: method=${request.method}, id=${request.id}, jsonrpc=${request.jsonrpc}" }
                        
                        when (request.method) {
                            "tools/list" -> {
                                val tools = getAvailableTools()
                                logger.d { "Returning ${tools.size} tools: ${tools.map { it.name }}" }
                                val response = McpResponse(
                                    jsonrpc = "2.0",
                                    id = request.id,
                                    result = McpResult(tools = tools)
                                )
                                logger.d { "Sending MCP response: jsonrpc=${response.jsonrpc}, id=${response.id}, tools count=${response.result?.tools?.size ?: 0}" }
                                call.respond(response)
                            }
                            
                            "tools/call" -> {
                                val toolName = request.params?.get("name") ?: ""
                                val argumentsJson = request.params?.get("arguments") ?: "{}"
                                
                                logger.d { "Calling tool: $toolName with arguments: $argumentsJson" }
                                
                                val result = callTool(toolName, argumentsJson)
                                val response = McpResponse(
                                    id = request.id,
                                    result = McpResult(
                                        content = listOf(
                                            McpContent(
                                                type = "text",
                                                text = result
                                            )
                                        )
                                    )
                                )
                                call.respond(response)
                            }
                            
                            else -> {
                                val error = McpError(
                                    code = -32601,
                                    message = "Method not found: ${request.method}"
                                )
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    McpResponse(
                                        id = request.id,
                                        error = error
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        logger.e(e) { "Error handling MCP request" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            McpResponse(
                                error = McpError(
                                    code = -32603,
                                    message = "Internal error: ${e.message}"
                                )
                            )
                        )
                    }
                }
            }
        }.start(wait = false)
        
        // Даем серверу время на запуск
        // В реальности сервер запускается асинхронно, но мы даем ему время на инициализацию
        kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.delay(1000) // Увеличена задержка для инициализации
        }
        
        // Проверяем, что сервер действительно запустился
        try {
            val serverInstance = server
            if (serverInstance != null) {
                logger.i { "MCP Server started successfully on http://localhost:$port" }
            } else {
                logger.e { "MCP Server failed to start - server instance is null" }
            }
        } catch (e: Exception) {
            logger.e(e) { "Error checking MCP Server status" }
        }
    }
    
    /**
     * Остановить MCP сервер
     */
    fun stop() {
        server?.stop(1000, 2000)
        server = null
        logger.i { "MCP Server stopped" }
    }
    
    /**
     * Получить список доступных инструментов
     */
    private fun getAvailableTools(): List<McpTool> {
        return listOf(
            McpTool(
                name = "get_task_count",
                description = "Получить количество задач в Яндекс.Трекере",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "queue" to ToolProperty(
                            type = "string",
                            description = "Очередь задач (опционально)"
                        )
                    ),
                    required = emptyList()
                )
            ),
            McpTool(
                name = "create_task",
                description = "Создать новую задачу в Яндекс.Трекере",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "title" to ToolProperty(
                            type = "string",
                            description = "Название задачи (обязательно)"
                        ),
                        "description" to ToolProperty(
                            type = "string",
                            description = "Описание задачи (опционально)"
                        ),
                        "queue" to ToolProperty(
                            type = "string",
                            description = "Очередь задач (опционально)"
                        )
                    ),
                    required = listOf("title")
                )
            )
        )
    }
    
    /**
     * Вызвать инструмент
     */
    private suspend fun callTool(toolName: String, argumentsJson: String): String {
        // Парсим аргументы из JSON
        val arguments = try {
            val json = Json { ignoreUnknownKeys = true; isLenient = true }
            val jsonElement = json.parseToJsonElement(argumentsJson)
            jsonElement.jsonObject.entries.associate { 
                it.key to it.value.jsonPrimitive.content 
            }
        } catch (e: Exception) {
            logger.w(e) { "Failed to parse arguments: $argumentsJson" }
            emptyMap()
        }
        
        return when (toolName) {
            "get_task_count" -> {
                val queue = arguments["queue"] ?: null
                val targetQueue = queue ?: defaultQueue
                
                if (trackerApi == null) {
                    return "Ошибка: API Яндекс.Трекера не настроен"
                }
                
                try {
                    val result = trackerApi.getTaskCount(targetQueue)
                    result.fold(
                        onSuccess = { count ->
                            buildString {
                                appendLine("Количество задач в Яндекс.Трекере: $count")
                                appendLine("Очередь: $targetQueue")
                                appendLine()
                                appendLine("✅ Данные получены из реального API Яндекс.Трекера")
                            }
                        },
                        onFailure = { error ->
                            "Ошибка при получении количества задач: ${error.message}"
                        }
                    )
                } catch (e: Exception) {
                    logger.e(e) { "Exception while getting task count" }
                    "Ошибка при получении количества задач: ${e.message}"
                }
            }
            
            "create_task" -> {
                val title = arguments["title"]
                if (title.isNullOrBlank()) {
                    return "Ошибка: не указано название задачи (title)"
                }
                
                val description = arguments["description"] ?: ""
                val queue = arguments["queue"] ?: defaultQueue
                
                if (trackerApi == null) {
                    return "Ошибка: API Яндекс.Трекера не настроен"
                }
                
                try {
                    val result = trackerApi.createTask(
                        queue = queue,
                        summary = title,
                        description = if (description.isNotEmpty()) description else null
                    )
                    result.fold(
                        onSuccess = { taskKey ->
                            buildString {
                                appendLine("✅ Задача успешно создана в Яндекс.Трекере!")
                                appendLine()
                                appendLine("ID задачи: $taskKey")
                                appendLine("Название: $title")
                                if (description.isNotEmpty()) {
                                    appendLine("Описание: $description")
                                }
                                appendLine("Очередь: $queue")
                                appendLine()
                                appendLine("Задача доступна в Яндекс.Трекере по ключу: $taskKey")
                            }
                        },
                        onFailure = { error ->
                            "Ошибка при создании задачи: ${error.message}"
                        }
                    )
                } catch (e: Exception) {
                    logger.e(e) { "Exception while creating task" }
                    "Ошибка при создании задачи: ${e.message}"
                }
            }
            
            else -> {
                "Неизвестный инструмент: $toolName\nДоступные инструменты:\n- get_task_count\n- create_task"
            }
        }
    }
}

