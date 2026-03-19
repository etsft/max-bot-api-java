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

package ru.max.botapi.model;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the AttachmentRequest sealed hierarchy.
 */
class AttachmentRequestTest {

    @Test
    void imageAttachmentRequest_construction() {
        var payload = new PhotoAttachmentRequestPayload("tok", null, null);
        var req = new ImageAttachmentRequest(payload);
        assertThat(req.type()).isEqualTo("image");
        assertThat(req.payload().token()).isEqualTo("tok");
    }

    @Test
    void videoAttachmentRequest_construction() {
        var req = new VideoAttachmentRequest(new MediaRequestPayload("vtok"));
        assertThat(req.type()).isEqualTo("video");
    }

    @Test
    void audioAttachmentRequest_construction() {
        var req = new AudioAttachmentRequest(new MediaRequestPayload("atok"));
        assertThat(req.type()).isEqualTo("audio");
    }

    @Test
    void fileAttachmentRequest_construction() {
        var req = new FileAttachmentRequest(new MediaRequestPayload("ftok"));
        assertThat(req.type()).isEqualTo("file");
    }

    @Test
    void stickerAttachmentRequest_construction() {
        var payload = new StickerAttachmentRequest.StickerRequestPayload("smile");
        var req = new StickerAttachmentRequest(payload);
        assertThat(req.type()).isEqualTo("sticker");
    }

    @Test
    void contactAttachmentRequest_construction() {
        var payload = new ContactAttachmentRequest.ContactRequestPayload(
                "John", 123L, null, "+1234567890");
        var req = new ContactAttachmentRequest(payload);
        assertThat(req.type()).isEqualTo("contact");
        assertThat(req.payload().name()).isEqualTo("John");
    }

    @Test
    void inlineKeyboardAttachmentRequest_construction() {
        var kp = new InlineKeyboardAttachment.KeyboardPayload(List.of());
        var req = new InlineKeyboardAttachmentRequest(kp);
        assertThat(req.type()).isEqualTo("inline_keyboard");
    }

    @Test
    void shareAttachmentRequest_construction() {
        var req = new ShareAttachmentRequest(null);
        assertThat(req.type()).isEqualTo("share");
        assertThat(req.payload()).isNull();
    }

    @Test
    void locationAttachmentRequest_construction() {
        var req = new LocationAttachmentRequest(55.75, 37.62);
        assertThat(req.type()).isEqualTo("location");
    }

    @Test
    void unknownAttachmentRequest_construction() {
        var req = new UnknownAttachmentRequest("future_type", "{}");
        assertThat(req.type()).isEqualTo("future_type");
    }

    @Test
    void exhaustiveSwitch_coversAllTypes() {
        AttachmentRequest[] all = {
                new ImageAttachmentRequest(new PhotoAttachmentRequestPayload(null, null, null)),
                new VideoAttachmentRequest(new MediaRequestPayload(null)),
                new AudioAttachmentRequest(new MediaRequestPayload(null)),
                new FileAttachmentRequest(new MediaRequestPayload(null)),
                new StickerAttachmentRequest(
                        new StickerAttachmentRequest.StickerRequestPayload("c")),
                new ContactAttachmentRequest(
                        new ContactAttachmentRequest.ContactRequestPayload(null, null, null, null)),
                new InlineKeyboardAttachmentRequest(
                        new InlineKeyboardAttachment.KeyboardPayload(List.of())),
                new ShareAttachmentRequest(null),
                new LocationAttachmentRequest(0, 0),
                new UnknownAttachmentRequest("x", "{}")
        };
        for (AttachmentRequest req : all) {
            String desc = switch (req) {
                case ImageAttachmentRequest r -> "image";
                case VideoAttachmentRequest r -> "video";
                case AudioAttachmentRequest r -> "audio";
                case FileAttachmentRequest r -> "file";
                case StickerAttachmentRequest r -> "sticker";
                case ContactAttachmentRequest r -> "contact";
                case InlineKeyboardAttachmentRequest r -> "keyboard";
                case ShareAttachmentRequest r -> "share";
                case LocationAttachmentRequest r -> "location";
                case UnknownAttachmentRequest r -> "unknown";
            };
            assertThat(desc).isNotBlank();
        }
    }
}
