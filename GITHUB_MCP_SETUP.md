# Настройка GitHub MCP Server с удаленным SSE Endpoint

## Варианты подключения

### 1. Использование известных инструментов (по умолчанию)

Если `baseUrl = null`, приложение использует список известных инструментов из документации GitHub MCP Server. Это самый простой вариант и не требует настройки сервера.

### 2. Публичный GitHub Endpoint

GitHub предоставляет публичный SSE endpoint:
```
https://api.githubcopilot.com/mcp
```

**Важно:** 
- Этот endpoint использует **OAuth 2.1 с PKCE** для аутентификации
- Требуется подписка на **GitHub Copilot** или **Copilot Enterprise**
- OAuth требует интерактивной авторизации через браузер

#### Вариант A: Использование OAuth (рекомендуется)

Публичный endpoint GitHub использует OAuth 2.1 с PKCE. Для полноценной поддержки OAuth требуется:

1. **Регистрация OAuth приложения** в GitHub:
   - Перейдите в Settings → Developer settings → OAuth Apps
   - Создайте новое OAuth приложение
   - Укажите Redirect URI для вашего приложения

2. **Реализация OAuth flow**:
   - Открытие браузера для авторизации
   - Получение authorization code через redirect URI
   - Обмен code на access token
   - Хранение и обновление токена

**Текущая реализация:** Приложение пытается использовать Personal Access Token с публичным endpoint. Если это не работает, используйте локальный сервер (вариант 3) или реализуйте полный OAuth flow.

#### Вариант B: Попытка использования PAT (может не работать)

1. Обновите `NetworkModule.kt`:
```kotlin
baseUrl = "https://api.githubcopilot.com/mcp"
```

2. Текущая реализация использует Bearer token с PAT. Если получаете ошибку 401/403, используйте локальный сервер или реализуйте OAuth.

### 3. Развертывание собственного сервера

Вы можете развернуть GitHub MCP Server на своем сервере:

#### Вариант A: Использование Docker

```bash
docker run -i --rm \
  -e GITHUB_PERSONAL_ACCESS_TOKEN=<your-token> \
  -p 3000:3000 \
  ghcr.io/github/github-mcp-server
```

Затем используйте:
```kotlin
baseUrl = "http://your-server.com:3000"
```

#### Вариант B: Использование бинарного файла

1. Скачайте бинарный файл с [GitHub Releases](https://github.com/github/github-mcp-server/releases)
2. Запустите сервер с SSE поддержкой:
```bash
./github-mcp-server --sse-port 3000
```

3. Используйте URL вашего сервера:
```kotlin
baseUrl = "http://your-server.com:3000"
```

## Настройка в проекте

Чтобы использовать удаленный SSE endpoint, обновите `NetworkModule.kt`:

```kotlin
single<GitHubMcpApi> {
    GitHubMcpApiImpl(
        httpClient = get(),
        githubToken = GitHubConfig.PERSONAL_ACCESS_TOKEN,
        baseUrl = "https://api.githubcopilot.com/mcp" // или ваш сервер
    )
}
```

## Примечания

- Публичный endpoint GitHub (`api.githubcopilot.com`) **требует OAuth 2.1 с PKCE** (не PAT)
- Для собственного сервера убедитесь, что он доступен извне и настроен для SSE
- SSE endpoint обычно доступен по пути `/sse` от базового URL
- При ошибках подключения приложение автоматически использует известные инструменты из документации

## OAuth аутентификация

Для использования публичного GitHub endpoint требуется реализация OAuth 2.1 с PKCE. 

**Подробная документация:** См. [GITHUB_OAUTH_SETUP.md](./GITHUB_OAUTH_SETUP.md)

**Рекомендация:** Для разработки используйте локальный сервер (вариант 3), который работает с Personal Access Token.

## Документация

- [GitHub MCP Server](https://github.com/github/github-mcp-server)
- [GitHub Copilot MCP Documentation](https://docs.github.com/en/copilot/how-tos/provide-context/use-mcp/use-the-github-mcp-server)
- [OAuth 2.1 с PKCE](https://oauth.net/2/pkce/)

