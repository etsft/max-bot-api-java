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
import ru.max.botapi.model.MediaRequestPayload;
import ru.max.botapi.model.MediaUploadedInfo;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.UploadEndpoint;
import ru.max.botapi.model.UploadType;
import ru.max.botapi.model.VideoAttachmentRequest;

/**
 * A bot that demonstrates video upload functionality.
 *
 * <p>When the user sends the command {@code /upload}, the bot uploads video to the MAX
 * platform, and sends it back to the chat.
 * This demonstrates the two-step upload flow:</p>
 * <ol>
 *   <li>Obtain an upload URL via {@code POST /uploads}</li>
 *   <li>Upload the video to that URL</li>
 *   <li>Send a message with the uploaded video token as an attachment</li>
 * </ol>
 *
 * <p>Usage:</p>
 * <pre>
 * export MAX_BOT_TOKEN="your-bot-token"
 * ./gradlew :max-bot-api-examples:run -PmainClass=ru.max.botapi.examples.VideoUploadBot
 * </pre>
 */
public final class VideoUploadBot {

    private VideoUploadBot() {
    }

    /**
     * Entry point for the video upload bot.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        String token = System.getenv("MAX_BOT_TOKEN");
        if (token == null || token.isBlank()) {
            System.err.println("Set the MAX_BOT_TOKEN environment variable.");
            System.exit(1);
        }

        final String videoPath = System.getenv("MAX_VIDEO_PATH");
        if (videoPath == null || videoPath.isBlank()) {
            System.err.println("Set the MAX_VIDEO_PATH environment variable.");
            System.exit(1);
        }
        if (Files.notExists(Path.of(videoPath))) {
            System.err.printf("File %s not found%n", videoPath);
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
                                 handleUploadCommand(api, uploadApi, msg, videoPath);
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
                                             MessageCreatedUpdate msg, String videoPath) {
        Long chatId = msg.message().recipient().chatId();
        if (chatId == null) {
            return;
        }
        try {
            Path video = Path.of(videoPath);

            // Step 2: Get upload URL
            UploadEndpoint endpoint = api.getUploadUrl(UploadType.VIDEO).execute();

            // Step 3: Upload the video (
            MediaUploadedInfo uploaded = uploadApi.uploadMedia(endpoint, video,
                    video.getName(video.getNameCount() - 1).toString());

            // Step 4: Send message with uploaded video
            VideoAttachmentRequest videoAttachment =
                    new VideoAttachmentRequest(new MediaRequestPayload(endpoint.token()));

            NewMessageBody body = new NewMessageBody(
                    "Here is your video:",
                    List.of(videoAttachment),
                    null, null, null
            );

            api.sendMessage(body).chatId(chatId).execute();
        } catch (Exception e) {
            System.err.println("Failed to upload video: " + e.getMessage());
        }
    }
}
