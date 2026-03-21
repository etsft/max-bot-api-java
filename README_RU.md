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
    implementation("ru.etsft.max.botapi:max-bot-api-client:0.1.0-SNAPSHOT")
    implementation("ru.etsft.max.botapi:max-bot-api-jackson:0.1.0-SNAPSHOT")
    implementation("ru.etsft.max.botapi:max-bot-api-longpolling:0.1.0-SNAPSHOT")

    // Опционально: поддержка webhook
    // implementation("ru.etsft.max.botapi:max-bot-api-webhook:0.1.0-SNAPSHOT")
}
```

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>ru.etsft.max.botapi</groupId>
        <artifactId>max-bot-api-client</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>ru.etsft.max.botapi</groupId>
        <artifactId>max-bot-api-jackson</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>ru.etsft.max.botapi</groupId>
        <artifactId>max-bot-api-longpolling</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
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
    .build();

consumer.start();   // неблокирующий вызов; опрос выполняется в виртуальном потоке
// ...
consumer.stop();    // корректное завершение работы
```

Потребитель вызывает `getUpdates` в цикле, отслеживая маркер, возвращённый каждым ответом, чтобы исключить повторную доставку событий. Необработанные исключения в обработчике перехватываются и логируются; цикл продолжает работу.

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

| Параметр | По умолчанию |
|---|---|
| `baseUrl` | `https://platform-api.max.ru` |
| `connectTimeout` | 10 секунд |
| `requestTimeout` | 30 секунд |
| `longPollTimeout` | 90 секунд |
| `maxRetries` | 3 |
| `enableRateLimiting` | `true` |
| `maxRequestsPerSecond` | 30 |

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
git clone https://gitlab.etsft.ru/batarelkin/max-bot-api-java.git
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

Для сообщений об ошибках и запросов новых возможностей открывайте issue в [репозитории проекта](https://gitlab.etsft.ru/batarelkin/max-bot-api-java).

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
