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

package ru.max.botapi.it;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import ru.max.botapi.client.MaxUploadAPI;
import ru.max.botapi.model.AttachmentRequest;
import ru.max.botapi.model.AudioAttachmentRequest;
import ru.max.botapi.model.FileAttachmentRequest;
import ru.max.botapi.model.FileUploadedInfo;
import ru.max.botapi.model.ImageAttachmentRequest;
import ru.max.botapi.model.ImageUploadedInfo;
import ru.max.botapi.model.MediaRequestPayload;
import ru.max.botapi.model.MediaUploadedInfo;
import ru.max.botapi.model.Message;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.PhotoAttachmentRequestPayload;
import ru.max.botapi.model.SendMessageResult;
import ru.max.botapi.model.UploadEndpoint;
import ru.max.botapi.model.UploadType;
import ru.max.botapi.model.VideoAttachmentDetails;
import ru.max.botapi.model.VideoAttachmentRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The upload flow end to end: reserve an endpoint, push the bytes, attach the token.
 *
 * <p>{@code MaxUploadAPI} is not reachable from {@code MaxBotAPI} — it posts multipart data
 * straight to the endpoint URL — so it is constructed and closed here.</p>
 *
 * <p>A freshly uploaded attachment is not immediately usable: the API answers HTTP 400
 * {@code attachment.not.ready} until it has processed the media. The client resends the message
 * itself for up to {@code attachmentReadyTimeout}, so every send here is a single call; waiting
 * that out is part of the contract being tested.</p>
 */
@Order(4)
@DisplayName("Live: uploads and attachments")
class AttachmentLiveTest extends LiveTestBase {

    private final MaxUploadAPI uploads = new MaxUploadAPI(new RecordingSerializer());

    private final List<String> sentMessageIds = new ArrayList<>();

    private String videoToken;

    @Test
    @Order(1)
    @DisplayName("getUploadUrl works for every UploadType")
    void getUploadUrl() {
        for (UploadType type : UploadType.values()) {
            UploadEndpoint endpoint = api().getUploadUrl(type).execute();

            assertThat(endpoint.url())
                    .withFailMessage("getUploadUrl(%s) returned no URL", type)
                    .isNotBlank();
        }
    }

    @Test
    @Order(2)
    @DisplayName("image upload round trip")
    void uploadImage() {
        UploadEndpoint endpoint = api().getUploadUrl(UploadType.IMAGE).execute();
        Path image = Fixtures.image();

        ImageUploadedInfo info = uploads.uploadImage(endpoint, image, image.getFileName().toString());

        assertThat(info.photos()).isNotEmpty();
        send(new ImageAttachmentRequest(
                new PhotoAttachmentRequestPayload(null, null, info.photos())), "image");
    }

    @Test
    @Order(3)
    @DisplayName("file upload round trip")
    void uploadFile() {
        UploadEndpoint endpoint = api().getUploadUrl(UploadType.FILE).execute();
        Path file = Fixtures.textFile();

        FileUploadedInfo info = uploads.uploadFile(endpoint, file, file.getFileName().toString());

        assertThat(info.token()).isNotBlank();
        send(new FileAttachmentRequest(new MediaRequestPayload(info.token())), "file");
    }

    @Test
    @Order(4)
    @DisplayName("video upload round trip")
    void uploadVideo() {
        Path video = Fixtures.video();
        UploadEndpoint endpoint = api().getUploadUrl(UploadType.VIDEO).execute();

        assertThat(endpoint.token())
                .withFailMessage("The video upload endpoint carried no token; "
                        + "uploadMedia cannot proceed without one")
                .isNotBlank();

        MediaUploadedInfo info = uploads.uploadMedia(endpoint, video, video.getFileName().toString());

        assertThat(info.token()).isNotBlank();
        videoToken = info.token();
        send(new VideoAttachmentRequest(new MediaRequestPayload(info.token())), "video");
    }

    @Test
    @Order(5)
    @DisplayName("getVideoAttachmentDetails resolves the uploaded video")
    void getVideoDetails() {
        Assumptions.assumeTrue(videoToken != null,
                "no video was uploaded, see " + IntegrationConfig.VIDEO_PATH);

        VideoAttachmentDetails details = api().getVideoAttachmentDetails(videoToken).execute();

        assertThat(details.token()).isEqualTo(videoToken);
        // Renditions appear as MAX transcodes: right after the upload only a low resolution
        // exists, and sometimes none at all. Assert the shape, not the transcoding progress.
        if (details.urls() != null) {
            assertThat(Stream.of(details.urls().mp41080(), details.urls().mp4720(),
                            details.urls().mp4480(), details.urls().mp4360(),
                            details.urls().mp4240(), details.urls().mp4144(),
                            details.urls().hls())
                    .filter(Objects::nonNull).toList())
                    .withFailMessage("urls was present but held no URL: %s", details.urls())
                    .isNotEmpty();
        }
    }

    @Test
    @Order(6)
    @DisplayName("audio upload round trip")
    void uploadAudio() {
        Path audio = Fixtures.audio();
        UploadEndpoint endpoint = api().getUploadUrl(UploadType.AUDIO).execute();

        assertThat(endpoint.token()).isNotBlank();

        MediaUploadedInfo info = uploads.uploadMedia(endpoint, audio, audio.getFileName().toString());

        assertThat(info.token()).isNotBlank();
        send(new AudioAttachmentRequest(new MediaRequestPayload(info.token())), "audio");
    }

    /**
     * Sends a message carrying the attachment; the client waits out {@code attachment.not.ready}.
     */
    private void send(AttachmentRequest attachment, String label) {
        NewMessageBody body = new NewMessageBody(
                "Live suite: " + label, List.of(attachment), null, false, null);

        SendMessageResult result = api().sendMessage(body)
                .chatId(IntegrationConfig.chatId())
                .execute();
        Message message = result.message();
        ModelAssertions.assertFullyMapped(message);
        assertThat(message.body().attachments())
                .withFailMessage("The %s attachment did not come back on the message", label)
                .isNotNull();
        sentMessageIds.add(message.body().mid());
    }

    /**
     * Deletes the messages this class posted and closes the upload client.
     */
    @AfterAll
    void cleanUpUploads() {
        sentMessageIds.forEach(mid ->
                cleanUp("delete message " + mid, () -> api().deleteMessage(mid).execute()));
        // Not routed through cleanUp: the upload client is built with the test instance, so it
        // exists even when the class was skipped before the API was opened.
        try {
            uploads.close();
        } catch (RuntimeException e) {
            System.out.println("Cleanup failed (close upload API): " + e);
        }
    }
}
