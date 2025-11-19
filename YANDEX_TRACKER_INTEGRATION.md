# Интеграция с Яндекс.Трекером - Требуемые данные

## Анализ документации

На основе документации [Яндекс.Трекер API](https://yandex.ru/support/tracker/ru/common-format) и предоставленного токена, для завершения интеграции нужны следующие данные:

## ✅ Уже есть:

1. **IAM токен**: `t1.9euelZqJm8eclpaXipbGmZSblpnGxu3rnpWajJGcmZyQk5eLkpqclZyRlInl8_d2Zlg3-e8RJCQr_d3z9zYVVjf57xEkJCv9zef1656VmsrGmMfIj56OjIvLipqLmcuR7_zF656VmsrGmMfIj56OjIvLipqLmcuR.eQbA-X3_Qwool1R7kZWZlpskLKStQntMdrmUrd6qjjOd-scIXFd7ikjRngoj2gCJ6oXVMplyFhwF3VZrEk1OAA`
   - Формат: `Authorization: Bearer <IAM-токен>`

2. **Базовый URL API**: `https://api.tracker.yandex.net/v3/`

## ❓ Нужно получить:

### 1. Идентификатор организации (ОБЯЗАТЕЛЬНО)

**Вопрос:** Какой идентификатор организации использовать?

**Варианты:**
- `X-Org-ID` — если используется Яндекс 360 для бизнеса
- `X-Cloud-Org-ID` — если используется Yandex Cloud Organization

**Как узнать:**
1. Перейдите в Яндекс.Трекер
2. Откройте **Администрирование** → **Организации**
3. Скопируйте значение поля **идентификатор**

**Пример:** `X-Org-ID: 12345678` или `X-Cloud-Org-ID: abc123def456`

### 2. Ключ очереди по умолчанию (ОПЦИОНАЛЬНО, но рекомендуется)

**Вопрос:** Какой ключ очереди использовать по умолчанию?

**Как узнать:**
1. Перейдите в Яндекс.Трекер
2. Откройте нужную очередь
3. Скопируйте ключ очереди (например, `TEST`, `TREK`, `PROJ`)

**Пример:** `queue: "TEST"`

**Примечание:** Если не указан, можно использовать первую доступную очередь или запрашивать у пользователя.

### 3. Тип организации (для правильного заголовка)

**Вопрос:** Какая организация используется?
- Яндекс 360 для бизнеса → используем `X-Org-ID`
- Yandex Cloud Organization → используем `X-Cloud-Org-ID`

## Формат запросов

### Получение количества задач

```http
POST https://api.tracker.yandex.net/v3/issues/_search
Authorization: Bearer <IAM-токен>
X-Org-ID: <идентификатор_организации>
Content-Type: application/json

{
  "filter": {
    "queue": "<ключ_очереди>"  // опционально
  }
}
```

**Ответ:**
- Заголовок `X-Total-Count` содержит общее количество задач
- Тело ответа содержит массив задач

### Создание задачи

```http
POST https://api.tracker.yandex.net/v3/issues/
Authorization: Bearer <IAM-токен>
X-Org-ID: <идентификатор_организации>
Content-Type: application/json

{
  "queue": "<ключ_очереди>",
  "summary": "<название_задачи>",
  "description": "<описание_задачи>",
  "type": "task"  // или "bug", "feature" и т.д.
}
```

**Ответ:**
- Созданная задача с полями, включая `key` (ID задачи)

## Следующие шаги

После получения этих данных:
1. Добавлю токен и идентификатор организации в `Secrets.kt`
2. Создам API клиент для Яндекс.Трекера
3. Интегрирую реальные вызовы API в MCP сервер
4. Заменю mock данные на реальные запросы

## Ссылки на документацию

- [Общий формат запросов](https://yandex.ru/support/tracker/ru/common-format)
- [Задачи API](https://yandex.ru/support/tracker/ru/issues)
- [MCP Hub для Трекера](https://yandex.cloud/ru/docs/ai-studio/concepts/mcp-hub/templates#tracker)
- [Проект на GitHub](https://github.com/aikts/yandex-tracker-mcp)

