package com.raremartial.aiac.network.models

import kotlinx.serialization.Serializable

/**
 * Модель инструмента MCP (Model Context Protocol)
 */
@Serializable
data class McpTool(
    val name: String,
    val description: String? = null,
    val inputSchema: ToolInputSchema? = null
)

/**
 * Схема входных параметров инструмента
 */
@Serializable
data class ToolInputSchema(
    val type: String? = null,
    val properties: Map<String, ToolProperty>? = null,
    val required: List<String>? = null
)

/**
 * Свойство параметра инструмента
 */
@Serializable
data class ToolProperty(
    val type: String? = null,
    val description: String? = null,
    val enum: List<String>? = null
)

/**
 * Ответ со списком инструментов MCP
 */
@Serializable
data class McpToolsResponse(
    val tools: List<McpTool>? = null,
    val error: McpError? = null
)

/**
 * Ошибка MCP
 */
@Serializable
data class McpError(
    val code: Int? = null,
    val message: String? = null
)

