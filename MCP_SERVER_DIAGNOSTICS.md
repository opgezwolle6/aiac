# Диагностика проблемы подключения к MCP серверу

## Анализ текущей реализации

### Проблема
Ошибка `Connection refused` при попытке подключиться к MCP серверу на `localhost:8080`.

### Возможные причины

#### 1. **Порядок инициализации модулей Koin**
- `mcpServerModule` загружается последним в `KoinInit.jvm.kt`
- `CustomMcpApi` создается в `networkModule`, который загружается раньше
- Сервер запускается асинхронно (`wait = false`), но может не успеть запуститься до создания `CustomMcpApi`

#### 2. **Асинхронный запуск сервера**
- `McpServer.start()` использует `embeddedServer(...).start(wait = false)`
- Задержка в 200ms может быть недостаточной
- Сервер может не успеть начать слушать порт до подключения

#### 3. **Порт 8080 может быть занят**
- Другой процесс может использовать порт 8080
- Нужно проверить: `lsof -i :8080` или `netstat -an | grep 8080`

#### 4. **Токен Яндекс.Трекера**
- В `Secrets.kt` токен изменился на более короткий: `y0__xCVrtNfGIvXOyClkd6cFTDUlPqUCFLvBunZO9smSL7xBEGI5XtTD8gu`
- Это может быть OAuth токен, а не IAM токен
- **ВАЖНО**: Для API Яндекс.Трекера нужен IAM токен в формате `t1.9euelZqJm8eclpaXipbGmZSblpnGxu3rnpWajJGcmZyQk5eLkpqclZyRlInl8_d2Zlg3-e8RJCQr_d3z9zYVVjf57xEkJCv9zef1656VmsrGmMfIj56OjIvLipqLmcuR7_zF656VmsrGmMfIj56OjIvLipqLmcuR.eQbA-X3_Qwool1R7kZWZlpskLKStQntMdrmUrd6qjjOd-scIXFd7ikjRngoj2gCJ6oXVMplyFhwF3VZrEk1OAA`
- Но это не влияет на запуск MCP сервера, только на работу с API Яндекс.Трекера

## Что нужно проверить

### 1. Проверить, запускается ли сервер
Добавьте логирование в `McpServer.start()` и проверьте логи при запуске приложения:
- Должно быть сообщение: "Starting MCP Server on port 8080"
- Должно быть сообщение: "MCP Server started on http://localhost:8080"

### 2. Проверить, занят ли порт 8080
```bash
# macOS/Linux
lsof -i :8080
# или
netstat -an | grep 8080
```

### 3. Проверить, доступен ли сервер
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":null}'
```

### 4. Проверить логи приложения
- Ищите сообщения с тегом "McpServer"
- Ищите сообщения с тегом "CustomMcpApi"
- Ищите сообщения с тегом "ChatViewModel"

## ✅ ИСПРАВЛЕНО: Основная проблема

### Проблема: Ленивая инициализация Koin
Koin использует **ленивую инициализацию (lazy initialization)** для `single` зависимостей. Это означает, что `McpServer` создавался только при первом запросе через `get<McpServer>()`. Но никто не запрашивал `McpServer` напрямую, поэтому сервер никогда не запускался!

### Решение
Добавлен параметр `createdAtStart = true` в `mcpServerModule`:
```kotlin
val mcpServerModule = module(createdAtStart = true) {
    single<McpServer> {
        // Сервер теперь запускается сразу при инициализации Koin
        server.start()
        server
    }
}
```

Теперь сервер запускается **сразу при инициализации Koin**, а не при первом запросе.

## Дополнительные улучшения

1. ✅ **Увеличена задержка запуска сервера** до 1 секунды
2. ✅ **Увеличена задержка в ViewModel** до 2 секунд
3. ✅ **Добавлена проверка статуса сервера** после запуска
4. ✅ **Улучшено логирование** для диагностики
5. ✅ **Добавлено отображение ошибки в UI** с инструкциями
6. ✅ **Исправлена ленивая инициализация** - сервер запускается сразу

## Что проверить сейчас

1. **Запустите приложение** - сервер должен запуститься автоматически
2. **Проверьте логи** - должны быть сообщения:
   - "Starting MCP Server on port 8080"
   - "MCP Server started successfully on http://localhost:8080"
3. **Проверьте UI** - должна появиться карточка с командами MCP или сообщение об ошибке
4. **Проверьте порт** (если проблема сохраняется):
   ```bash
   lsof -i :8080
   ```

## Проверка токена Яндекс.Трекера

Текущий токен в `Secrets.kt`: `y0__xCVrtNfGIvXOyClkd6cFTDUlPqUCFLvBunZO9smSL7xBEGI5XtTD8gu`

Это похоже на **OAuth токен**, а не IAM токен. Для API Яндекс.Трекера нужен **IAM токен** в формате:
- Начинается с `t1.`
- Очень длинный (более 200 символов)

Если у вас OAuth токен, его нужно обменять на IAM токен или использовать заголовок `Authorization: OAuth <токен>` вместо `Authorization: Bearer <токен>`.

