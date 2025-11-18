# Настройка OAuth для публичного GitHub MCP Server

## Обзор

Публичный GitHub MCP Server endpoint (`https://api.githubcopilot.com/mcp/`) использует **OAuth 2.1 с PKCE** для аутентификации. Это более безопасный способ авторизации по сравнению с Personal Access Token.

## Требования

- Подписка на **GitHub Copilot** или **Copilot Enterprise**
- OAuth приложение, зарегистрированное в GitHub
- Реализация OAuth flow в приложении

## OAuth 2.1 с PKCE Flow

### Шаг 1: Регистрация OAuth приложения

1. Перейдите в [GitHub Settings → Developer settings → OAuth Apps](https://github.com/settings/developers)
2. Нажмите "New OAuth App"
3. Заполните форму:
   - **Application name**: AIAC MCP Client
   - **Homepage URL**: `https://your-app.com` (или локальный URL)
   - **Authorization callback URL**: `your-app://oauth/callback` (для мобильного приложения) или `http://localhost:8080/oauth/callback` (для десктопа)
4. Сохраните **Client ID** и **Client Secret**

### Шаг 2: Генерация PKCE параметров

PKCE (Proof Key for Code Exchange) требует:

1. **Code Verifier**: случайная строка (43-128 символов)
2. **Code Challenge**: SHA256 хеш от Code Verifier (base64url encoded)

Пример генерации в Kotlin:

```kotlin
import java.security.MessageDigest
import java.util.Base64

fun generateCodeVerifier(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

fun generateCodeChallenge(verifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(verifier.toByteArray())
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
}
```

### Шаг 3: Инициация OAuth flow

1. Откройте браузер с URL авторизации:
```
https://github.com/login/oauth/authorize?
  client_id=YOUR_CLIENT_ID&
  redirect_uri=YOUR_REDIRECT_URI&
  scope=repo,read:org&
  response_type=code&
  code_challenge=CODE_CHALLENGE&
  code_challenge_method=S256&
  state=RANDOM_STATE
```

2. Пользователь авторизуется в GitHub
3. GitHub перенаправляет на `redirect_uri` с `code` и `state`

### Шаг 4: Обмен authorization code на access token

Отправьте POST запрос:

```kotlin
POST https://github.com/login/oauth/access_token
Content-Type: application/json

{
  "grant_type": "authorization_code",
  "code": "AUTHORIZATION_CODE",
  "redirect_uri": "YOUR_REDIRECT_URI",
  "client_id": "YOUR_CLIENT_ID",
  "code_verifier": "CODE_VERIFIER"
}
```

Ответ:
```json
{
  "access_token": "gho_...",
  "token_type": "Bearer",
  "scope": "repo,read:org"
}
```

### Шаг 5: Использование access token

Используйте полученный `access_token` для запросов к GitHub MCP Server:

```kotlin
headers {
    append(HttpHeaders.Authorization, "Bearer $accessToken")
}
```

## Реализация в проекте

### Текущий статус

Текущая реализация пытается использовать Personal Access Token с публичным endpoint. Если это не работает (ошибка 401/403), рекомендуется:

1. **Использовать локальный сервер** (вариант 3 в GITHUB_MCP_SETUP.md) - самый простой способ
2. **Реализовать полный OAuth flow** - для использования публичного endpoint

### План реализации OAuth

1. Создать `GitHubOAuthManager` для управления OAuth flow
2. Реализовать генерацию PKCE параметров
3. Добавить WebView или браузер для авторизации
4. Обработать redirect и получить authorization code
5. Обменять code на access token
6. Сохранить токен безопасно (например, в Keychain/Keystore)
7. Использовать токен для запросов к MCP Server

## Альтернативные решения

### Вариант 1: Использование локального сервера (рекомендуется)

Разверните GitHub MCP Server локально с PAT:

```bash
cd github-mcp-server
export GITHUB_PERSONAL_ACCESS_TOKEN='your-token'
./start-server.sh
```

Затем используйте:
```kotlin
baseUrl = "http://localhost:3000"
```

### Вариант 2: Использование известных инструментов

Если OAuth не реализован, используйте известные инструменты из документации:

```kotlin
baseUrl = null
```

## Документация

- [GitHub OAuth Apps](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps)
- [OAuth 2.1 с PKCE](https://oauth.net/2/pkce/)
- [GitHub MCP Server](https://github.com/github/github-mcp-server)
- [GitHub Copilot MCP Documentation](https://docs.github.com/en/copilot/how-tos/provide-context/use-mcp/use-the-github-mcp-server)

