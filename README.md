# Mini HTTP Backend Service

Сервис реализован на Kotlin и поддерживает:
- регистрацию пользователей
- авторизацию с токеном
- CRUD для сообщений (создание, чтение, удаление)
- файловое хранение JSON
- многопоточную обработку клиентов
- логирование запросов

## Архитектура

Проект разделен на слои:
- `src/app/data` - модели `User`, `Session`, `Message`
- `src/app/repository` - чтение и запись JSON файлов, поиск, удаление, генерация ID
- `src/app/service` - бизнес-логика авторизации и доступа
- `src/app/controller` - HTTP маршрутизация и ответы
- `src/app/server` - TCP сервер, парсер HTTP, формирование HTTP ответов
- `src/app/util` - JSON утилиты и логгер

## Хранилище

При запуске создается папка `storage`:
- `storage/users.json`
- `storage/sessions.json`
- `storage/messages.json`
- `storage/server.log`

## Запуск

1. Откройте проект в IDE с Kotlin SDK.
2. Запустите `src/Main.kt`.
3. Сервер стартует на `http://localhost:8080`.

## HTTP API

### 1. Регистрация

`POST /register`

Body:
```json
{
  "username": "alex",
  "password": "12345"
}
```

Ответы:
- `201 Created` если пользователь создан
- `400 Bad Request` если пользователь уже существует или данные невалидны

### 2. Логин

`POST /login`

Body:
```json
{
  "username": "alex",
  "password": "12345"
}
```

Ответ:
```json
{
  "token": "random_token_value"
}
```

Коды:
- `200 OK` успешный вход
- `401 Unauthorized` неверные данные
- `400 Bad Request` некорректный JSON или пустые поля

### 3. Создание сообщения

`POST /messages`

Headers:
`Authorization: Bearer <token>`

Body:
```json
{
  "text": "Hello world"
}
```

Коды:
- `201 Created`
- `401 Unauthorized`
- `400 Bad Request`

### 4. Получение сообщений

`GET /messages`

Ответ: JSON массив сообщений.

Коды:
- `200 OK`

### 5. Удаление сообщения

`DELETE /messages/<id>`

Headers:
`Authorization: Bearer <token>`

Коды:
- `200 OK` удалено
- `401 Unauthorized` нет токена или неверный токен
- `403 Forbidden` сообщение принадлежит другому пользователю
- `404 Not Found` сообщение не найдено

## Логирование

Каждый запрос записывается в `storage/server.log` в формате:

`time method path user`

Пример:

`2025-02-10T13:00:00Z GET /messages alex`

## Обработка ошибок

Сервис возвращает корректные HTTP коды:
- `200 OK`
- `201 Created`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `500 Internal Server Error`

## Пример через curl

```bash
curl -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"alex\",\"password\":\"12345\"}"
```

## Автотесты

Добавлен автономный тест-раннер: `src/tests/TestRunner.kt`.

Проверяет:
- регистрацию и дубликаты пользователей
- логин, токен и проверку токена
- создание, чтение и удаление сообщений автором
- запрет удаления чужого сообщения и случай not found