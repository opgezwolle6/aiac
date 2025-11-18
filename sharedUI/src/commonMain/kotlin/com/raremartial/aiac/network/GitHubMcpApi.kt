package com.raremartial.aiac.network

import com.raremartial.aiac.network.models.McpTool
import com.raremartial.aiac.network.models.ToolInputSchema
import com.raremartial.aiac.network.models.ToolProperty
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import com.raremartial.aiac.network.models.McpError

/**
 * API клиент для GitHub MCP Server
 * Документация: https://github.com/github/github-mcp-server
 * 
 * GitHub MCP Server предоставляет инструменты для работы с GitHub:
 * - Issues, Pull Requests, Repositories
 * - Code Search, File Operations
 * - GitHub Actions, Security Advisories
 * - Copilot, Copilot Spaces
 * - И многие другие инструменты
 */
interface GitHubMcpApi {
    /**
     * Получить список всех доступных инструментов MCP
     */
    suspend fun getTools(): Result<List<McpTool>>
}

/**
 * MCP JSON-RPC запрос для list_tools
 */
@Serializable
data class McpRequest(
    val jsonrpc: String = "2.0",
    val id: Int = 1,
    val method: String,
    val params: Map<String, String>? = null
)

/**
 * MCP JSON-RPC ответ
 */
@Serializable
data class McpResponse(
    val jsonrpc: String? = null,
    val id: Int? = null,
    val result: McpResult? = null,
    val error: McpError? = null
)

@Serializable
data class McpResult(
    val tools: List<McpTool>? = null
)

