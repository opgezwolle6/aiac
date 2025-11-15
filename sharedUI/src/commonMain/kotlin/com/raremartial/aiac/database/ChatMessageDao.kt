package com.raremartial.aiac.database

import com.raremartial.aiac.data.model.*
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlin.time.ExperimentalTime

/**
 * DAO для работы с сообщениями чата в базе данных
 */
class ChatMessageDao(
    private val database: ChatDatabase,
    private val json: Json
) {
    /**
     * Получить все сообщения, отсортированные по timestamp
     */
    suspend fun getAllMessages(): List<ChatMessage> {
        return database.chatDatabaseQueries.selectAll().executeAsList().map { row ->
            toChatMessage(row)
        }
    }

    /**
     * Получить сообщение по ID
     */
    suspend fun getMessageById(id: String): ChatMessage? {
        return database.chatDatabaseQueries.selectById(id).executeAsOneOrNull()?.let { row ->
            toChatMessage(row)
        }
    }

    /**
     * Вставить сообщение
     */
    @OptIn(ExperimentalTime::class)
    suspend fun insertMessage(message: ChatMessage) {
        database.chatDatabaseQueries.insertMessage(
            id = message.id,
            content = message.content,
            role = message.role.name,
            timestamp = message.timestamp.toEpochMilliseconds(),
            is_pending = if (message.isPending) 1 else 0,
            structured_data = message.structuredData?.let { json.encodeToString(StructuredResponse.serializer(), it) },
            solution_results = if (message.solutionResults.isNotEmpty()) {
                json.encodeToString(ListSerializer(SolutionResult.serializer()), message.solutionResults)
            } else null,
            comparison_analysis = message.comparisonAnalysis?.let { json.encodeToString(ComparisonAnalysis.serializer(), it) },
            selected_methods = if (message.selectedMethods.isNotEmpty()) {
                json.encodeToString(ListSerializer(SolutionMethod.serializer()), message.selectedMethods)
            } else null,
            token_usage = message.tokenUsage?.let { json.encodeToString(TokenUsage.serializer(), it) },
            is_summary = if (message.isSummary) 1 else 0
        )
    }

    /**
     * Обновить сообщение
     */
    @OptIn(ExperimentalTime::class)
    suspend fun updateMessage(message: ChatMessage) {
        database.chatDatabaseQueries.updateMessage(
            content = message.content,
            role = message.role.name,
            timestamp = message.timestamp.toEpochMilliseconds(),
            is_pending = if (message.isPending) 1 else 0,
            structured_data = message.structuredData?.let { json.encodeToString(StructuredResponse.serializer(), it) },
            solution_results = if (message.solutionResults.isNotEmpty()) {
                json.encodeToString(ListSerializer(SolutionResult.serializer()), message.solutionResults)
            } else null,
            comparison_analysis = message.comparisonAnalysis?.let { json.encodeToString(ComparisonAnalysis.serializer(), it) },
            selected_methods = if (message.selectedMethods.isNotEmpty()) {
                json.encodeToString(ListSerializer(SolutionMethod.serializer()), message.selectedMethods)
            } else null,
            token_usage = message.tokenUsage?.let { json.encodeToString(TokenUsage.serializer(), it) },
            is_summary = if (message.isSummary) 1 else 0,
            id = message.id
        )
    }

    /**
     * Удалить сообщение
     */
    suspend fun deleteMessage(id: String) {
        database.chatDatabaseQueries.deleteMessage(id)
    }

    /**
     * Удалить все сообщения
     */
    suspend fun deleteAll() {
        database.chatDatabaseQueries.deleteAll()
    }

    /**
     * Получить количество сообщений
     */
    suspend fun getMessageCount(): Long {
        return database.chatDatabaseQueries.countMessages().executeAsOne()
    }

    /**
     * Конвертирует строку БД в ChatMessage
     */
    @OptIn(ExperimentalTime::class)
    private fun toChatMessage(row: Chat_message): ChatMessage {
        return ChatMessage(
            id = row.id,
            content = row.content,
            role = MessageRole.valueOf(row.role),
            timestamp = Instant.fromEpochMilliseconds(row.timestamp),
            isPending = row.is_pending == 1L,
            structuredData = row.structured_data?.let {
                try {
                    json.decodeFromString(StructuredResponse.serializer(), it)
                } catch (e: Exception) {
                    null
                }
            },
            solutionResults = row.solution_results?.let {
                try {
                    json.decodeFromString(ListSerializer(SolutionResult.serializer()), it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList(),
            comparisonAnalysis = row.comparison_analysis?.let {
                try {
                    json.decodeFromString(ComparisonAnalysis.serializer(), it)
                } catch (e: Exception) {
                    null
                }
            },
            selectedMethods = row.selected_methods?.let {
                try {
                    json.decodeFromString(ListSerializer(SolutionMethod.serializer()), it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList(),
            tokenUsage = row.token_usage?.let {
                try {
                    json.decodeFromString(TokenUsage.serializer(), it)
                } catch (e: Exception) {
                    null
                }
            },
            isSummary = row.is_summary == 1L
        )
    }
}

