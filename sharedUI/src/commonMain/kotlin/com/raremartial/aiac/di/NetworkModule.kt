package com.raremartial.aiac.di

import com.raremartial.aiac.network.HuggingFaceApi
import com.raremartial.aiac.network.HuggingFaceApiImpl
import com.raremartial.aiac.network.YandexGPTApi
import com.raremartial.aiac.network.YandexGPTApiImpl
import com.raremartial.aiac.network.GitHubMcpApi
import com.raremartial.aiac.network.GitHubMcpApiImpl
import io.ktor.client.HttpClient
import org.koin.dsl.module

val networkModule = module {
    
    single<HttpClient> {
        createHttpClient()
    }
    
    single<YandexGPTApi> {
        YandexGPTApiImpl(
            httpClient = get(),
            apiKey = YandexConfig.API_KEY,
            folderId = YandexConfig.FOLDER_ID
        )
    }
    
    single<HuggingFaceApi> {
        HuggingFaceApiImpl(
            httpClient = get(),
            apiKey = YandexConfig.HUGGINGFACE_API_KEY
        )
    }
    
    single<GitHubMcpApi> {
        GitHubMcpApiImpl(
            httpClient = get(),
            githubToken = GitHubConfig.PERSONAL_ACCESS_TOKEN,
            // Варианты baseUrl:
            // 1. null - использовать известные инструменты из документации (по умолчанию, рекомендуется)
            // 2. "https://api.githubcopilot.com/mcp" - публичный GitHub endpoint (требует OAuth 2.1 с PKCE)
            //    Примечание: Публичный endpoint может не работать с PAT, требуется реализация OAuth flow
            //    См. GITHUB_OAUTH_SETUP.md для подробностей
            // 3. "http://localhost:3000" - локальный сервер (рекомендуется для разработки)
            // 4. "https://your-mcp-server.com" - ваш собственный развернутый сервер
            baseUrl = "https://api.githubcopilot.com/mcp" // Пробуем публичный endpoint (может потребоваться OAuth)
        )
    }
}

