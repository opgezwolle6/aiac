package com.raremartial.aiac.di

import com.raremartial.aiac.mcp.McpServer
import co.touchlab.kermit.Logger
import org.koin.dsl.module

/**
 * Модуль для MCP сервера (только для JVM)
 * 
 * ВАЖНО: Используем createdAtStart = true, чтобы сервер запускался сразу при инициализации Koin,
 * а не при первом запросе (lazy initialization).
 */
val mcpServerModule = module(createdAtStart = true) {
    single<McpServer> {
        // YandexTrackerApi должен быть доступен из networkModule
        // Используем getOrNull, чтобы не падать, если API недоступен
        val logger = Logger.withTag("McpServerModule")
        val trackerApi = try {
            get<com.raremartial.aiac.network.YandexTrackerApi>()
        } catch (e: Exception) {
            logger.w(e) { 
                "YandexTrackerApi not available, MCP server will work without tracker integration" 
            }
            null
        }
        
        val server = McpServer(
            port = 8080,
            trackerApi = trackerApi,
            defaultQueue = YandexTrackerConfig.DEFAULT_QUEUE
        )
        server.start()
        server
    }
}