class GitHubMcpApiImpl(
    private val httpClient: io.ktor.client.HttpClient,
    private val githubToken: String,
    private val baseUrl: String? = null // Если null, используется известный список инструментов
) : GitHubMcpApi {

    private val logger = Logger.withTag("GitHubMcpApi")
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Получить известные инструменты из документации GitHub MCP Server
     * Документация: https://github.com/github/github-mcp-server
     * 
     * GitHub MCP Server предоставляет множество инструментов, сгруппированных по категориям:
     * - Issues, Pull Requests, Repositories
     * - Code Search, File Operations
     * - GitHub Actions, Security Advisories
     * - Copilot, Copilot Spaces
     */
    private fun getKnownTools(): List<McpTool> {
        return listOf(
            // Issues Tools
            McpTool(
                name = "add_issue_comment",
                description = "Add a comment to an issue",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "owner" to ToolProperty(type = "string", description = "Repository owner"),
                        "repo" to ToolProperty(type = "string", description = "Repository name"),
                        "issue_number" to ToolProperty(type = "number", description = "Issue number"),
                        "body" to ToolProperty(type = "string", description = "Comment body")
                    ),
                    required = listOf("owner", "repo", "issue_number", "body")
                )
            ),
            McpTool(
                name = "create_issue",
                description = "Create a new issue",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "owner" to ToolProperty(type = "string", description = "Repository owner"),
                        "repo" to ToolProperty(type = "string", description = "Repository name"),
                        "title" to ToolProperty(type = "string", description = "Issue title"),
                        "body" to ToolProperty(type = "string", description = "Issue body")
                    ),
                    required = listOf("owner", "repo", "title")
                )
            ),
            McpTool(
                name = "get_issue",
                description = "Get an issue by number",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "owner" to ToolProperty(type = "string", description = "Repository owner"),
                        "repo" to ToolProperty(type = "string", description = "Repository name"),
                        "issue_number" to ToolProperty(type = "number", description = "Issue number")
                    ),
                    required = listOf("owner", "repo", "issue_number")
                )
            ),
            McpTool(
                name = "list_issues",
                description = "List issues in a repository",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "owner" to ToolProperty(type = "string", description = "Repository owner"),
                        "repo" to ToolProperty(type = "string", description = "Repository name"),
                        "state" to ToolProperty(type = "string", description = "Issue state (open, closed, all)"),
                        "labels" to ToolProperty(type = "string", description = "Comma-separated list of labels")
                    ),
                    required = listOf("owner", "repo")
                )
            ),
            
            // Pull Requests Tools
            McpTool(
                name = "create_pull_request",
                description = "Create a pull request",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "owner" to ToolProperty(type = "string", description = "Repository owner"),
                        "repo" to ToolProperty(type = "string", description = "Repository name"),
                        "title" to ToolProperty(type = "string", description = "PR title"),
                        "body" to ToolProperty(type = "string", description = "PR body"),
                        "head" to ToolProperty(type = "string", description = "Head branch"),
                        "base" to ToolProperty(type = "string", description = "Base branch")
                    ),
                    required = listOf("owner", "repo", "title", "head", "base")
                )
            ),
            McpTool(
                name = "get_pull_request",
                description = "Get a pull request by number",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "owner" to ToolProperty(type = "string", description = "Repository owner"),
                        "repo" to ToolProperty(type = "string", description = "Repository name"),
                        "pull_number" to ToolProperty(type = "number", description = "Pull request number")
                    ),
                    required = listOf("owner", "repo", "pull_number")
                )
            ),
            McpTool(
                name = "list_pull_requests",
                description = "List pull requests in a repository",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "owner" to ToolProperty(type = "string", description = "Repository owner"),
                        "repo" to ToolProperty(type = "string", description = "Repository name"),
                        "state" to ToolProperty(type = "string", description = "PR state (open, closed, all)")
                    ),
                    required = listOf("owner", "repo")
                )
            ),
            
            // Repository Tools
            McpTool(
                name = "get_repository",
                description = "Get repository information",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "owner" to ToolProperty(type = "string", description = "Repository owner"),
                        "repo" to ToolProperty(type = "string", description = "Repository name")
                    ),
                    required = listOf("owner", "repo")
                )
            ),
            McpTool(
                name = "list_repositories",
                description = "List repositories for a user or organization",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "username" to ToolProperty(type = "string", description = "Username or organization"),
                        "type" to ToolProperty(type = "string", description = "Repository type (all, owner, member)")
                    ),
                    required = listOf("username")
                )
            ),
            McpTool(
                name = "search_repositories",
                description = "Search repositories",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "query" to ToolProperty(type = "string", description = "Search query"),
                        "sort" to ToolProperty(type = "string", description = "Sort field"),
                        "order" to ToolProperty(type = "string", description = "Sort order (asc, desc)")
                    ),
                    required = listOf("query")
                )
            ),
            
            // Code Search Tools
            McpTool(
                name = "code_search",
                description = "Search code across GitHub",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "query" to ToolProperty(type = "string", description = "Code search query"),
                        "sort" to ToolProperty(type = "string", description = "Sort field"),
                        "order" to ToolProperty(type = "string", description = "Sort order (asc, desc)")
                    ),
                    required = listOf("query")
                )
            ),
            McpTool(
                name = "get_file_contents",
                description = "Get file contents from a repository",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "owner" to ToolProperty(type = "string", description = "Repository owner"),
                        "repo" to ToolProperty(type = "string", description = "Repository name"),
                        "path" to ToolProperty(type = "string", description = "File path"),
                        "ref" to ToolProperty(type = "string", description = "Branch or commit SHA")
                    ),
                    required = listOf("owner", "repo", "path")
                )
            ),
            
            // GitHub Actions Tools
            McpTool(
                name = "list_workflow_runs",
                description = "List workflow runs for a repository",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "owner" to ToolProperty(type = "string", description = "Repository owner"),
                        "repo" to ToolProperty(type = "string", description = "Repository name"),
                        "status" to ToolProperty(type = "string", description = "Workflow run status"),
                        "conclusion" to ToolProperty(type = "string", description = "Workflow run conclusion")
                    ),
                    required = listOf("owner", "repo")
                )
            ),
            
            // Security Advisories Tools
            McpTool(
                name = "list_security_advisories",
                description = "List security advisories",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "ecosystem" to ToolProperty(type = "string", description = "Package ecosystem"),
                        "severity" to ToolProperty(type = "string", description = "Advisory severity"),
                        "type" to ToolProperty(type = "string", description = "Advisory type")
                    ),
                    required = emptyList()
                )
            ),
            
            // Copilot Tools
            McpTool(
                name = "create_pull_request_with_copilot",
                description = "Perform task with GitHub Copilot coding agent",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "owner" to ToolProperty(type = "string", description = "Repository owner"),
                        "repo" to ToolProperty(type = "string", description = "Repository name"),
                        "problem_statement" to ToolProperty(type = "string", description = "Task description"),
                        "title" to ToolProperty(type = "string", description = "PR title"),
                        "base_ref" to ToolProperty(type = "string", description = "Base branch")
                    ),
                    required = listOf("owner", "repo", "problem_statement", "title")
                )
            ),
            McpTool(
                name = "get_copilot_space",
                description = "Get Copilot Space",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "owner" to ToolProperty(type = "string", description = "Space owner"),
                        "name" to ToolProperty(type = "string", description = "Space name")
                    ),
                    required = listOf("owner", "name")
                )
            ),
            McpTool(
                name = "list_copilot_spaces",
                description = "List Copilot Spaces"
            ),
            
            // GitHub Support Docs Search
            McpTool(
                name = "github_support_docs_search",
                description = "Retrieve documentation relevant to answer GitHub product and support questions",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "query" to ToolProperty(type = "string", description = "User question about GitHub")
                    ),
                    required = listOf("query")
                )
            )
        )
    }

    override suspend fun getTools(): Result<List<McpTool>> {
        // GitHub MCP Server обычно работает через stdio или SSE
        // Для упрощения, если baseUrl не указан, возвращаем известные инструменты из документации
        if (baseUrl == null) {
            logger.d { "No baseUrl provided, returning known GitHub MCP Server tools" }
            return Result.success(getKnownTools())
        }
        
        // Формируем SSE URL
        // Если baseUrl уже содержит путь /mcp или /sse, используем как есть
        // Иначе добавляем /sse к baseUrl
        val sseUrl = when {
            baseUrl.endsWith("/sse") -> baseUrl
            baseUrl.endsWith("/mcp") -> "$baseUrl/sse"
            baseUrl.endsWith("/") -> "${baseUrl}sse"
            else -> "$baseUrl/sse"
        }
        
        logger.d { "Trying to get tools from GitHub MCP Server: $sseUrl" }
        
        // Пробуем отправить MCP JSON-RPC запрос list_tools
        val mcpRequest = McpRequest(
            method = "tools/list",
            params = null
        )
        
        return try {
            // Пробуем POST запрос к SSE endpoint
            // Примечание: Публичный endpoint GitHub (https://api.githubcopilot.com/mcp/) 
            // использует OAuth 2.1 с PKCE, но мы пробуем использовать PAT для совместимости
            val httpResponse: HttpResponse = httpClient.post(sseUrl) {
                headers {
                    // Для публичного GitHub endpoint пробуем использовать PAT
                    // Если не работает, потребуется реализация OAuth 2.1 с PKCE
                    // Для собственного сервера используем Bearer token с PAT
                    if (baseUrl.contains("api.githubcopilot.com")) {
                        // Публичный endpoint может требовать OAuth, но пробуем PAT
                        // В случае ошибки 401/403 нужно реализовать OAuth flow
                        append(HttpHeaders.Authorization, "Bearer $githubToken")
                        // Может потребоваться дополнительный заголовок для OAuth
                        // append("X-GitHub-Api-Version", "2022-11-28")
                    } else {
                        // Для собственного сервера используем стандартный Bearer token
                        append(HttpHeaders.Authorization, "Bearer $githubToken")
                    }
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    append(HttpHeaders.Accept, "text/event-stream")
                }
                setBody(mcpRequest)
            }

            val responseBody = httpResponse.bodyAsText()
            logger.d { "Response from GitHub MCP Server: ${responseBody.take(500)}..." }

            val statusCode = httpResponse.status.value
            
            when {
                statusCode == HttpStatusCode.OK.value -> {
                    // Пробуем распарсить MCP JSON-RPC ответ
                    try {
                        val response = json.decodeFromString<McpResponse>(responseBody)
                        if (response.error != null) {
                            logger.w { "GitHub MCP API error: ${response.error.message}, using known tools" }
                            return Result.success(getKnownTools())
                        }
                        val tools = response.result?.tools ?: emptyList()
                        if (tools.isNotEmpty()) {
                            logger.d { "Successfully retrieved ${tools.size} tools from GitHub MCP Server" }
                            return Result.success(tools)
                        } else {
                            logger.d { "No tools in response, using known tools" }
                            return Result.success(getKnownTools())
                        }
                    } catch (e: Exception) {
                        logger.d { "Failed to parse GitHub MCP response: ${e.message}, using known tools" }
                        return Result.success(getKnownTools())
                    }
                }
                statusCode == HttpStatusCode.Unauthorized.value -> {
                    if (baseUrl.contains("api.githubcopilot.com")) {
                        logger.w { 
                            "Unauthorized access to GitHub MCP Server. " +
                            "Публичный endpoint требует OAuth 2.1 с PKCE. " +
                            "Используйте локальный сервер или реализуйте OAuth flow. " +
                            "Используем известные инструменты."
                        }
                    } else {
                        logger.w { "Unauthorized access to GitHub MCP Server, check your token, using known tools" }
                    }
                    return Result.success(getKnownTools())
                }
                statusCode == HttpStatusCode.BadRequest.value -> {
                    logger.w { "Bad request to GitHub MCP Server (may require stdio connection), using known tools" }
                    return Result.success(getKnownTools())
                }
                else -> {
                    logger.w { "HTTP error from GitHub MCP Server: $statusCode, using known tools" }
                    return Result.success(getKnownTools())
                }
            }
        } catch (e: Exception) {
            logger.w(e) { "Exception when trying to connect to GitHub MCP Server: ${e.message}, using known tools" }
            // В случае ошибки возвращаем известные инструменты из документации
            return Result.success(getKnownTools())
        }
    }
}

