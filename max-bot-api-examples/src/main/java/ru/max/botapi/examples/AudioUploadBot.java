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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.client.MaxUploadAPI;
import ru.max.botapi.longpolling.MaxLongPollingConsumer;
import ru.max.botapi.model.AudioAttachmentRequest;
import ru.max.botapi.model.MediaRequestPayload;
import ru.max.botapi.model.MediaUploadedInfo;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.UploadEndpoint;
import ru.max.botapi.model.UploadType;

/**
 * A bot that demonstrates audio upload functionality.
 *
 * <p>When the user sends the command {@code /upload}, the bot uploads audio to the MAX
 * platform, and sends it back to the chat.
 * This demonstrates the two-step upload flow:</p>
 * <ol>
 *   <li>Obtain an upload URL via {@code POST /uploads}</li>
 *   <li>Upload the audio to that URL</li>
 *   <li>Send a message with the uploaded audio token as an attachment</li>
 * </ol>
 *
 * <p>Usage:</p>
 * <pre>
 * export MAX_BOT_TOKEN="your-bot-token"
 * ./gradlew :max-bot-api-examples:run -PmainClass=ru.max.botapi.examples.AudioUploadBot
 * </pre>
 */
public final class AudioUploadBot {

    private AudioUploadBot() {
    }

    /**
     * Entry point for the audio upload bot.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        String token = System.getenv("MAX_BOT_TOKEN");
        if (token == null || token.isBlank()) {
            System.err.println("Set the MAX_BOT_TOKEN environment variable.");
            System.exit(1);
        }

        final String audioPath = System.getenv("MAX_AUDIO_PATH");
        if (audioPath == null || audioPath.isBlank()) {
            System.err.println("Set the MAX_AUDIO_PATH environment variable.");
            System.exit(1);
        }
        if (Files.notExists(Path.of(audioPath))) {
            System.err.printf("File %s not found%n", audioPath);
            System.exit(1);
        }

        MaxBotAPI api = MaxBotAPI.create(token);

        try (MaxUploadAPI uploadApi = new MaxUploadAPI();
             MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
                     .api(api)
                     .handler(update -> {
                         if (update instanceof MessageCreatedUpdate msg) {
                             String text = msg.message().body().text();
                             if ("/upload".equals(text)) {
                                 handleUploadCommand(api, uploadApi, msg, audioPath);
                             }
                         }
                     })
                     .build()) {

            consumer.start();
            System.out.println("FileUploadBot is running. Send /upload to test. Press Ctrl+C to stop.");

            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void handleUploadCommand(MaxBotAPI api, MaxUploadAPI uploadApi,
                                            MessageCreatedUpdate msg, String audioPath) {
        Long chatId = msg.message().recipient().chatId();
        if (chatId == null) {
            return;
        }
        try {
            Path audio = Path.of(audioPath);

            // Step 2: Get upload URL
            UploadEndpoint endpoint = api.getUploadUrl(UploadType.AUDIO).execute();

            // Step 3: Upload the audio (
            MediaUploadedInfo uploaded = uploadApi.uploadMedia(endpoint, audio,
                    audio.getName(audio.getNameCount() - 1).toString());

            // Step 4: Send message with uploaded audio
            AudioAttachmentRequest audioAttachment =
                    new AudioAttachmentRequest(new MediaRequestPayload(endpoint.token()));

            NewMessageBody body = new NewMessageBody(
                    "Here is your audio:",
                    List.of(audioAttachment),
                    null, null, null
            );

            api.sendMessage(body).chatId(chatId).execute();
        } catch (Exception e) {
            System.err.println("Failed to upload audio: " + e.getMessage());
        }
    }
}
