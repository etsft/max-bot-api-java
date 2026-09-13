# MAX Bot API Java Client

[![Maven Central](https://img.shields.io/maven-central/v/ru.etsft.max/max-bot-api-client.svg)](https://central.sonatype.com/artifact/ru.etsft.max/max-bot-api-client)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/projects/jdk/21/)

Современная клиентская библиотека Java 21 для [MAX Messenger Bot API](https://dev.max.ru/docs-api).

---

## Обзор

Библиотека предоставляет полноценный, идиоматичный интерфейс Java 21 к поддерживаемым методам MAX Bot API. Она спроектирована как лёгкая, типобезопасная и совместимая с будущими версиями API — модуль ядра не имеет зависимостей за пределами JDK.

**Основные возможности:**

- **Java 21 records** для всех DTO-типов — иммутабельные, компактные, без шаблонного кода.
- **Sealed interfaces** для union-типов (`Attachment`, `Button`, `Update`) с полной поддержкой сопоставления с образцом (pattern matching).
- **Виртуальные потоки** для неблокирующего ввода-вывода как в режиме long polling, так и в режиме webhook.
- **Транспорт `java.net.http.HttpClient`** — внешние HTTP-библиотеки не требуются.
- **Fluent query builders** для поддерживаемых методов API с поддержкой синхронного (`execute()`) и асинхронного (`enqueue()`) вызова.
- **Прямая совместимость при десериализации** — неизвестные типы порождают резервные записи `Unknown*` вместо ошибок разбора.
- **Встроенный ограничитель частоты запросов** (token bucket, 30 rps) и **политика повторных попыток** (экспоненциальный откат при HTTP 429/503).
- **Потоковая загрузка файлов** — большие файлы передаются без буферизации в куче; сообщение со свежезагруженным вложением повторяется автоматически, пока MAX его не обработает.
- **670+ модульных тестов**, покрытие строк по JaCoCo ≥ 85% / покрытие ветвей ≥ 80%, а также живой набор тестов против реального API.

---

## Требования

- **JDK 21** или новее
- Любая система сборки, работающая с артефактами Maven Central (Gradle, Maven, …)

---

## Быстрый старт

### Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
    implementation("ru.etsft.max:max-bot-api-client:0.4.1")
    // Сериализатор по умолчанию: MaxBotAPI.create(...) ищет его в classpath
    implementation("ru.etsft.max:max-bot-api-jackson:0.4.1")
    implementation("ru.etsft.max:max-bot-api-longpolling:0.4.1")

    // Опционально: поддержка webhook
    // implementation("ru.etsft.max:max-bot-api-webhook:0.4.1")

    // Опционально: автоконфигурация Spring Boot (webhook + long polling)
    // implementation("ru.etsft.max:max-bot-api-spring-boot:0.4.1")
}
```

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>ru.etsft.max</groupId>
        <artifactId>max-bot-api-client</artifactId>
        <version>0.4.1</version>
    </dependency>
    <dependency>
        <groupId>ru.etsft.max</groupId>
        <artifactId>max-bot-api-jackson</artifactId>
        <version>0.4.1</version>
    </dependency>
    <dependency>
        <groupId>ru.etsft.max</groupId>
        <artifactId>max-bot-api-longpolling</artifactId>
        <version>0.4.1</version>
    </dependency>
    <!-- Опционально: поддержка webhook -->
    <!--
    <dependency>
        <groupId>ru.etsft.max</groupId>
        <artifactId>max-bot-api-webhook</artifactId>
        <version>0.4.1</version>
    </dependency>
    -->
    <!-- Опционально: автоконфигурация Spring Boot (webhook + long polling) -->
    <!--
    <dependency>
        <groupId>ru.etsft.max</groupId>
        <artifactId>max-bot-api-spring-boot</artifactId>
        <version>0.4.1</version>
    </dependency>
    -->
</dependencies>
```

`MaxBotAPI` и `MaxUploadAPI` держат HTTP-клиент и реализуют `AutoCloseable`: закрывайте их при остановке бота, например через try-with-resources.

---

## Примеры использования

Полные запускаемые версии этих фрагментов лежат в [`max-bot-api-examples`](max-bot-api-examples/src/main/java/ru/max/botapi/examples). Запуск:

```bash
export MAX_BOT_TOKEN="your-bot-token"
./gradlew :max-bot-api-examples:run -PmainClass=ru.max.botapi.examples.KeyboardBot
```

### EchoBot (Long Polling)

```java
MaxBotAPI api = MaxBotAPI.create("your-token");

MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
    .api(api)
    .handler(update -> {
        if (update instanceof MessageCreatedUpdate msg) {
            // Сообщение, которое только пересылает другое, не имеет собственного body.
            var body = msg.message().body();
            String text = body == null ? null : body.text();
            Long chatId = msg.message().recipient().chatId();
            if (text != null && chatId != null) {
                api.sendMessage(new NewMessageBody(text, null, null, null, null))
                    .chatId(chatId)
                    .execute();
            }
        }
    })
    .build();

consumer.start();
```

### Отправка сообщения со встроенной клавиатурой

```java
List<List<Button>> buttons = List.of(
    List.of(
        new CallbackButton("Yes", "btn_yes", ButtonIntent.POSITIVE),
        new CallbackButton("No",  "btn_no",  ButtonIntent.NEGATIVE)
    )
);

InlineKeyboardAttachment.KeyboardPayload payload =
    new InlineKeyboardAttachment.KeyboardPayload(buttons);
InlineKeyboardAttachmentRequest keyboard = new InlineKeyboardAttachmentRequest(payload);

api.sendMessage(new NewMessageBody("Choose:", List.of(keyboard), null, null, null))
    .chatId(chatId)
    .execute();
```

### Обработка нажатий кнопок (callback)

Используйте сопоставление с образцом для sealed-типа `Update` для диспетчеризации по виду события:

```java
switch (update) {
    case MessageCallbackUpdate cb -> {
        String callbackId = cb.callback().callbackId();
        api.answerOnCallback(new CallbackAnswer(null, "Answered!"), callbackId).execute();
    }
    case MessageCreatedUpdate msg -> { /* обработать новое сообщение */ }
    default -> { /* игнорировать неизвестные типы обновлений */ }
}
```

`callback().payload()` равен `null`, если у кнопки нет payload, — проверяйте это до `switch` по нему.

### Модерация комментариев в канале

Комментарии к посту в канале — отдельная группа методов. Бот должен быть администратором канала:
`READ_ALL_MESSAGES` — чтобы читать комментарии, `WRITE` — публиковать, `EDIT` — редактировать,
`DELETE` — удалять. Комментарии не содержат вложений и не пересылаются, поэтому для них
используются `NewCommentBody` и `CommentMessage`, а не типы обычных сообщений.

```java
String postId = "mid.abc123";

// Опубликовать комментарий и прочитать страницу комментариев.
SendCommentResult posted = api.sendComment(new NewCommentBody("Спасибо!"), postId).execute();
CommentList comments = api.getComments(postId).count(50).execute();

// Один комментарий по его идентификатору.
CommentMessage one = api.getCommentById(postId, posted.message().body().mid()).execute();

// Редактирование и удаление. Удалённый комментарий восстановить нельзя.
api.editComment(new NewCommentBody("Большое спасибо!"), postId, one.body().mid()).execute();
api.deleteComment(postId, one.body().mid()).execute();
```

Чтобы реагировать на комментарии, подпишитесь на `COMMENT_CREATED`, `COMMENT_EDITED` и
`COMMENT_REMOVED`. Пост, к которому относится комментарий, — в `message.recipient().postId()`.
Чтобы ответить на комментарий, передайте `new NewMessageLink(MessageLinkType.REPLY, comment.body().mid())`
как `link` в `NewCommentBody`. Собственные комментарии бота тоже приходят как `comment_created`,
поэтому отвечающий бот должен их пропускать — см. [`CommentsBot`](max-bot-api-examples/src/main/java/ru/max/botapi/examples/CommentsBot.java).

### Команды бота

```java
api.editMyCommands(new BotCommandsPatch(List.of(
        new BotCommand("start", "Запустить бота"),
        new BotCommand("help",  "Показать справку")))).execute();

// Пустой список удаляет все команды.
api.editMyCommands(new BotCommandsPatch(List.of())).execute();
```

### Загрузка файла

Загрузка файла выполняется в два шага: сначала запрашивается URL для загрузки через API, затем файл передаётся потоком на этот URL. Форма ответа и тип результата зависят от `UploadType` — см. [Загрузка файлов](#загрузка-файлов) ниже.

```java
try (MaxUploadAPI uploadApi = new MaxUploadAPI()) {
    // Шаг 1: запросить endpoint для загрузки
    UploadEndpoint endpoint = api.getUploadUrl(UploadType.FILE).execute();

    // Шаг 2: передать файл потоком (без буферизации в куче)
    FileUploadedInfo info = uploadApi.uploadFile(endpoint, Path.of("file.txt"), "file.txt");

    // Шаг 3: прикрепить полученный токен к сообщению. MAX обрабатывает загрузку
    // асинхронно; execute() повторяет отправку, пока вложение не будет готово.
    api.sendMessage(new NewMessageBody(
        "File:",
        List.of(new FileAttachmentRequest(new MediaRequestPayload(info.token()))),
        null, null, null
    )).chatId(chatId).execute();
}
```

---

## Модули

| Модуль | Описание |
|---|---|
| `max-bot-api-core` | Модельные records, sealed interfaces, SPI сериализатора. Нет внешних зависимостей (только JDK). |
| `max-bot-api-client` | HTTP-транспорт (`java.net.http`), `MaxClient`, фасад `MaxBotAPI`, ограничитель частоты, политика повторных попыток. |
| `max-bot-api-jackson` | Адаптер сериализатора Jackson 2.x с кастомными десериализаторами для полиморфных типов. |
| `max-bot-api-gson` | Зарезервирован под адаптер сериализатора Gson; реализации пока нет. |
| `max-bot-api-longpolling` | Потребитель long polling на базе виртуальных потоков с экспоненциальным откатом. |
| `max-bot-api-webhook` | Встроенный HTTP/HTTPS-сервер webhook (JDK `HttpServer`) с проверкой секретного заголовка. |
| `max-bot-api-test-support` | WireMock-заглушки, JSON-фикстуры и вспомогательные классы для интеграционных тестов. |
| `max-bot-api-integration-tests` | Ручной набор тестов против боевого API с реальным токеном бота. Исключён из `build` и CI — см. [README модуля](max-bot-api-integration-tests/README.md). |
| `max-bot-api-spring-boot` | Автоконфигурация Spring Boot для режимов webhook и long polling — контроллер, регистрация подписки, управление жизненным циклом. |
| `max-bot-api-examples` | Запускаемые примеры: `EchoBot`, `KeyboardBot`, `FileUploadBot`, `ImageUploadBot`, `VideoUploadBot`, `AudioUploadBot`, `CommentsBot`, `WebhookBot`. |

---

## Long Polling

`MaxLongPollingConsumer` управляет циклом опроса в виртуальном потоке. При временных сбоях соединение восстанавливается автоматически с применением экспоненциального отката.

```java
MaxBotAPI api = MaxBotAPI.create("your-token");

MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
    .api(api)
    .handler(update -> {
        // диспетчеризация по sealed-типу Update
        if (update instanceof MessageCreatedUpdate msg) {
            // обработать сообщение
        }
    })
    .onError(e -> log.error("Ошибка опроса", e))  // опционально; по умолчанию — логирование на уровне WARN
    .pollTimeout(30)                               // опционально; секунды, по умолчанию longPollTimeout
    .build();

consumer.start();   // неблокирующий вызов; опрос выполняется в виртуальном потоке
// ...
consumer.stop();    // корректное завершение работы
```

Потребитель вызывает `getUpdates` в цикле, отслеживая маркер, возвращённый каждым ответом, чтобы исключить повторную доставку событий. Ошибки передаются в коллбэк `.onError()`; если он не задан, они логируются на уровне WARN. Цикл никогда не останавливается из-за ошибки:

- Если не удался сам `getUpdates` (сетевой сбой, ошибка API), цикл повторяет запрос с экспоненциальным откатом: 1 с → 2 с → 4 с → 8 с → 16 с → 30 с (максимум). Успешный опрос сбрасывает откат.
- Если исключение выбросил обработчик, потребитель без паузы переходит к следующему обновлению. Маркер всё равно продвигается, поэтому это обновление повторно не придёт.

Обработчик выполняется в потоке опроса, и следующий опрос ждёт его завершения. Долгую работу передавайте в отдельный executor.

Для получения только определённых типов событий используйте `.types()` с константами `UpdateType`:

```java
consumer = MaxLongPollingConsumer.builder()
    .api(api)
    .handler(handler)
    .types(Set.of(UpdateType.MESSAGE_CREATED, UpdateType.MESSAGE_CALLBACK))
    .build();
```

### Доступные типы обновлений

| Константа `UpdateType` | Значение API | Описание |
|---|---|---|
| `MESSAGE_CREATED` | `message_created` | Новое сообщение отправлено в чат |
| `MESSAGE_CALLBACK` | `message_callback` | Пользователь нажал кнопку встроенной клавиатуры |
| `MESSAGE_EDITED` | `message_edited` | Существующее сообщение отредактировано |
| `MESSAGE_REMOVED` | `message_removed` | Сообщение удалено |
| `BOT_ADDED` | `bot_added` | Бот добавлен в чат |
| `BOT_REMOVED` | `bot_removed` | Бот удалён из чата |
| `USER_ADDED` | `user_added` | Пользователь добавлен в чат |
| `USER_REMOVED` | `user_removed` | Пользователь удалён из чата |
| `BOT_STARTED` | `bot_started` | Пользователь начал прямой диалог с ботом |
| `BOT_STOPPED` | `bot_stopped` | Пользователь остановил (заблокировал) бота |
| `CHAT_TITLE_CHANGED` | `chat_title_changed` | Изменился заголовок чата |
| `MESSAGE_CONSTRUCTION_REQUEST` | `message_construction_request` | Запрошена сессия конструирования сообщения |
| `MESSAGE_CONSTRUCTED` | `message_constructed` | Сессия конструирования сообщения завершена |
| `MESSAGE_CHAT_CREATED` | `message_chat_created` | Чат создан через сообщение |
| `DIALOG_CLEARED` | `dialog_cleared` | Пользователь очистил историю диалога с ботом |
| `DIALOG_MUTED` | `dialog_muted` | Пользователь отключил уведомления в диалоге с ботом |
| `DIALOG_UNMUTED` | `dialog_unmuted` | Пользователь включил уведомления в диалоге с ботом |
| `DIALOG_REMOVED` | `dialog_removed` | Пользователь удалил диалог с ботом (приходит вместе с `bot_stopped`) |
| `COMMENT_CREATED` | `comment_created` | Опубликован новый комментарий к посту в канале |
| `COMMENT_EDITED` | `comment_edited` | Комментарий к посту в канале изменён |
| `COMMENT_REMOVED` | `comment_removed` | Комментарий к посту в канале удалён |

`message_construction_request`, `message_constructed` и `message_chat_created` достались
библиотеке от TamTam API и в документации MAX не описаны. Они помечены `@Deprecated` и
оставлены только для того, чтобы ответ, который их всё ещё содержит, десериализовался.

---

## Webhooks

`MaxWebhookServer` — встроенный сервер (JDK `HttpServer`), который принимает POST-запросы от платформы MAX и передаёт каждое обновление обработчику. Если задан секрет, запросы без совпадающего заголовка `X-Max-Bot-Api-Secret` отклоняются с кодом 401.

```java
MaxWebhookServer server = MaxWebhookServer.builder()
    .handler(update -> {
        // обработать обновление
    })
    .serializer(api.serializer())
    .secret("my-secret")   // register() также передаёт его в MAX
    .port(8443)            // по умолчанию 8443
    .path("/webhook")      // по умолчанию /webhook
    .build();

server.start();            // обычный HTTP; для HTTPS — start(sslContext)

// Подписка. Передайте множество констант UpdateType или null, чтобы получать все типы.
server.register(api, "https://example.com/webhook",
    Set.of(UpdateType.MESSAGE_CREATED, UpdateType.MESSAGE_CALLBACK));
```

См. [таблицу типов обновлений](#доступные-типы-обновлений) выше.

MAX доставляет webhook только на HTTPS-адреса. Либо терминируйте TLS перед сервером обратным прокси (nginx, балансировщик) и вызывайте `start()`, либо передайте в `start(sslContext)` `SSLContext` с вашим сертификатом. `register()` отправляет в MAX настроенный секрет вместе с подпиской, поэтому MAX добавляет его в каждую доставку.

`register()` и `unregister()` выбрасывают `IllegalStateException`, если MAX отвечает `success: false`. Полный пример бота, который подписывается при запуске и отписывается при остановке, — [`WebhookBot`](max-bot-api-examples/src/main/java/ru/max/botapi/examples/WebhookBot.java).

#### Срок подтверждения доставки

MAX ждёт HTTP 200 не дольше **30 секунд**. Более медленный ответ или любой другой код считается ошибкой доставки: MAX повторяет попытку до 10 раз с растущим интервалом (60 с, 150 с, 375 с, …) и через 8 часов без успешного ответа отписывает бота.

По умолчанию обработчик выполняется до отправки ответа. Если обработка может длиться так долго, передайте серверу executor: тогда сервер отвечает сразу, а обработчик запускается после ответа.

```java
ExecutorService dispatch = Executors.newVirtualThreadPerTaskExecutor();

MaxWebhookServer server = MaxWebhookServer.builder()
    // ...
    .dispatchExecutor(dispatch)
    .build();
```

В обоих режимах сервер отвечает 200, даже если обработчик выбросил исключение, поэтому MAX не доставит повторно обновление, на котором обработчик упал. Если обновление нельзя потерять, пусть обработчик сохраняет его (в очередь, в таблицу) и обрабатывает уже оттуда.

### Интеграция со Spring Boot

Модуль `max-bot-api-spring-boot` обеспечивает настройку как webhook, так и long polling режимов без шаблонного кода благодаря автоконфигурации. Добавьте зависимость и выберите подходящий режим для вашего развёртывания.

Стартер создаёт `MaxBotAPI` через `MaxBotAPI.create(token)`, которому нужен `max-bot-api-jackson` в classpath. Для режима webhook также нужен Spring MVC — стартер его не подтягивает:

```kotlin
dependencies {
    implementation("ru.etsft.max:max-bot-api-spring-boot:0.4.1")
    implementation("ru.etsft.max:max-bot-api-jackson:0.4.1")
    implementation("org.springframework.boot:spring-boot-starter-web") // только для режима webhook
}
```

Автоматически созданный `MaxBotAPI` использует `MaxClientConfig` по умолчанию. Чтобы изменить настройки клиента (таймауты, `attachmentReadyTimeout`, …), объявите собственный бин `MaxBotAPI` — стартер будет использовать его:

```java
@Bean
MaxBotAPI maxBotAPI(@Value("${max.bot.webhook.token}") String token) {
    return MaxBotAPI.create(token, MaxClientConfig.builder()
        .attachmentReadyTimeout(Duration.ofMinutes(2))
        .build());
}
```

#### Режим Webhook

Настройте `application.yml`:

```yaml
max:
  bot:
    mode: webhook
    webhook:
      token: "your-bot-token"
      url: "https://your-app.example.com/max-bot/webhook"
      secret: "your-shared-secret"
```

Стартер автоматически:

- Регистрирует `@RestController` endpoint по пути `/max-bot/webhook` (настраивается через `max.bot.webhook.path`).
- Проверяет заголовок `X-Max-Bot-Api-Secret` с использованием сравнения за константное время.
- Подписывает webhook URL на платформе MAX при запуске приложения.
- Отписывается при корректном завершении работы.
- При `max.bot.webhook.async-dispatch: true` отвечает MAX до запуска обработчика, который затем выполняется в виртуальном потоке. См. [Срок подтверждения доставки](#срок-подтверждения-доставки).

Определите бин `UpdateHandler` для обработки входящих обновлений:

```java
@Bean
UpdateHandler updateHandler() {
    return update -> {
        if (update instanceof MessageCreatedUpdate msg) {
            // обработать сообщение
        }
    };
}
```

##### Свойства конфигурации Webhook

| Свойство | По умолчанию | Описание |
|---|---|---|
| `max.bot.webhook.token` | — | Токен доступа бота (обязательный). |
| `max.bot.webhook.path` | `/max-bot/webhook` | Путь endpoint-а контроллера. |
| `max.bot.webhook.secret` | — | Общий секрет для проверки заголовка. |
| `max.bot.webhook.url` | — | Публичный URL для автоматической регистрации webhook. |
| `max.bot.webhook.auto-register` | `true` | Регистрировать подписку webhook при запуске. |
| `max.bot.webhook.auto-unregister` | `true` | Отписываться при завершении работы приложения. |
| `max.bot.webhook.async-dispatch` | `false` | Сначала отвечать MAX, затем запускать `UpdateHandler` в виртуальном потоке. |
| `max.bot.webhook.update-types` | — | Список констант `UpdateType` для подписки (пустой = все). См. [таблицу типов](#доступные-типы-обновлений). |

#### Режим Long Polling

Настройте `application.yml`:

```yaml
max:
  bot:
    mode: longpolling
    longpolling:
      token: "your-bot-token"
      poll-timeout: 30
      update-types:
        - MESSAGE_CREATED
        - MESSAGE_CALLBACK
```

Стартер автоматически:

- Создаёт экземпляр `MaxBotAPI` из настроенного токена.
- Оборачивает `MaxLongPollingConsumer` в бин `SmartLifecycle`, который запускается и останавливается вместе с контекстом приложения.
- Передаёт `poll-timeout` и `update-types` потребителю.

Определите бин `UpdateHandler` для обработки входящих обновлений:

```java
@Bean
UpdateHandler updateHandler() {
    return update -> {
        if (update instanceof MessageCreatedUpdate msg) {
            // обработать сообщение
        }
    };
}
```

Для обработки ошибок опроса объявите бин `PollingErrorHandler`. Он опционален — при отсутствии ошибки логируются на уровне WARN:

```java
@Bean
PollingErrorHandler pollingErrorHandler() {
    return e -> log.error("Ошибка опроса", e);
}
```

##### Свойства конфигурации Long Polling

| Свойство | По умолчанию | Описание |
|---|---|---|
| `max.bot.longpolling.token` | — | Токен доступа бота (обязательный). |
| `max.bot.longpolling.poll-timeout` | — | Таймаут опроса в секундах (по умолчанию из `MaxClientConfig`, 30 сек). |
| `max.bot.longpolling.update-types` | — | Список констант `UpdateType` для получения (пустой = все). См. [таблицу типов](#доступные-типы-обновлений). |

#### Выбор между режимами

Свойство `max.bot.mode` выбирает механизм доставки обновлений.

| Значение | Описание |
|---|---|
| `webhook` | Получение обновлений через HTTP webhook. Требуется публичный HTTPS-эндпоинт. |
| `longpolling` | Получение обновлений через опрос MAX API. Публичный URL не требуется. |
| `none` | Явное отключение доставки обновлений. Ни одна автоконфигурация не активируется. |

Одновременно может быть активен только один режим. Если свойство не задано, при запуске выводится WARN-сообщение. Установка `max.bot.mode=none` подавляет предупреждение и явно выражает намерение — полезно для тестовых профилей или приложений, где стартер бота находится в classpath, но не нужен.

Оба режима `webhook` и `longpolling` используют один интерфейс `UpdateHandler` (`ru.max.botapi.core.UpdateHandler`). Определите один бин-обработчик и переключайте режимы, изменяя только свойство `max.bot.mode` — изменения кода не требуются.

Пример — отключение бота в тестовом профиле:

```yaml
# application-test.yml
max:
  bot:
    mode: none
```

---

## Загрузка файлов

MAX API требует двухэтапной процедуры загрузки:

1. **Запрос URL для загрузки** — вызовите `api.getUploadUrl(UploadType)`, чтобы получить предподписанный `UploadEndpoint`.
2. **Потоковая передача файла** — вызовите метод `MaxUploadAPI`, соответствующий типу загрузки. Перегрузки для файлов на диске используют `java.net.http` с `BodyPublisher`, напрямую связанным с файлом, поэтому файлы произвольного размера передаются без загрузки в кучу.

Платформа MAX возвращает три разные формы ответа в зависимости от типа загрузки, и момент появления токена вложения тоже различается. Чтобы сделать это явным и типобезопасным, `MaxUploadAPI` предоставляет отдельный метод для каждого типа, каждый возвращает свой подтип sealed-интерфейса `UploadedInfo`.

| `UploadType` | Метод                  | Результат            | Токен получается из              |
|--------------|-------------------------|----------------------|--------------------------------|
| `FILE`       | `uploadFile(...)`       | `FileUploadedInfo`   | ответа на загрузку (JSON)       |
| `IMAGE`      | `uploadImage(...)`      | `ImageUploadedInfo`  | ответа на загрузку (JSON)       |
| `VIDEO`      | `uploadMedia(...)`      | `MediaUploadedInfo`  | `UploadEndpoint.token()`       |
| `AUDIO`      | `uploadMedia(...)`      | `MediaUploadedInfo`  | `UploadEndpoint.token()`       |

Для видео и аудио ответ на загрузку — это крошечный XML (`<retval>1</retval>`) без токена; токен вложения нужно брать из `UploadEndpoint`, возвращённого `POST /uploads`. `uploadMedia(...)` автоматически переносит этот токен в возвращаемый `MediaUploadedInfo`, чтобы вызывающему коду не приходилось передавать его вручную.

`MaxUploadAPI` владеет собственным HTTP-клиентом: создайте один экземпляр, используйте его для всех загрузок и закройте при остановке. Во фрагментах ниже предполагается открытый `uploadApi`.

#### Файл

```java
UploadEndpoint endpoint = api.getUploadUrl(UploadType.FILE).execute();
FileUploadedInfo info = uploadApi.uploadFile(endpoint, Path.of("/tmp/doc.pdf"), "doc.pdf");

FileAttachmentRequest att =
    new FileAttachmentRequest(new MediaRequestPayload(info.token()));
api.sendMessage(new NewMessageBody("File:", List.of(att), null, null, null))
    .chatId(chatId).execute();
```

#### Изображение

```java
UploadEndpoint endpoint = api.getUploadUrl(UploadType.IMAGE).execute();
ImageUploadedInfo info = uploadApi.uploadImage(endpoint, Path.of("/tmp/photo.jpg"), "photo.jpg");

// Передайте карту photos в payload без изменений.
ImageAttachmentRequest att = new ImageAttachmentRequest(
    new PhotoAttachmentRequestPayload(null, null, info.photos()));
api.sendMessage(new NewMessageBody(null, List.of(att), null, null, null))
    .chatId(chatId).execute();
```

#### Видео / Аудио

```java
UploadEndpoint endpoint = api.getUploadUrl(UploadType.VIDEO).execute();
MediaUploadedInfo info = uploadApi.uploadMedia(endpoint, Path.of("/tmp/clip.mp4"), "clip.mp4");

// info.token() == endpoint.token() (ответ на загрузку не содержит токена).
VideoAttachmentRequest att =
    new VideoAttachmentRequest(new MediaRequestPayload(info.token()));
api.sendMessage(new NewMessageBody("Video:", List.of(att), null, null, null))
    .chatId(chatId).execute();
```

Сервер MAX обрабатывает загруженное медиа асинхронно: это занимает от секунды до заметно большего времени для крупных видео. Пока обработка не завершена, `sendMessage` отвечает HTTP 400 с кодом `attachment.not.ready`. Клиент справляется с этим сам: повторяет тот же запрос с небольшим откатом (0,5 с, 1 с, 2 с, далее каждые 3 с) в пределах `attachmentReadyTimeout` (по умолчанию 30 секунд). Повтор безопасен — отклонённое сообщение не создаётся, а токен загрузки остаётся действительным.

Только если вложение так и не стало готовым, вызов выбрасывает `AttachmentNotReadyException` (наследник `MaxApiException`). Для крупных файлов увеличьте тайм-аут, а чтобы обрабатывать исключение самостоятельно, задайте `Duration.ZERO`:

```java
MaxClientConfig config = MaxClientConfig.builder()
    .attachmentReadyTimeout(Duration.ofMinutes(2))
    .build();
```

Ожидание блокирует вызывающий поток. Если вложения отправляются из обработчика вебхука, это время входит в срок подтверждения доставки — используйте `enqueue()` или асинхронную обработку (см. [Срок подтверждения доставки](#срок-подтверждения-доставки)).

---

## Конфигурация

`MaxClientConfig` — это Java record с builder-ом. Значения по умолчанию подходят для большинства производственных сценариев.

### Значения по умолчанию

| Параметр | По умолчанию                  |
|---|-------------------------------|
| `baseUrl` | `https://platform-api2.max.ru` |
| `connectTimeout` | 10 секунд                     |
| `requestTimeout` | 60 секунд                     |
| `longPollTimeout` | 30 секунд                     |
| `maxRetries` | 3                             |
| `enableRateLimiting` | `true`                        |
| `maxRequestsPerSecond` | 30                            |
| `attachmentReadyTimeout` | 30 секунд                   |
| `sslContext` | JVM trust store + встроенные доверенные сертификаты MAX |

### Пользовательская конфигурация

```java
MaxClientConfig config = MaxClientConfig.builder()
    .baseUrl("https://platform-api2.max.ru")
    .connectTimeout(Duration.ofSeconds(5))
    .requestTimeout(Duration.ofSeconds(20))
    .longPollTimeout(Duration.ofSeconds(60))
    .maxRetries(5)
    .enableRateLimiting(true)
    .maxRequestsPerSecond(30)
    .build();

MaxBotAPI api = MaxBotAPI.create("your-token", config);
```

### TLS-сертификаты

Клиент включает сертификаты Russian Trusted Root CA и Russian Trusted Sub CA,
которые нужны MAX, и использует их по умолчанию. Для стандартного endpoint
`platform-api2.max.ru` отдельно настраивать JVM trust store не нужно.

Чтобы добавить свои `.crt` или `.cer` файлы поверх встроенных сертификатов:

```java
MaxClientConfig config = MaxClientConfig.builder()
    .trustedCertificates(
        Path.of("company-root-ca.crt"),
        Path.of("company-sub-ca.crt"))
    .build();

MaxBotAPI api = MaxBotAPI.create("your-token", config);
```

Чтобы использовать только стандартную SSL-конфигурацию JVM и не подключать
встроенные сертификаты:

```java
MaxClientConfig config = MaxClientConfig.builder()
    .withoutBundledTrustedCertificates()
    .build();
```

Для полного контроля передайте собственный `SSLContext` через `.sslContext(...)`.

### Использование конфигурации по умолчанию

```java
MaxClientConfig config = MaxClientConfig.defaults();
```

### Ограничения частоты запросов

Встроенный ограничитель держит клиент в целом в пределах `maxRequestsPerSecond` (по умолчанию 30) — это общий лимит платформы. Кроме того, MAX допускает не более **2 операций в секунду в одном диалоге, групповом чате или канале** для отправки сообщений (`POST /messages`), их редактирования (`PUT /messages`) и ответов на callback (`POST /answers`). Этот лимит по чату клиент не отслеживает, поэтому бот, который пишет в один чат пачками, должен сам ставить такие вызовы в очередь или делать паузы.

---

## Обработка ошибок

Все исключения являются непроверяемыми (unchecked) и наследуются от `RuntimeException`.

### Иерархия исключений

```
RuntimeException
├── MaxClientException                — сбой транспорта/сети (ошибка I/O, таймаут)
└── MaxApiException                   — API вернул 4xx или 5xx
    ├── MaxRateLimitException         — 429 Too Many Requests
    ├── MaxMethodNotAllowedException  — 405 Method Not Allowed
    └── AttachmentNotReadyException   — вложение всё ещё обрабатывается после attachmentReadyTimeout
```

### `MaxApiException`

Выбрасывается, когда сервер возвращает HTTP-ответ с ошибкой. Предоставляет:

- `statusCode()` — HTTP-код статуса
- `errorMessage()` — человекочитаемое сообщение об ошибке от API
- `errorCode()` — опциональный машиночитаемый код ошибки от API (может быть `null`)

### `MaxRateLimitException`

Подкласс `MaxApiException`, выбрасываемый при HTTP 429. Предоставляет:

- `retryAfter()` — рекомендуемая `Duration` для ожидания перед повторной попыткой, или `null`, если сервер её не указал

Встроенная политика повторных попыток автоматически обрабатывает ответы 429 и 503 с экспоненциальным откатом вплоть до `maxRetries` попыток, после чего исключение пробрасывается дальше.

### `MaxClientException`

Выбрасывается при сбоях на транспортном уровне (отказ соединения, таймаут, ошибка I/O). Оборачивает исходную причину.

### Пример

```java
try {
    api.sendMessage(new NewMessageBody("Hello", null, null, null, null))
        .chatId(chatId)
        .execute();
} catch (MaxRateLimitException e) {
    Duration retryAfter = e.retryAfter();
    // выдержать паузу и повторить
} catch (MaxApiException e) {
    System.err.println("Ошибка API " + e.statusCode() + ": " + e.errorMessage());
} catch (MaxClientException e) {
    System.err.println("Ошибка транспорта: " + e.getMessage());
}
```

---

## Переход на 0.4.0

В 0.4.0 модели приведены в соответствие со схемой MAX API. Эти изменения ломают совместимость как на уровне исходного кода, так и бинарную:

| Было | Стало |
|---|---|
| `CommentRemovedUpdate(timestamp, message)` | `CommentRemovedUpdate(timestamp, messageId, chatId, userId, postId)`: комментария больше нет, приходят только его идентификаторы |
| `new OpenAppButton(text, url, payload)` | `OpenAppButton.ofWebApp(text, "bot_username")` или `new OpenAppButton(text, webApp, contactId, payload)`; `webApp` обязателен — без него MAX отклоняет кнопку |
| `new MessageButton(text, message)` | `new MessageButton(text)`: кнопка отправляет собственный текст |
| `Message.body()` никогда не `null` | `null`, если сообщение только пересылает другое; проверяйте перед `body().text()` |
| `ChatPatch.icon` — это `Image` | `PhotoAttachmentRequestPayload`: внешний URL, токен или загруженные `photos` |
| `lastActivityTime()` — `long`, `0` при отсутствии | `Long`, `null`, если его скрывают настройки приватности (`User`, `UserWithPhoto`, `BotInfo`, `ChatMember`) |
| `MarkupElement(type, from, length)` | добавлены `url`, `userLink` и `userId` |
| `DialogClearedUpdate`, `DialogMutedUpdate`, `DialogUnmutedUpdate`, `DialogRemovedUpdate`, `BotStoppedUpdate` | добавлен `userLocale`; у `DialogMutedUpdate` ещё `mutedUntil` |
| `MessageRemovedUpdate(timestamp, messageId, chatId, userId)` | добавлен `postId` |
| `Subscription(url, updateTypes)` | `Subscription(url, time, updateTypes)` |
| `AudioAttachment(payload)` | `AudioAttachment(payload, transcription)` |
| `MaxWebhookServer.register()` / `unregister()` игнорируют `success: false` | выбрасывают `IllegalStateException` |

---

## Сборка из исходного кода

Требуется JDK 21+. Gradle wrapper включён в репозиторий, устанавливать Gradle не нужно.

```bash
# Клонировать репозиторий
git clone https://github.com/etsft/max-bot-api-java.git
cd max-bot-api-java

# Собрать и запустить все тесты
./gradlew build

# Только запуск тестов
./gradlew test

# Сгенерировать отчёт о покрытии JaCoCo
./gradlew jacocoTestReport

# Запустить проверки качества Checkstyle и SpotBugs
./gradlew check

# Сборка без тестов
./gradlew build -x test
```

Живой интеграционный набор намеренно не входит ни в одну из команд выше. Он обращается к
боевому MAX API с реальным токеном бота и запускается явно:

```bash
./gradlew :max-bot-api-integration-tests:liveTest --console=plain
```

Список переменных окружения и описание побочных эффектов — в
[max-bot-api-integration-tests/README.md](max-bot-api-integration-tests/README.md).

Артефакты сборки и отчёты о покрытии размещаются в директории `build/` каждого модуля. Агрегированный отчёт о покрытии находится в `build/reports/jacoco/`.

---

## Участие в разработке

1. Сделайте форк репозитория и создайте feature-ветку от `master`.
2. Напишите тесты для любой новой функциональности. Пороги покрытия должны оставаться выполненными (≥ 85% строк, ≥ 80% ветвей).
3. Убедитесь, что `./gradlew check` проходит (Checkstyle + SpotBugs), прежде чем отправлять pull request.
4. Сохраняйте минимальный публичный API. Новые модели должны использовать Java records; новые union-типы — sealed interfaces.
5. Отправьте pull request с чётким описанием изменения и его обоснованием.

Для сообщений об ошибках и запросов новых возможностей открывайте issue на [GitHub](https://github.com/etsft/max-bot-api-java/issues).

---

## Лицензия

Проект распространяется под лицензией [Apache License 2.0](LICENSE).

```
Copyright 2026 Boris Tarelkin

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

**Ссылки:**
- [Документация MAX Bot API](https://dev.max.ru/docs-api) (на русском)
- [Эталонный клиент на TypeScript](https://github.com/max-messenger/max-bot-api-client-ts)
- [Эталонный клиент на Java 8](https://github.com/max-messenger/max-bot-api-client-java)
