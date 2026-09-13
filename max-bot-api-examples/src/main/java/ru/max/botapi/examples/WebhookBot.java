/*
 * Copyright 2026 Boris Tarelkin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.max.botapi.examples;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.UpdateType;
import ru.max.botapi.webhook.MaxWebhookServer;

/**
 * An echo bot that receives updates through a webhook instead of long polling.
 *
 * <p>The server listens for plain HTTP on {@code PORT}; MAX only calls HTTPS URLs, so put a
 * TLS-terminating reverse proxy (nginx, a load balancer, a tunnel) in front of it and pass the
 * public URL as {@code MAX_WEBHOOK_URL}. To terminate TLS in the JVM instead, call
 * {@link MaxWebhookServer#start(javax.net.ssl.SSLContext)}.</p>
 *
 * <p>The bot subscribes on startup and unsubscribes on shutdown. MAX echoes the secret in the
 * {@code X-Max-Bot-Api-Secret} header, and requests without it are rejected. Handlers run on a
 * dispatch executor, so the server answers MAX at once however long a handler takes.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * export MAX_BOT_TOKEN="your-bot-token"
 * export MAX_WEBHOOK_URL="https://bot.example.com/webhook"
 * export MAX_WEBHOOK_SECRET="a-long-random-string"
 * export PORT=8080   # optional, defaults to 8080
 * ./gradlew :max-bot-api-examples:run -PmainClass=ru.max.botapi.examples.WebhookBot
 * </pre>
 */
public final class WebhookBot {

    private static final int DEFAULT_PORT = 8080;

    private WebhookBot() {
    }

    /**
     * Entry point for the webhook bot.
     *
     * @param args command-line arguments (unused)
     * @throws IOException if the server cannot bind to the port
     */
    public static void main(String[] args) throws IOException {
        String token = requireEnv("MAX_BOT_TOKEN");
        String webhookUrl = requireEnv("MAX_WEBHOOK_URL");
        String secret = requireEnv("MAX_WEBHOOK_SECRET");
        String portValue = System.getenv("PORT");
        int port = portValue == null || portValue.isBlank() ? DEFAULT_PORT : Integer.parseInt(portValue);

        MaxBotAPI api = MaxBotAPI.create(token);
        ExecutorService dispatch = Executors.newVirtualThreadPerTaskExecutor();

        MaxWebhookServer server = MaxWebhookServer.builder()
                .serializer(api.serializer())
                .secret(secret)
                .port(port)
                .path("/webhook")
                .dispatchExecutor(dispatch)
                .handler(update -> {
                    if (update instanceof MessageCreatedUpdate msg) {
                        var body = msg.message().body();
                        String text = body == null ? null : body.text();
                        Long chatId = msg.message().recipient().chatId();
                        if (text != null && !text.isBlank() && chatId != null) {
                            api.sendMessage(new NewMessageBody(text, null, null, null, null))
                                    .chatId(chatId)
                                    .execute();
                        }
                    }
                })
                .build();

        server.start();
        server.register(api, webhookUrl, Set.of(UpdateType.MESSAGE_CREATED));
        System.out.printf("WebhookBot is listening on port %d, path %s. Press Ctrl+C to stop.%n",
                server.getPort(), server.getPath());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.unregister(api, webhookUrl);
            } catch (RuntimeException e) {
                System.err.println("Failed to unregister the webhook: " + e.getMessage());
            }
            server.close();
            dispatch.close();
            api.close();
        }));
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            System.err.println("Set the " + name + " environment variable.");
            System.exit(1);
        }
        return value;
    }
}
