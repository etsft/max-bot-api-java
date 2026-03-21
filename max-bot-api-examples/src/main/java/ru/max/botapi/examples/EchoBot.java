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

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.longpolling.MaxLongPollingConsumer;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.NewMessageBody;

/**
 * A simple echo bot that replies to every incoming text message with the same text.
 *
 * <p>Usage:</p>
 * <pre>
 * export MAX_BOT_TOKEN="your-bot-token"
 * ./gradlew :max-bot-api-examples:run -PmainClass=ru.max.botapi.examples.EchoBot
 * </pre>
 */
public final class EchoBot {

    private EchoBot() {
    }

    /**
     * Entry point for the echo bot.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        String token = System.getenv("MAX_BOT_TOKEN");
        if (token == null || token.isBlank()) {
            System.err.println("Set the MAX_BOT_TOKEN environment variable.");
            System.exit(1);
        }

        MaxBotAPI api = MaxBotAPI.create(token);

        try (MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
                .api(api)
                .handler(update -> {
                    if (update instanceof MessageCreatedUpdate msg) {
                        String text = msg.message().body().text();
                        Long chatId = msg.message().recipient().chatId();
                        if (text != null && !text.isBlank() && chatId != null) {
                            try {
                                api.sendMessage(new NewMessageBody(text, null, null, null, null))
                                        .chatId(chatId)
                                        .execute();
                            } catch (Exception e) {
                                System.err.println("Failed to send echo: " + e.getMessage());
                            }
                        }
                    }
                })
                .build()) {

            consumer.start();
            System.out.println("EchoBot is running. Press Ctrl+C to stop.");

            // Keep the main thread alive until interrupted
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
