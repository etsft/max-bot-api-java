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

import java.util.List;

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.longpolling.MaxLongPollingConsumer;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.ButtonIntent;
import ru.max.botapi.model.CallbackAnswer;
import ru.max.botapi.model.CallbackButton;
import ru.max.botapi.model.InlineKeyboardAttachment;
import ru.max.botapi.model.InlineKeyboardAttachmentRequest;
import ru.max.botapi.model.MessageCallbackUpdate;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.NewMessageBody;

/**
 * A bot that demonstrates inline keyboards and callback handling.
 *
 * <p>When the user sends any message, the bot replies with a message containing
 * an inline keyboard with three buttons. When a button is pressed, the bot
 * answers the callback with a notification showing which button was pressed.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * export MAX_BOT_TOKEN="your-bot-token"
 * ./gradlew :max-bot-api-examples:run -PmainClass=ru.max.botapi.examples.KeyboardBot
 * </pre>
 */
public final class KeyboardBot {

    private KeyboardBot() {
    }

    /**
     * Entry point for the keyboard bot.
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
                    try {
                        switch (update) {
                            case MessageCreatedUpdate msg -> handleMessage(api, msg);
                            case MessageCallbackUpdate cb -> handleCallback(api, cb);
                            default -> { /* ignore other update types */ }
                        }
                    } catch (Exception e) {
                        System.err.println("Error: " + e.getMessage());
                    }
                })
                .build()) {

            consumer.start();
            System.out.println("KeyboardBot is running. Press Ctrl+C to stop.");

            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void handleMessage(MaxBotAPI api, MessageCreatedUpdate msg) {
        Long chatId = msg.message().recipient().chatId();
        if (chatId == null) {
            return;
        }

        // Build inline keyboard with three buttons in two rows
        List<List<Button>> buttons = List.of(
                List.of(
                        new CallbackButton("Hello", "btn_hello", ButtonIntent.POSITIVE),
                        new CallbackButton("World", "btn_world", ButtonIntent.DEFAULT)
                ),
                List.of(
                        new CallbackButton("Info", "btn_info", ButtonIntent.DEFAULT)
                )
        );

        InlineKeyboardAttachment.KeyboardPayload payload =
                new InlineKeyboardAttachment.KeyboardPayload(buttons);

        InlineKeyboardAttachmentRequest keyboard = new InlineKeyboardAttachmentRequest(payload);

        NewMessageBody body = new NewMessageBody(
                "Choose an option:",
                List.of(keyboard),
                null, null, null
        );

        api.sendMessage(body).chatId(chatId).execute();
    }

    private static void handleCallback(MaxBotAPI api, MessageCallbackUpdate cb) {
        String callbackId = cb.callback().callbackId();
        String payload = cb.callback().payload();

        String text = switch (payload) {
            case "btn_hello" -> "You pressed Hello!";
            case "btn_world" -> "You pressed World!";
            case "btn_info" -> "This is the KeyboardBot example.";
            default -> "Unknown button: " + payload;
        };

        api.answerOnCallback(new CallbackAnswer(null, text), callbackId).execute();
    }
}
