# MAX Bot API Java Client

[![Maven Central](https://img.shields.io/maven-central/v/ru.etsft.max/max-bot-api-client.svg)](https://central.sonatype.com/artifact/ru.etsft.max/max-bot-api-client)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/projects/jdk/21/)

A modern Java 21 client library for the [MAX Messenger Bot API](https://dev.max.ru/docs-api).

---

## Overview

This library provides a complete, idiomatic Java 21 interface to supported MAX Bot API methods. It is designed to be lightweight, type-safe, and forward-compatible, with no dependencies outside the JDK for the core module.

**Key capabilities:**

- **Java 21 records** for all DTO types — immutable, compact, and zero boilerplate.
- **Sealed interfaces** for union types (`Attachment`, `Button`, `Update`) with full pattern matching support.
- **Virtual threads** for non-blocking I/O in both long polling and webhook modes.
- **`java.net.http.HttpClient`** transport — no external HTTP library required.
- **Fluent query builders** for supported API methods, supporting both synchronous (`execute()`) and asynchronous (`enqueue()`) invocation.
- **Forward-compatible deserialization** — unknown types produce `Unknown*` fallback records instead of parse errors.
- **Built-in rate limiter** (30 rps token bucket) and **retry policy** (exponential backoff on HTTP 429/503).
- **Streaming file upload** — no heap buffering for large files; messages with a freshly uploaded attachment are resent automatically until MAX has processed it.
- **670+ unit tests**, JaCoCo line coverage ≥ 85% / branch coverage ≥ 80%, plus a live suite against the real API.

---

## Requirements

- **JDK 21** or later
- Any build tool that resolves Maven Central artifacts (Gradle, Maven, …)

---

## Quick Start

### Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
    implementation("ru.etsft.max:max-bot-api-client:0.5.0")
    // The default serializer: MaxBotAPI.create(...) looks it up on the classpath
    implementation("ru.etsft.max:max-bot-api-jackson:0.5.0")
    implementation("ru.etsft.max:max-bot-api-longpolling:0.5.0")

    // Optional: webhook support
    // implementation("ru.etsft.max:max-bot-api-webhook:0.5.0")

    // Optional: Spring Boot auto-configuration (webhook + long polling)
    // implementation("ru.etsft.max:max-bot-api-spring-boot:0.5.0")
}
```

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>ru.etsft.max</groupId>
        <artifactId>max-bot-api-client</artifactId>
        <version>0.5.0</version>
    </dependency>
    <dependency>
        <groupId>ru.etsft.max</groupId>
        <artifactId>max-bot-api-jackson</artifactId>
        <version>0.5.0</version>
    </dependency>
    <dependency>
        <groupId>ru.etsft.max</groupId>
        <artifactId>max-bot-api-longpolling</artifactId>
        <version>0.5.0</version>
    </dependency>
    <!-- Optional: webhook support -->
    <!--
    <dependency>
        <groupId>ru.etsft.max</groupId>
        <artifactId>max-bot-api-webhook</artifactId>
        <version>0.5.0</version>
    </dependency>
    -->
    <!-- Optional: Spring Boot auto-configuration (webhook + long polling) -->
    <!--
    <dependency>
        <groupId>ru.etsft.max</groupId>
        <artifactId>max-bot-api-spring-boot</artifactId>
        <version>0.5.0</version>
    </dependency>
    -->
</dependencies>
```

`MaxBotAPI` and `MaxUploadAPI` hold an HTTP client and implement `AutoCloseable`: close them when the bot shuts down, for example with try-with-resources.

---

## Usage Examples

Complete, runnable versions of these snippets live in [`max-bot-api-examples`](max-bot-api-examples/src/main/java/ru/max/botapi/examples). Run one with:

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
            // A message that only forwards another one has no body of its own.
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

`callback().payload()` is `null` when the button carries no payload — check it before switching on it.

### Moderating Channel Comments

Comments on a channel post are a separate group of methods. The bot must be an administrator of
the channel: `READ_ALL_MESSAGES` to read comments, plus `WRITE` to post, `EDIT` to edit and
`DELETE` to remove them. Comments carry no attachments and cannot be forwarded, which is why they
use `NewCommentBody` and `CommentMessage` rather than the message types.

```java
String postId = "mid.abc123";

// Post a comment, then read the page of comments back.
SendCommentResult posted = api.sendComment(new NewCommentBody("Thanks!"), postId).execute();
CommentList comments = api.getComments(postId).count(50).execute();

// A single comment by its identifier.
CommentMessage one = api.getCommentById(postId, posted.message().body().mid()).execute();

// Edit and delete. A deleted comment cannot be restored.
api.editComment(new NewCommentBody("Thanks a lot!"), postId, one.body().mid()).execute();
api.deleteComment(postId, one.body().mid()).execute();
```

Subscribe to `COMMENT_CREATED`, `COMMENT_EDITED` and `COMMENT_REMOVED` to react to comments as
they arrive. The post a comment belongs to is in `message.recipient().postId()`. To reply to a
comment, pass `new NewMessageLink(MessageLinkType.REPLY, comment.body().mid())` as the `link` of
`NewCommentBody`. The bot's own comments arrive as `comment_created` as well, so a bot that replies
must skip them — see [`CommentsBot`](max-bot-api-examples/src/main/java/ru/max/botapi/examples/CommentsBot.java).

### Setting Bot Commands

```java
api.editMyCommands(new BotCommandsPatch(List.of(
        new BotCommand("start", "Start the bot"),
        new BotCommand("help",  "Show help")))).execute();

// An empty list removes every command.
api.editMyCommands(new BotCommandsPatch(List.of())).execute();
```

### Uploading a File

File upload is a two-step process: first obtain an upload URL from the API, then stream the file to that URL. The upload response shape and the result type depend on the `UploadType` — see [File Upload](#file-upload) below for the full picture.

```java
try (MaxUploadAPI uploadApi = new MaxUploadAPI()) {
    // Step 1: request an upload endpoint
    UploadEndpoint endpoint = api.getUploadUrl(UploadType.FILE).execute();

    // Step 2: stream the file (no heap buffering)
    FileUploadedInfo info = uploadApi.uploadFile(endpoint, Path.of("file.txt"), "file.txt");

    // Step 3: attach the uploaded token to a message. MAX processes the upload
    // asynchronously; execute() resends the message until it is ready.
    api.sendMessage(new NewMessageBody(
        "File:",
        List.of(new FileAttachmentRequest(new MediaRequestPayload(info.token()))),
        null, null, null
    )).chatId(chatId).execute();
}
```

---

## Modules

| Module | Description |
|---|---|
| `max-bot-api-core` | Model records, sealed interfaces, serializer SPI. Zero external dependencies (JDK only). |
| `max-bot-api-client` | HTTP transport (`java.net.http`), `MaxClient`, `MaxBotAPI` facade, rate limiter, retry policy. |
| `max-bot-api-jackson` | Jackson 3.x serializer adapter with custom deserializers for polymorphic types. |
| `max-bot-api-gson` | Reserved for a Gson serializer adapter; contains no implementation yet. |
| `max-bot-api-longpolling` | Long polling consumer backed by virtual threads, with exponential backoff. |
| `max-bot-api-webhook` | Embedded HTTP/HTTPS webhook server (JDK `HttpServer`) with secret-header validation. |
| `max-bot-api-test-support` | WireMock stubs, JSON fixtures, and test helpers for integration tests. |
| `max-bot-api-integration-tests` | Hand-run suite against the live API with a real bot token. Excluded from `build` and CI — see the [module README](max-bot-api-integration-tests/README.md). |
| `max-bot-api-spring-boot` | Spring Boot auto-configuration for both webhook and long-polling modes — controller, subscription registration, lifecycle management. |
| `max-bot-api-examples` | Runnable examples: `EchoBot`, `KeyboardBot`, `FileUploadBot`, `ImageUploadBot`, `VideoUploadBot`, `AudioUploadBot`, `CommentsBot`, `WebhookBot`. |

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
    .onError(e -> log.error("Polling error", e))  // optional; defaults to WARN logging
    .pollTimeout(30)                               // optional; seconds, defaults to longPollTimeout
    .build();

consumer.start();   // non-blocking; polling runs on a virtual thread
// ...
consumer.stop();    // graceful shutdown
```

The consumer calls `getUpdates` in a loop, tracking the marker returned by each response to avoid re-delivering events. Errors are passed to the `.onError()` callback; when it is not set, they are logged at WARN level. The loop never stops on an error:

- If `getUpdates` itself fails (network failure, API error), the loop retries with exponential backoff: 1s → 2s → 4s → 8s → 16s → 30s (capped). A successful poll resets it.
- If the handler throws, the consumer moves on to the next update without delay. The marker still advances, so the failed update is not delivered again.

The handler runs on the polling thread, so the next poll waits for it. Hand long work off to another executor.

To receive only specific event types, use `.types()` with one or more `UpdateType` constants:

```java
consumer = MaxLongPollingConsumer.builder()
    .api(api)
    .handler(handler)
    .types(Set.of(UpdateType.MESSAGE_CREATED, UpdateType.MESSAGE_CALLBACK))
    .build();
```

### Available update types

| `UpdateType` constant | API value | Description |
|---|---|---|
| `MESSAGE_CREATED` | `message_created` | A new message was sent to a chat |
| `MESSAGE_CALLBACK` | `message_callback` | A user tapped an inline keyboard button |
| `MESSAGE_EDITED` | `message_edited` | An existing message was edited |
| `MESSAGE_REMOVED` | `message_removed` | A message was deleted |
| `BOT_ADDED` | `bot_added` | The bot was added to a chat |
| `BOT_REMOVED` | `bot_removed` | The bot was removed from a chat |
| `USER_ADDED` | `user_added` | A user was added to a chat |
| `USER_REMOVED` | `user_removed` | A user was removed from a chat |
| `BOT_STARTED` | `bot_started` | A user started a direct conversation with the bot |
| `BOT_STOPPED` | `bot_stopped` | A user stopped (blocked) the bot |
| `CHAT_TITLE_CHANGED` | `chat_title_changed` | The chat title was changed |
| `MESSAGE_CONSTRUCTION_REQUEST` | `message_construction_request` | A message construction session was requested |
| `MESSAGE_CONSTRUCTED` | `message_constructed` | A message construction session completed |
| `MESSAGE_CHAT_CREATED` | `message_chat_created` | A new chat was created via a message |
| `DIALOG_CLEARED` | `dialog_cleared` | A user cleared the history of their dialog with the bot |
| `DIALOG_MUTED` | `dialog_muted` | A user muted notifications in their dialog with the bot |
| `DIALOG_UNMUTED` | `dialog_unmuted` | A user unmuted notifications in their dialog with the bot |
| `DIALOG_REMOVED` | `dialog_removed` | A user deleted their dialog with the bot (arrives with `bot_stopped`) |
| `COMMENT_CREATED` | `comment_created` | A new comment was published on a channel post |
| `COMMENT_EDITED` | `comment_edited` | A comment on a channel post was edited |
| `COMMENT_REMOVED` | `comment_removed` | A comment on a channel post was deleted |

`message_construction_request`, `message_constructed` and `message_chat_created` are inherited
from the TamTam API and are not part of the MAX API documentation. They are deprecated and kept only so that a response still carrying them
deserializes.

---

## Webhooks

`MaxWebhookServer` is an embedded server (the JDK's `HttpServer`) that receives POST requests from the MAX platform and dispatches each update to a handler. When a secret is configured, requests without the matching `X-Max-Bot-Api-Secret` header are rejected with 401.

```java
MaxWebhookServer server = MaxWebhookServer.builder()
    .handler(update -> {
        // handle update
    })
    .serializer(api.serializer())
    .secret("my-secret")   // also sent to MAX by register()
    .port(8443)            // default 8443
    .path("/webhook")      // default /webhook
    .build();

server.start();            // plain HTTP; use start(sslContext) for HTTPS

// Subscribe. Pass a set of UpdateType constants, or null to receive every type.
server.register(api, "https://example.com/webhook",
    Set.of(UpdateType.MESSAGE_CREATED, UpdateType.MESSAGE_CALLBACK));
```

See the [update types table](#available-update-types) above.

MAX delivers webhooks only to HTTPS URLs. Either terminate TLS in front of the server with a reverse proxy (nginx, a load balancer) and call `start()`, or pass an `SSLContext` loaded with your certificate to `start(sslContext)`. `register()` sends the configured secret to MAX with the subscription, so MAX includes it in every delivery.

`register()` and `unregister()` throw `IllegalStateException` when MAX answers with `success: false`. A complete bot that subscribes on startup and unsubscribes on shutdown is in [`WebhookBot`](max-bot-api-examples/src/main/java/ru/max/botapi/examples/WebhookBot.java).

#### Delivery deadline

MAX waits up to **30 seconds** for HTTP 200. A slower answer, or any other status, counts as a failed delivery: MAX retries up to 10 times at growing intervals (60 s, 150 s, 375 s, …) and unsubscribes the bot after 8 hours without a successful answer.

By default the handler runs before the server answers. A handler that can take that long should get a dispatch executor: the server then answers at once and runs the handler afterwards.

```java
ExecutorService dispatch = Executors.newVirtualThreadPerTaskExecutor();

MaxWebhookServer server = MaxWebhookServer.builder()
    // ...
    .dispatchExecutor(dispatch)
    .build();
```

Either way the server answers 200 even when the handler throws, so MAX does not redeliver an update the handler failed on. If an update must not be lost, have the handler persist it (a queue, a table) and process it from there.

### Spring Boot Integration

The `max-bot-api-spring-boot` module provides zero-boilerplate setup for both webhook and long-polling modes via auto-configuration. Add the dependency and choose the mode that fits your deployment.

The starter targets Spring Boot 4.x. It builds `MaxBotAPI` with `MaxBotAPI.create(token)`, which needs `max-bot-api-jackson` on the classpath. Webhook mode also needs Spring MVC, which the starter does not bring in:

```kotlin
dependencies {
    implementation("ru.etsft.max:max-bot-api-spring-boot:0.5.0")
    implementation("ru.etsft.max:max-bot-api-jackson:0.5.0")
    implementation("org.springframework.boot:spring-boot-starter-webmvc") // webhook mode only
}
```

The auto-configured `MaxBotAPI` uses the default `MaxClientConfig`. To change client settings (timeouts, `attachmentReadyTimeout`, …), declare your own `MaxBotAPI` bean; the starter then uses it instead:

```java
@Bean
MaxBotAPI maxBotAPI(@Value("${max.bot.webhook.token}") String token) {
    return MaxBotAPI.create(token, MaxClientConfig.builder()
        .attachmentReadyTimeout(Duration.ofMinutes(2))
        .build());
}
```

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
- With `max.bot.webhook.async-dispatch: true`, answers MAX before running the handler, which then runs on a virtual thread. See [Delivery deadline](#delivery-deadline).

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
| `max.bot.webhook.async-dispatch` | `false` | Answer MAX first, then run the `UpdateHandler` on a virtual thread. |
| `max.bot.webhook.update-types` | — | List of `UpdateType` constants to subscribe to (empty = all). See [update types table](#available-update-types). |

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
        - MESSAGE_CREATED
        - MESSAGE_CALLBACK
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

To handle polling errors, declare a `PollingErrorHandler` bean. It is optional — when absent, errors are logged at WARN level:

```java
@Bean
PollingErrorHandler pollingErrorHandler() {
    return e -> log.error("Polling error", e);
}
```

##### Long Polling Configuration Properties

| Property | Default | Description |
|---|---|---|
| `max.bot.longpolling.token` | — | Bot access token (required). |
| `max.bot.longpolling.poll-timeout` | — | Poll timeout in seconds (defaults to `MaxClientConfig` value, 30s). |
| `max.bot.longpolling.update-types` | — | List of `UpdateType` constants to receive (empty = all). See [update types table](#available-update-types). |

#### Choosing Between Modes

The `max.bot.mode` property selects the update delivery mechanism.

| Value | Description |
|---|---|
| `webhook` | Receive updates via HTTP webhook callbacks. Requires a public HTTPS endpoint. |
| `longpolling` | Receive updates by polling the MAX API. No public URL required. |
| `none` | Explicitly disable all bot update delivery. Neither auto-configuration activates. |

Only one mode can be active at a time. When the property is not set, a WARN log is emitted at startup to remind you to configure it. Setting `max.bot.mode=none` silences the warning and makes the intent explicit — useful for test profiles or applications where the bot starter is on the classpath but not needed.

Both `webhook` and `longpolling` modes use the same `UpdateHandler` interface (`ru.max.botapi.core.UpdateHandler`). Define a single handler bean and switch modes by changing only the `max.bot.mode` property — no code changes required.

Example — disable the bot in a test profile:

```yaml
# application-test.yml
max:
  bot:
    mode: none
```

---

## File Upload

The MAX API requires a two-step upload workflow:

1. **Request an upload URL** — call `api.getUploadUrl(UploadType)` to receive a pre-signed `UploadEndpoint`.
2. **Stream the file** — call the `MaxUploadAPI` method that matches the upload type. File-based overloads stream directly from disk via `java.net.http`'s `BodyPublisher`, so arbitrarily large files are transferred without loading them into the heap.

The MAX platform returns three different response shapes depending on the upload type, and the moment when the attachment token becomes available also differs. To keep this explicit and type-safe, `MaxUploadAPI` exposes one method per type, each returning a dedicated subtype of the sealed `UploadedInfo` interface.

| `UploadType` | Method                  | Result               | Token comes from         |
|--------------|-------------------------|----------------------|--------------------------|
| `FILE`       | `uploadFile(...)`       | `FileUploadedInfo`   | upload response (JSON)   |
| `IMAGE`      | `uploadImage(...)`      | `ImageUploadedInfo`  | upload response (JSON)   |
| `VIDEO`      | `uploadMedia(...)`      | `MediaUploadedInfo`  | `UploadEndpoint.token()` |
| `AUDIO`      | `uploadMedia(...)`      | `MediaUploadedInfo`  | `UploadEndpoint.token()` |

For video and audio the upload response is a tiny XML body (`<retval>1</retval>`) and carries no token; the attachment token must be taken from the `UploadEndpoint` returned by `POST /uploads`. `uploadMedia(...)` carries that token forward into the returned `MediaUploadedInfo` so callers don't have to thread it through manually.

`MaxUploadAPI` owns its own HTTP client: create one, reuse it for all uploads, and close it on shutdown. The snippets below assume an open `uploadApi`.

#### File

```java
UploadEndpoint endpoint = api.getUploadUrl(UploadType.FILE).execute();
FileUploadedInfo info = uploadApi.uploadFile(endpoint, Path.of("/tmp/doc.pdf"), "doc.pdf");

FileAttachmentRequest att =
    new FileAttachmentRequest(new MediaRequestPayload(info.token()));
api.sendMessage(new NewMessageBody("File:", List.of(att), null, null, null))
    .chatId(chatId).execute();
```

#### Image

```java
UploadEndpoint endpoint = api.getUploadUrl(UploadType.IMAGE).execute();
ImageUploadedInfo info = uploadApi.uploadImage(endpoint, Path.of("/tmp/photo.jpg"), "photo.jpg");

// Pass the photos map verbatim into the request payload.
ImageAttachmentRequest att = new ImageAttachmentRequest(
    new PhotoAttachmentRequestPayload(null, null, info.photos()));
api.sendMessage(new NewMessageBody(null, List.of(att), null, null, null))
    .chatId(chatId).execute();
```

#### Video / Audio

```java
UploadEndpoint endpoint = api.getUploadUrl(UploadType.VIDEO).execute();
MediaUploadedInfo info = uploadApi.uploadMedia(endpoint, Path.of("/tmp/clip.mp4"), "clip.mp4");

// info.token() == endpoint.token() (the upload response carries no token).
VideoAttachmentRequest att =
    new VideoAttachmentRequest(new MediaRequestPayload(info.token()));
api.sendMessage(new NewMessageBody("Video:", List.of(att), null, null, null))
    .chatId(chatId).execute();
```

The MAX server processes uploaded media asynchronously, which can take from about a second to much longer for large videos. Until it has finished, `sendMessage` answers HTTP 400 with the code `attachment.not.ready`. The client handles this itself: it resends the same request with a short backoff (0.5s, 1s, 2s, then every 3s) for up to `attachmentReadyTimeout` (30 seconds by default). Resending is safe — the rejected message was not created, and the upload token stays valid.

Only if the attachment is still not ready after that does the call throw `AttachmentNotReadyException` (a subclass of `MaxApiException`). Raise the timeout for large media, or set it to `Duration.ZERO` to handle the exception yourself:

```java
MaxClientConfig config = MaxClientConfig.builder()
    .attachmentReadyTimeout(Duration.ofMinutes(2))
    .build();
```

The wait blocks the calling thread. When you send attachments from a webhook handler, it counts against the webhook delivery deadline — use `enqueue()` or async dispatch (see [Delivery deadline](#delivery-deadline)).

---

## Configuration

`MaxClientConfig` is a Java record with a builder. The defaults are appropriate for most production use cases.

### Default values

| Parameter | Default                       |
|---|-------------------------------|
| `baseUrl` | `https://platform-api2.max.ru` |
| `connectTimeout` | 10 seconds                    |
| `requestTimeout` | 60 seconds                    |
| `longPollTimeout` | 30 seconds                    |
| `maxRetries` | 3                             |
| `enableRateLimiting` | `true`                        |
| `maxRequestsPerSecond` | 30                            |
| `attachmentReadyTimeout` | 30 seconds                  |
| `sslContext` | JVM trust store + bundled MAX trusted certificates |

### Custom configuration

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

### TLS certificates

The client includes the Russian Trusted Root CA and Russian Trusted Sub CA certificates
required by MAX and uses them by default. No additional JVM trust store setup is required
for the default `platform-api2.max.ru` endpoint.

To add your own `.crt` or `.cer` files on top of the bundled certificates:

```java
MaxClientConfig config = MaxClientConfig.builder()
    .trustedCertificates(
        Path.of("company-root-ca.crt"),
        Path.of("company-sub-ca.crt"))
    .build();

MaxBotAPI api = MaxBotAPI.create("your-token", config);
```

To use only the JVM default SSL configuration and skip bundled certificates:

```java
MaxClientConfig config = MaxClientConfig.builder()
    .withoutBundledTrustedCertificates()
    .build();
```

For full control, pass a custom `SSLContext` with `.sslContext(...)`.

### Using defaults

```java
MaxClientConfig config = MaxClientConfig.defaults();
```

### Rate limits

The built-in limiter caps the client as a whole at `maxRequestsPerSecond` (30 by default), the platform-wide limit. MAX also allows at most **2 operations per second in a single dialog, group chat or channel** for sending messages (`POST /messages`), editing them (`PUT /messages`) and answering callbacks (`POST /answers`). The client does not track that per-chat limit, so a bot that writes to one chat in bursts must queue or delay those calls itself.

---

## Error Handling

All exceptions are unchecked and extend `RuntimeException`.

### Exception hierarchy

```
RuntimeException
├── MaxClientException                — transport/network failure (I/O error, timeout)
└── MaxApiException                   — API returned 4xx or 5xx
    ├── MaxRateLimitException         — 429 Too Many Requests
    ├── MaxMethodNotAllowedException  — 405 Method Not Allowed
    └── AttachmentNotReadyException   — attachment still processing after attachmentReadyTimeout
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

## Migrating to 0.4.0

0.4.0 brings the models in line with the MAX API schema. These changes break source and binary compatibility:

| Before | After |
|---|---|
| `CommentRemovedUpdate(timestamp, message)` | `CommentRemovedUpdate(timestamp, messageId, chatId, userId, postId)`: the comment is gone, so only its identifiers arrive |
| `new OpenAppButton(text, url, payload)` | `OpenAppButton.ofWebApp(text, "bot_username")` or `new OpenAppButton(text, webApp, contactId, payload)`; `webApp` is required, MAX refuses the button without it |
| `new MessageButton(text, message)` | `new MessageButton(text)`: the button sends its own text |
| `Message.body()` is never `null` | `null` when the message only forwards another one; check it before `body().text()` |
| `ChatPatch.icon` is an `Image` | a `PhotoAttachmentRequestPayload`: an external URL, a token, or uploaded `photos` |
| `lastActivityTime()` is a `long`, `0` when absent | a `Long`, `null` when privacy settings hide it (`User`, `UserWithPhoto`, `BotInfo`, `ChatMember`) |
| `MarkupElement(type, from, length)` | adds `url`, `userLink` and `userId` |
| `DialogClearedUpdate`, `DialogMutedUpdate`, `DialogUnmutedUpdate`, `DialogRemovedUpdate`, `BotStoppedUpdate` | add `userLocale`; `DialogMutedUpdate` also `mutedUntil` |
| `MessageRemovedUpdate(timestamp, messageId, chatId, userId)` | adds `postId` |
| `Subscription(url, updateTypes)` | `Subscription(url, time, updateTypes)` |
| `AudioAttachment(payload)` | `AudioAttachment(payload, transcription)` |
| `MaxWebhookServer.register()` / `unregister()` ignore `success: false` | they throw `IllegalStateException` |

---

## Building from Source

Requires JDK 21+. The Gradle wrapper is included, so no local Gradle installation is needed.

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

The live integration suite is deliberately not part of any of the above. It talks to the real
MAX API with a real bot token and is run by name:

```bash
./gradlew :max-bot-api-integration-tests:liveTest --console=plain
```

See [max-bot-api-integration-tests/README.md](max-bot-api-integration-tests/README.md) for the
environment variables it expects and the side effects it has.

Build output and coverage reports are placed under each module's `build/` directory. The aggregated coverage report is at `build/reports/jacoco/`.

---

## Contributing

1. Fork the repository and create a feature branch from `master`.
2. Write tests for any new functionality. Coverage gates must continue to pass (≥ 85% line, ≥ 80% branch).
3. Ensure `./gradlew check` passes (Checkstyle + SpotBugs) before submitting a pull request.
4. Keep public API surface minimal. New models should use Java records; new union types should use sealed interfaces.
5. Submit a pull request with a clear description of the change and its rationale.

For bug reports and feature requests, open an issue on [GitHub](https://github.com/etsft/max-bot-api-java/issues).

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
- [Reference TypeScript client](https://github.com/max-messenger/max-bot-api-client-ts)
- [Reference Java 8 client](https://github.com/max-messenger/max-bot-api-client-java)
