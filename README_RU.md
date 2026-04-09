# MAX Bot API Java Client

[![Build Status](https://gitlab.etsft.ru/batarelkin/max-bot-api-java/badges/main/pipeline.svg)](https://gitlab.etsft.ru/batarelkin/max-bot-api-java/-/pipelines)
[![Coverage](https://gitlab.etsft.ru/batarelkin/max-bot-api-java/badges/main/coverage.svg)](https://gitlab.etsft.ru/batarelkin/max-bot-api-java/-/jobs)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/projects/jdk/21/)

Современная клиентская библиотека Java 21 для [MAX Messenger Bot API](https://dev.max.ru/docs-api).

---

## Обзор

Библиотека предоставляет полноценный, идиоматичный интерфейс Java 21 ко всем 31 методу MAX Bot API. Она спроектирована как лёгкая, типобезопасная и совместимая с будущими версиями API — модуль ядра не имеет зависимостей за пределами JDK.

**Основные возможности:**

- **Java 21 records** для всех DTO-типов — иммутабельные, компактные, без шаблонного кода.
- **Sealed interfaces** для union-типов (`Attachment`, `Button`, `Update`) с полной поддержкой сопоставления с образцом (pattern matching).
- **Виртуальные потоки** для неблокирующего ввода-вывода как в режиме long polling, так и в режиме webhook.
- **Транспорт `java.net.http.HttpClient`** — внешние HTTP-библиотеки не требуются.
- **Fluent query builders** для всех 31 метода API с поддержкой синхронного (`execute()`) и асинхронного (`enqueue()`) вызова.
- **Прямая совместимость при десериализации** — неизвестные типы порождают резервные записи `Unknown*` вместо ошибок разбора.
- **Встроенный ограничитель частоты запросов** (token bucket, 30 rps) и **политика повторных попыток** (экспоненциальный откат при HTTP 429/503).
- **Потоковая загрузка файлов** — большие файлы передаются без буферизации в куче.
- **485+ тестов**, покрытие строк по JaCoCo ≥ 85% / покрытие ветвей ≥ 80%.

---

## Требования

- **JDK 21** или новее
- **Gradle 8** или новее (либо Maven 3.9+)

---

## Быстрый старт

### Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
    implementation("ru.etsft.max:max-bot-api-client:0.2.0")
    implementation("ru.etsft.max:max-bot-api-jackson:0.2.0")
    implementation("ru.etsft.max:max-bot-api-longpolling:0.2.0")

    // Опционально: поддержка webhook
    // implementation("ru.etsft.max:max-bot-api-webhook:0.2.0")

    // Опционально: автоконфигурация Spring Boot (webhook + long polling)
    // implementation("ru.etsft.max:max-bot-api-spring-boot:0.2.0")
}
```

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>ru.etsft.max</groupId>
        <artifactId>max-bot-api-client</artifactId>
        <version>0.2.0</version>
    </dependency>
    <dependency>
        <groupId>ru.etsft.max</groupId>
        <artifactId>max-bot-api-jackson</artifactId>
        <version>0.2.0</version>
    </dependency>
    <dependency>
        <groupId>ru.etsft.max</groupId>
        <artifactId>max-bot-api-longpolling</artifactId>
        <version>0.2.0</version>
    </dependency>
    <!-- Опционально: автоконфигурация Spring Boot (webhook + long polling) -->
    <!--
    <dependency>
        <groupId>ru.etsft.max</groupId>
        <artifactId>max-bot-api-spring-boot</artifactId>
        <version>0.2.0</version>
    </dependency>
    -->
</dependencies>
```

---

## Примеры использования

### EchoBot (Long Polling)

```java
MaxBotAPI api = MaxBotAPI.create("your-token");

MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
    .api(api)
    .handler(update -> {
        if (update instanceof MessageCreatedUpdate msg) {
            String text = msg.message().body().text();
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

### Загрузка файла

Загрузка файла выполняется в два шага: сначала запрашивается URL для загрузки через API, затем файл передаётся потоком на этот URL.

```java
MaxUploadAPI uploadApi = new MaxUploadAPI();

// Шаг 1: запросить endpoint для загрузки
UploadEndpoint endpoint = api.getUploadUrl(UploadType.FILE).execute();

// Шаг 2: передать файл потоком (без буферизации в куче)
UploadedInfo info = uploadApi.upload(endpoint.url(), Path.of("file.txt"), "file.txt");

// Шаг 3: прикрепить полученный токен к сообщению
api.sendMessage(new NewMessageBody(
    "File:",
    List.of(new FileAttachmentRequest(new MediaRequestPayload(info.token()))),
    null, null, null
)).chatId(chatId).execute();
```

---

## Модули

| Модуль | Описание |
|---|---|
| `max-bot-api-core` | Модельные records, sealed interfaces, SPI сериализатора. Нет внешних зависимостей (только JDK). |
| `max-bot-api-client` | HTTP-транспорт (`java.net.http`), `MaxClient`, фасад `MaxBotAPI`, ограничитель частоты, политика повторных попыток. |
| `max-bot-api-jackson` | Адаптер сериализатора Jackson 2.x с кастомными десериализаторами для полиморфных типов. |
| `max-bot-api-gson` | Адаптер сериализатора Gson (опциональный, заглушка). |
| `max-bot-api-longpolling` | Потребитель long polling на базе виртуальных потоков с экспоненциальным откатом. |
| `max-bot-api-webhook` | HTTPS-сервер webhook с проверкой секретного заголовка. |
| `max-bot-api-test-support` | WireMock-заглушки, JSON-фикстуры и вспомогательные классы для интеграционных тестов. |
| `max-bot-api-spring-boot` | Автоконфигурация Spring Boot для режимов webhook и long polling — контроллер, регистрация подписки, управление жизненным циклом. |
| `max-bot-api-examples` | Запускаемые примеры: `EchoBot`, `KeyboardBot`, `FileUploadBot`. |

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
    .build();

consumer.start();   // неблокирующий вызов; опрос выполняется в виртуальном потоке
// ...
consumer.stop();    // корректное завершение работы
```

Потребитель вызывает `getUpdates` в цикле, отслеживая маркер, возвращённый каждым ответом, чтобы исключить повторную доставку событий. Ошибки (сетевые сбои, ошибки API, исключения в обработчике) передаются в коллбэк `.onError()`; если он не задан, ошибки логируются на уровне WARN. После каждой ошибки цикл продолжает работу с экспоненциальным откатом (1с → 2с → 4с → максимум 30с).

---

## Webhooks

`MaxWebhookServer` ожидает HTTPS POST-запросы от платформы MAX и передаёт каждое входящее обновление обработчику. Перед обработкой выполняется проверка секретного заголовка.

```java
MaxWebhookServer server = MaxWebhookServer.builder()
    .api(api)
    .handler(update -> {
        // обработать обновление
    })
    .port(8443)
    .secret("my-secret")
    .build();

server.start();
```

Убедитесь, что TLS-сертификат, обслуживаемый на заданном порту, является доверенным для платформы MAX, либо используйте обратный прокси (например, nginx) для терминирования TLS.

### Интеграция со Spring Boot

Модуль `max-bot-api-spring-boot` обеспечивает настройку как webhook, так и long polling режимов без шаблонного кода благодаря автоконфигурации. Добавьте зависимость и выберите подходящий режим для вашего развёртывания.

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
| `max.bot.webhook.update-types` | — | Список типов обновлений для подписки (пустой = все). |

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
        - message_created
        - message_callback
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
| `max.bot.longpolling.update-types` | — | Список типов обновлений для получения (пустой = все). |

#### Выбор между режимами

Свойство `max.bot.mode` выбирает механизм доставки обновлений. Оно принимает два значения: `webhook` или `longpolling`. Одновременно может быть активен только один режим — если свойство не задано, ни одна автоконфигурация не активируется.

Оба режима используют один интерфейс `UpdateHandler` (`ru.max.botapi.core.UpdateHandler`). Определите один бин-обработчик и переключайте режимы, изменяя только свойство `max.bot.mode` — изменения кода не требуются.

| Свойство | Значения | Описание |
|---|---|---|
| `max.bot.mode` | `webhook`, `longpolling` | Режим доставки обновлений (обязательный). |

---

## Загрузка файлов

MAX API требует двухэтапной процедуры загрузки:

1. **Запрос URL для загрузки** — вызовите `api.getUploadUrl(UploadType)`, чтобы получить предподписанный `UploadEndpoint`.
2. **Потоковая передача файла** — вызовите `MaxUploadAPI.upload(url, path, filename)`. Реализация использует `java.net.http` с `BodyPublisher`, напрямую связанным с файлом, поэтому файлы произвольного размера передаются без загрузки в кучу.

```java
MaxUploadAPI uploadApi = new MaxUploadAPI();

// Получить endpoint для загрузки
UploadEndpoint endpoint = api.getUploadUrl(UploadType.IMAGE).execute();

// Загрузить — потоком, без буферизации в куче
UploadedInfo info = uploadApi.upload(endpoint.url(), Path.of("/tmp/photo.jpg"), "photo.jpg");

// Использовать полученный токен во вложении
ImageAttachmentRequest attachment =
    new ImageAttachmentRequest(new MediaRequestPayload(info.token()));

api.sendMessage(new NewMessageBody(null, List.of(attachment), null, null, null))
    .chatId(chatId)
    .execute();
```

Поддерживаемые значения `UploadType`: `IMAGE`, `VIDEO`, `AUDIO`, `FILE`.

---

## Конфигурация

`MaxClientConfig` — это Java record с builder-ом. Значения по умолчанию подходят для большинства производственных сценариев.

### Значения по умолчанию

| Параметр | По умолчанию                  |
|---|-------------------------------|
| `baseUrl` | `https://platform-api.max.ru` |
| `connectTimeout` | 10 секунд                     |
| `requestTimeout` | 60 секунд                     |
| `longPollTimeout` | 30 секунд                     |
| `maxRetries` | 3                             |
| `enableRateLimiting` | `true`                        |
| `maxRequestsPerSecond` | 30                            |

### Пользовательская конфигурация

```java
MaxClientConfig config = MaxClientConfig.builder()
    .baseUrl("https://platform-api.max.ru")
    .connectTimeout(Duration.ofSeconds(5))
    .requestTimeout(Duration.ofSeconds(20))
    .longPollTimeout(Duration.ofSeconds(60))
    .maxRetries(5)
    .enableRateLimiting(true)
    .maxRequestsPerSecond(30)
    .build();

MaxBotAPI api = MaxBotAPI.create("your-token", config);
```

### Использование конфигурации по умолчанию

```java
MaxClientConfig config = MaxClientConfig.defaults();
```

---

## Обработка ошибок

Все исключения являются непроверяемыми (unchecked) и наследуются от `RuntimeException`.

### Иерархия исключений

```
RuntimeException
└── MaxClientException          — сбой транспорта/сети (ошибка I/O, таймаут)
└── MaxApiException             — API вернул 4xx или 5xx
    └── MaxRateLimitException   — API вернул 429 Too Many Requests
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

## Сборка из исходного кода

Требуется JDK 21+ и Gradle 8+. Gradle wrapper включён в репозиторий.

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

Артефакты сборки и отчёты о покрытии размещаются в директории `build/` каждого модуля. Агрегированный отчёт о покрытии находится в `build/reports/jacoco/`.

---

## Участие в разработке

1. Сделайте форк репозитория и создайте feature-ветку от `main`.
2. Напишите тесты для любой новой функциональности. Пороги покрытия должны оставаться выполненными (≥ 85% строк, ≥ 80% ветвей).
3. Убедитесь, что `./gradlew check` проходит (Checkstyle + SpotBugs), прежде чем отправлять merge request.
4. Сохраняйте минимальный публичный API. Новые модели должны использовать Java records; новые union-типы — sealed interfaces.
5. Отправьте merge request с чётким описанием изменения и его обоснованием.

Для сообщений об ошибках и запросов новых возможностей открывайте issue в [репозитории проекта](https://github.com/etsft/max-bot-api-java.git).

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
