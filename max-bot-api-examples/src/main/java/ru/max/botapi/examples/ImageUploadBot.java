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
import ru.max.botapi.model.ImageAttachmentRequest;
import ru.max.botapi.model.ImageUploadedInfo;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.PhotoAttachmentRequestPayload;
import ru.max.botapi.model.UploadEndpoint;
import ru.max.botapi.model.UploadType;

/**
 * A bot that demonstrates image upload functionality.
 *
 * <p>When the user sends the command {@code /upload}, the bot uploads image to the MAX
 * platform, and sends it back to the chat.
 * This demonstrates the two-step upload flow:</p>
 * <ol>
 *   <li>Obtain an upload URL via {@code POST /uploads}</li>
 *   <li>Upload the image to that URL</li>
 *   <li>Send a message with the uploaded image token as an attachment</li>
 * </ol>
 *
 * <p>Usage:</p>
 * <pre>
 * export MAX_BOT_TOKEN="your-bot-token"
 * ./gradlew :max-bot-api-examples:run -PmainClass=ru.max.botapi.examples.ImageUploadBot
 * </pre>
 */
public final class ImageUploadBot {

    private ImageUploadBot() {
    }

    /**
     * Entry point for the image upload bot.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        String token = System.getenv("MAX_BOT_TOKEN");
        if (token == null || token.isBlank()) {
            System.err.println("Set the MAX_BOT_TOKEN environment variable.");
            System.exit(1);
        }

        final String imagePath = System.getenv("MAX_IMAGE_PATH");
        if (imagePath == null || imagePath.isBlank()) {
            System.err.println("Set the MAX_IMAGE_PATH environment variable.");
            System.exit(1);
        }
        if (Files.notExists(Path.of(imagePath))) {
            System.err.printf("File %s not found%n", imagePath);
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
                                 handleUploadCommand(api, uploadApi, msg, imagePath);
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
                                             MessageCreatedUpdate msg, String imagePath) {
        Long chatId = msg.message().recipient().chatId();
        if (chatId == null) {
            return;
        }
        try {
            Path image = Path.of(imagePath);

            // Step 2: Get upload URL
            UploadEndpoint endpoint = api.getUploadUrl(UploadType.IMAGE).execute();

            // Step 3: Upload the image (
            ImageUploadedInfo uploaded = uploadApi.uploadImage(endpoint, image,
                    image.getName(image.getNameCount() - 1).toString());

            // Step 4: Send message with uploaded image
            ImageAttachmentRequest imageAttachment =
                    new ImageAttachmentRequest(new PhotoAttachmentRequestPayload(
                            endpoint.token(), null, uploaded.photos()));

            NewMessageBody body = new NewMessageBody(
                    "Here is your image:",
                    List.of(imageAttachment),
                    null, null, null
            );

            api.sendMessage(body).chatId(chatId).execute();
        } catch (Exception e) {
            System.err.println("Failed to upload image: " + e.getMessage());
        }
    }
}
