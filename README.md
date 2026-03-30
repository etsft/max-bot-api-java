# MAX Bot API Java Client

[![Build Status](https://gitlab.etsft.ru/batarelkin/max-bot-api-java/badges/main/pipeline.svg)](https://gitlab.etsft.ru/batarelkin/max-bot-api-java/-/pipelines)
[![Coverage](https://gitlab.etsft.ru/batarelkin/max-bot-api-java/badges/main/coverage.svg)](https://gitlab.etsft.ru/batarelkin/max-bot-api-java/-/jobs)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/projects/jdk/21/)

A modern Java 21 client library for the [MAX Messenger Bot API](https://dev.max.ru/docs-api).

---

## Overview

This library provides a complete, idiomatic Java 21 interface to all 31 MAX Bot API methods. It is designed to be lightweight, type-safe, and forward-compatible, with no dependencies outside the JDK for the core module.

**Key capabilities:**

- **Java 21 records** for all DTO types — immutable, compact, and zero boilerplate.
- **Sealed interfaces** for union types (`Attachment`, `Button`, `Update`) with full pattern matching support.
- **Virtual threads** for non-blocking I/O in both long polling and webhook modes.
- **`java.net.http.HttpClient`** transport — no external HTTP library required.
- **Fluent query builders** for all 31 API methods, supporting both synchronous (`execute()`) and asynchronous (`enqueue()`) invocation.
- **Forward-compatible deserialization** — unknown types produce `Unknown*` fallback records instead of parse errors.
- **Built-in rate limiter** (30 rps token bucket) and **retry policy** (exponential backoff on HTTP 429/503).
- **Streaming file upload** — no heap buffering for large files.
- **485+ tests**, JaCoCo line coverage ≥ 85% / branch coverage ≥ 80%.

---

## Requirements

- **JDK 21** or later
- **Gradle 8** or later (or Maven 3.9+)

---

## Quick Start

### Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
    implementation("ru.etsft.max:max-bot-api-client:0.2.0")
    implementation("ru.etsft.max:max-bot-api-jackson:0.2.0")
    implementation("ru.etsft.max:max-bot-api-longpolling:0.2.0")

    // Optional: webhook support
    // implementation("ru.etsft.max:max-bot-api-webhook:0.2.0")

    // Optional: Spring Boot auto-configuration (webhook + long polling)
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
    <!-- Optional: Spring Boot auto-configuration (webhook + long polling) -->
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

## Usage Examples

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

### Sending a Message with an Inline Keyboard

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

### Handling Button Callbacks

Use pattern matching on the sealed `Update` type to dispatch by event kind:

```java
switch (update) {
    case MessageCallbackUpdate cb -> {
        String callbackId = cb.callback().callbackId();
        api.answerOnCallback(new CallbackAnswer(null, "Answered!"), callbackId).execute();
    }
    case MessageCreatedUpdate msg -> { /* handle new message */ }
    default -> { /* ignore unknown update types */ }
}
```

### Uploading a File

File upload is a two-step process: first obtain an upload URL from the API, then stream the file to that URL.

```java
MaxUploadAPI uploadApi = new MaxUploadAPI();

// Step 1: request an upload endpoint
UploadEndpoint endpoint = api.getUploadUrl(UploadType.FILE).execute();

// Step 2: stream the file (no heap buffering)
UploadedInfo info = uploadApi.upload(endpoint.url(), Path.of("file.txt"), "file.txt");

// Step 3: attach the uploaded token to a message
api.sendMessage(new NewMessageBody(
    "File:",
    List.of(new FileAttachmentRequest(new MediaRequestPayload(info.token()))),
    null, null, null
)).chatId(chatId).execute();
```

---

## Modules

| Module | Description |
|---|---|
| `max-bot-api-core` | Model records, sealed interfaces, serializer SPI. Zero external dependencies (JDK only). |
| `max-bot-api-client` | HTTP transport (`java.net.http`), `MaxClient`, `MaxBotAPI` facade, rate limiter, retry policy. |
| `max-bot-api-jackson` | Jackson 2.x serializer adapter with custom deserializers for polymorphic types. |
| `max-bot-api-gson` | Gson serializer adapter (optional, placeholder). |
| `max-bot-api-longpolling` | Long polling consumer backed by virtual threads, with exponential backoff. |
| `max-bot-api-webhook` | HTTPS webhook server with secret-header validation. |
| `max-bot-api-test-support` | WireMock stubs, JSON fixtures, and test helpers for integration tests. |
| `max-bot-api-spring-boot` | Spring Boot auto-configuration for both webhook and long-polling modes — controller, subscription registration, lifecycle management. |
| `max-bot-api-examples` | Runnable examples: `EchoBot`, `KeyboardBot`, `FileUploadBot`. |

---

## Long Polling

`MaxLongPollingConsumer` manages the polling loop on a virtual thread. It reconnects automatically and applies exponential backoff on transient errors.

```java
MaxBotAPI api = MaxBotAPI.create("your-token");

MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
    .api(api)
    .handler(update -> {
        // dispatch on the sealed Update type
        if (update instanceof MessageCreatedUpdate msg) {
            // process message
        }
    })
    .build();

consumer.start();   // non-blocking; polling runs on a virtual thread
// ...
consumer.stop();    // graceful shutdown
```

The consumer calls `getUpdates` in a loop, tracking the marker returned by each response to avoid re-delivering events. Unhandled exceptions in the handler are caught and logged; the loop continues.

---

## Webhooks

`MaxWebhookServer` listens for HTTPS POST requests from the MAX platform and dispatches each incoming update to a handler. It validates the secret header before processing.

```java
MaxWebhookServer server = MaxWebhookServer.builder()
    .api(api)
    .handler(update -> {
        // handle update
    })
    .port(8443)
    .secret("my-secret")
    .build();

server.start();
```

Ensure the TLS certificate served on the configured port is trusted by the MAX platform, or use a reverse proxy (e.g., nginx) to terminate TLS.

### Spring Boot Integration

The `max-bot-api-spring-boot` module provides zero-boilerplate setup for both webhook and long-polling modes via auto-configuration. Add the dependency and choose the mode that fits your deployment.

#### Webhook Mode

Configure `application.yml`:

```yaml
max:
  bot:
    mode: webhook
    webhook:
      token: "your-bot-token"
      url: "https://your-app.example.com/max-bot/webhook"
      secret: "your-shared-secret"
```

The starter automatically:

- Registers a `@RestController` endpoint at `/max-bot/webhook` (configurable via `max.bot.webhook.path`).
- Validates the `X-Max-Bot-Api-Secret` header using constant-time comparison.
- Subscribes the webhook URL with the MAX platform on application startup.
- Unsubscribes on graceful shutdown.

Define an `UpdateHandler` bean to process incoming updates:

```java
@Bean
UpdateHandler updateHandler() {
    return update -> {
        if (update instanceof MessageCreatedUpdate msg) {
            // process message
        }
    };
}
```

##### Webhook Configuration Properties

| Property | Default | Description |
|---|---|---|
| `max.bot.webhook.token` | — | Bot access token (required). |
| `max.bot.webhook.path` | `/max-bot/webhook` | Controller endpoint path. |
| `max.bot.webhook.secret` | — | Shared secret for header validation. |
| `max.bot.webhook.url` | — | Public URL for webhook auto-registration. |
| `max.bot.webhook.auto-register` | `true` | Register webhook subscription on startup. |
| `max.bot.webhook.auto-unregister` | `true` | Unsubscribe on application shutdown. |
| `max.bot.webhook.update-types` | — | List of update types to subscribe to (empty = all). |

#### Long Polling Mode

Configure `application.yml`:

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

The starter automatically:

- Creates a `MaxBotAPI` instance from the configured token.
- Wraps a `MaxLongPollingConsumer` in a `SmartLifecycle` bean that starts and stops with the application context.
- Passes `poll-timeout` and `update-types` to the consumer.

Define an `UpdateHandler` bean to process incoming updates:

```java
@Bean
UpdateHandler updateHandler() {
    return update -> {
        if (update instanceof MessageCreatedUpdate msg) {
            // process message
        }
    };
}
```

##### Long Polling Configuration Properties

| Property | Default | Description |
|---|---|---|
| `max.bot.longpolling.token` | — | Bot access token (required). |
| `max.bot.longpolling.poll-timeout` | — | Poll timeout in seconds (defaults to `MaxClientConfig` value, 30s). |
| `max.bot.longpolling.update-types` | — | List of update types to receive (empty = all). |

#### Choosing Between Modes

The `max.bot.mode` property selects the update delivery mechanism. It accepts two values: `webhook` or `longpolling`. Only one mode can be active at a time — if the property is not set, neither auto-configuration activates.

Both modes use the same `UpdateHandler` interface (`ru.max.botapi.core.UpdateHandler`). Define a single handler bean and switch modes by changing only the `max.bot.mode` property — no code changes required.

| Property | Values | Description |
|---|---|---|
| `max.bot.mode` | `webhook`, `longpolling` | Update delivery mode (required). |

---

## File Upload

The MAX API requires a two-step upload workflow:

1. **Request an upload URL** — call `api.getUploadUrl(UploadType)` to receive a pre-signed `UploadEndpoint`.
2. **Stream the file** — call `MaxUploadAPI.upload(url, path, filename)`. The implementation uses `java.net.http` with a `BodyPublisher` backed directly by the file, so arbitrarily large files are transferred without loading them into the heap.

```java
MaxUploadAPI uploadApi = new MaxUploadAPI();

// Obtain upload endpoint
UploadEndpoint endpoint = api.getUploadUrl(UploadType.IMAGE).execute();

// Upload — streaming, no heap buffering
UploadedInfo info = uploadApi.upload(endpoint.url(), Path.of("/tmp/photo.jpg"), "photo.jpg");

// Use the returned token in an attachment
ImageAttachmentRequest attachment =
    new ImageAttachmentRequest(new MediaRequestPayload(info.token()));

api.sendMessage(new NewMessageBody(null, List.of(attachment), null, null, null))
    .chatId(chatId)
    .execute();
```

Supported `UploadType` values: `IMAGE`, `VIDEO`, `AUDIO`, `FILE`.

---

## Configuration

`MaxClientConfig` is a Java record with a builder. The defaults are appropriate for most production use cases.

### Default values

| Parameter | Default                       |
|---|-------------------------------|
| `baseUrl` | `https://platform-api.max.ru` |
| `connectTimeout` | 10 seconds                    |
| `requestTimeout` | 60 seconds                    |
| `longPollTimeout` | 30 seconds                    |
| `maxRetries` | 3                             |
| `enableRateLimiting` | `true`                        |
| `maxRequestsPerSecond` | 30                            |

### Custom configuration

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

### Using defaults

```java
MaxClientConfig config = MaxClientConfig.defaults();
```

---

## Error Handling

All exceptions are unchecked and extend `RuntimeException`.

### Exception hierarchy

```
RuntimeException
└── MaxClientException          — transport/network failure (I/O error, timeout)
└── MaxApiException             — API returned 4xx or 5xx
    └── MaxRateLimitException   — API returned 429 Too Many Requests
```

### `MaxApiException`

Thrown when the server returns an HTTP error response. Provides:

- `statusCode()` — the HTTP status code
- `errorMessage()` — human-readable error message from the API
- `errorCode()` — optional machine-readable error code from the API (may be `null`)

### `MaxRateLimitException`

A subclass of `MaxApiException` thrown on HTTP 429. Provides:

- `retryAfter()` — suggested `Duration` to wait before retrying, or `null` if not specified by the server

The built-in retry policy automatically handles 429 and 503 responses with exponential backoff up to `maxRetries` attempts before propagating the exception.

### `MaxClientException`

Thrown on transport-level failures (connection refused, timeout, I/O error). Wraps the original cause.

### Example

```java
try {
    api.sendMessage(new NewMessageBody("Hello", null, null, null, null))
        .chatId(chatId)
        .execute();
} catch (MaxRateLimitException e) {
    Duration retryAfter = e.retryAfter();
    // back off and retry
} catch (MaxApiException e) {
    System.err.println("API error " + e.statusCode() + ": " + e.errorMessage());
} catch (MaxClientException e) {
    System.err.println("Transport error: " + e.getMessage());
}
```

---

## Building from Source

Requires JDK 21+ and Gradle 8+. The Gradle wrapper is included.

```bash
# Clone the repository
git clone https://github.com/etsft/max-bot-api-java.git
cd max-bot-api-java

# Build and run all tests
./gradlew build

# Run tests only
./gradlew test

# Generate JaCoCo coverage report
./gradlew jacocoTestReport

# Run Checkstyle and SpotBugs quality gates
./gradlew check

# Build without tests
./gradlew build -x test
```

Build output and coverage reports are placed under each module's `build/` directory. The aggregated coverage report is at `build/reports/jacoco/`.

---

## Contributing

1. Fork the repository and create a feature branch from `main`.
2. Write tests for any new functionality. Coverage gates must continue to pass (≥ 85% line, ≥ 80% branch).
3. Ensure `./gradlew check` passes (Checkstyle + SpotBugs) before submitting a merge request.
4. Keep public API surface minimal. New models should use Java records; new union types should use sealed interfaces.
5. Submit a merge request with a clear description of the change and its rationale.

For bug reports and feature requests, open an issue on the [project repository](https://github.com/etsft/max-bot-api-java.git).

---

## License

This project is licensed under the [Apache License 2.0](LICENSE).

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

**References:**
- [MAX Bot API documentation](https://dev.max.ru/docs-api) (Russian)
