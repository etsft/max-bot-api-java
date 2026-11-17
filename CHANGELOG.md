# Журнал изменений

Здесь описаны заметные изменения проекта. Формат основан на
[Keep a Changelog](https://keepachangelog.com/ru/1.1.0/), версии следуют
[семантическому версионированию](https://semver.org/lang/ru/) (до 1.0 минорный релиз может
ломать совместимость).

## [0.7.0]

Инструкции по переходу — в README: [на русском](README_RU.md#переход-на-070),
[на английском](README.md#migrating-to-070).

### Добавлено

- Схема MAX Bot API 0.0.34: ответ HTTP 200 с `success:false` несёт машиночитаемый код отказа.
  Он доступен как `SimpleQueryResult.code()` и `AddMembersResult.code()` (`@Nullable`), например
  `not.found` или `chat.denied`. Определять отказ нужно по коду, а не по тексту `message`. При
  `success:false` кода может и не быть: если не удалось добавить отдельных пользователей
  (некоторых или всех), `addMembers` сообщает об этом только через `failedUserIds` и
  `failedUserDetails`. Отказ с HTTP 4xx, как и раньше, — `MaxApiException` с кодом в
  `errorCode()`.
- `MaxOperationFailedException` (`max-bot-api-client`, наследник `IllegalStateException`) с
  `errorCode()` и `errorMessage()`. Его выбрасывают `MaxWebhookServer.register()` и
  `unregister()`, когда MAX отвечает `success:false`.
- Живые тесты (`max-bot-api-integration-tests`): свойство `-Pit.class=<Имя>` запускает один
  класс; `SuccessFalseLiveTest` сверяет `code()` с реальными ответами MAX; в подсказках
  оператору печатается название чата.

### Изменено

- У record `SimpleQueryResult` и `AddMembersResult` появился компонент `code` (третьим по
  счёту). Прежние конструкторы сохранены, но деконструирующие паттерны вида
  `SimpleQueryResult(var success, var message)` и `AddMembersResult(var s, var m, var ids,
  var details)` перестанут компилироваться. `equals()`, `hashCode()` и `toString()` учитывают
  `code`.
- `MaxWebhookServer.register()` и `unregister()` выбрасывают `MaxOperationFailedException`
  вместо `IllegalStateException`. Это подкласс, поэтому существующие `catch` продолжат работать.
  Изменился текст `getMessage()`: после префикса `MAX API rejected webhook registration` (или
  `unregistration`) идут `code=…, message=…`, причём отсутствующие части опускаются. Например,
  без кода — `MAX API rejected webhook registration: message=Registration denied` вместо
  прежнего `…: Registration denied`.
- `MaxWebhookRegistrar` пишет в лог при отказе MAX и `code`, и `message`.

## [0.6.0]

Инструкции по переходу — в README: [на русском](README_RU.md#переход-на-060),
[на английском](README.md#migrating-to-060).

### Добавлено

- `max-bot-api-gson`: адаптер сериализации на Gson с тем же форматом JSON, что и у Jackson.
  Адаптеры находятся через `ServiceLoader` (`MaxSerializerProvider`); если на classpath есть
  оба, выбирается Jackson.
- Схема MAX Bot API 0.0.33: `BotAdminPermissionsChangedUpdate`, `GetMessagesQuery.after()` /
  `before()`, `SendCommentQuery.disableLinkPreview()`, `CommentMessage.stat`.
- `NewMessageBody.text(...)`, `NewMessageBody.attachments(...)` и методы `with*`;
  `InlineKeyboardAttachmentRequest.builder()`; `CallbackButton.of(text, payload)`.
- `UpdateRouter`: `UpdateHandler` с отдельным обработчиком на каждый тип апдейта.
- `MaxLongPollingConsumer.Builder.dispatchExecutor(...)`: обработчик выполняется вне потока
  опроса.
- `MaxBotAPI.uploads()`: `MaxUploadAPI` с сериализатором и настройками API. `MaxUploadAPI`
  использует заданные connect timeout и SSL-контекст и ограничивает время каждой загрузки
  (по умолчанию 10 минут).
- Ограничение размера тела webhook-запроса, по умолчанию 1 МБ: `max.bot.webhook.max-body-size`,
  `MaxWebhookServer.Builder.maxBodySize(...)`.
- `RetryPolicy.shouldRetry(HttpMethod, int)`, `RetryPolicy.maxRetryAfter()`,
  `MaxClient.config()`, `ModelTypes.rawJsonOf(...)`.
- `Automatic-Module-Name` в каждом публикуемом jar (`ru.max.botapi.core`,
  `ru.max.botapi.client`, ...).

### Изменено

- `Chat.type`, `Chat.status`, `MessageRecipient.chatType`, `LinkedMessage.type` и
  `CommentLinkedMessage.type` стали `@Nullable`: значение, незнакомое этой версии, читается как
  `null` и не роняет весь ответ.
- `null`-элементы в списках из ответов (незнакомое значение enum, JSON `null`) отбрасываются и
  не роняют весь ответ.
- `VideoAttachmentDetails.thumbnail` имеет тип `VideoAttachmentDetails.Thumbnail`;
  `BotCommand.description` необязателен.
- HTTP 503 повторяется только для `GET`, `PUT` и `DELETE`. `Retry-After` дольше 60 секунд не
  ожидается: сразу бросается `MaxRateLimitException` со значением от сервера.
- `max.bot.webhook.auto-unregister` по умолчанию `false`, чтобы rolling-деплой не удалял общий
  webhook.
- `max.bot.mode` принимает любое написание, которое Spring Boot привязывает к enum
  (`long-polling`, `longPolling`, ...). Если режим выбран, но нет бина `MaxBotAPI` или
  `UpdateHandler`, в лог пишется предупреждение.
- `Update` сериализуется с `update_type`; значения `Unknown*` — исходным JSON.
- `max-bot-api-longpolling` и `max-bot-api-webhook` отдают `core` и `client` как зависимости
  со scope `compile`.
- `MaxWebhookServer.getPort()` после старта возвращает реально занятый порт.
- Эндпоинт webhook в Spring — `MaxWebhookController.receiveWebhook(InputStream, String)`.
- Зависимости: Jackson 3.3.0, Spring Boot 4.1.2, Gradle 9.9.1.

### Исправлено

- Идентификаторы сообщений, комментариев и видео кодируются в пути запроса.
- Цикл long polling больше не останавливается, если исключение бросает обработчик ошибок, а
  `isRunning()` возвращает `false`, когда цикл завершился из-за `Error`.
- `new MaxBotAPI(client).config()` возвращает конфигурацию клиента, а не настройки по
  умолчанию.
- Jackson, как и Gson, отклоняет объект или массив на месте enum.
- Имя файла в multipart экранируется; `close()` останавливает `HttpClient`; сообщение об ошибке
  с экранированными символами читается целиком.
- Секрет webhook проверяется до чтения тела запроса.

## [0.5.0] и ранее

См. [теги](https://github.com/etsft/max-bot-api-java/tags) и разделы о переходе в README.

[0.7.0]: https://github.com/etsft/max-bot-api-java/compare/v0.6.0...v0.7.0
[0.6.0]: https://github.com/etsft/max-bot-api-java/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/etsft/max-bot-api-java/releases/tag/v0.5.0
