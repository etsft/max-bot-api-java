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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the Attachment sealed hierarchy — construction and exhaustive switch.
 */
class AttachmentTest {

    @Test
    void photoAttachment_construction() {
        var payload = new PhotoAttachment.PhotoPayload("http://img", "tok1", 42L);
        var att = new PhotoAttachment(payload);
        assertThat(att.type()).isEqualTo("image");
        assertThat(att.payload().url()).isEqualTo("http://img");
        assertThat(att.payload().photoId()).isEqualTo(42L);
    }

    @Test
    void videoAttachment_construction() {
        var payload = new VideoAttachment.VideoPayload("http://vid", "vtok", null);
        var thumb = new VideoThumbnail("http://thumb");
        var att = new VideoAttachment(payload, thumb, 1920, 1080, 120);
        assertThat(att.type()).isEqualTo("video");
        assertThat(att.thumbnail().url()).isEqualTo("http://thumb");
        assertThat(att.width()).isEqualTo(1920);
    }

    @Test
    void audioAttachment_construction() {
        var att = new AudioAttachment(new MediaPayload("http://audio", "atok"));
        assertThat(att.type()).isEqualTo("audio");
    }

    @Test
    void fileAttachment_construction() {
        var att = new FileAttachment(new MediaPayload("http://file", "ftok"),
                "doc.pdf", 1024L);
        assertThat(att.type()).isEqualTo("file");
        assertThat(att.filename()).isEqualTo("doc.pdf");
        assertThat(att.size()).isEqualTo(1024L);
    }

    @Test
    void stickerAttachment_construction() {
        var payload = new StickerAttachment.StickerPayload("http://sticker", "smile");
        var att = new StickerAttachment(payload, 128, 128);
        assertThat(att.type()).isEqualTo("sticker");
        assertThat(att.width()).isEqualTo(128);
    }

    @Test
    void contactAttachment_construction() {
        var payload = new ContactAttachment.ContactPayload("vcf-data", null);
        var att = new ContactAttachment(payload);
        assertThat(att.type()).isEqualTo("contact");
        assertThat(att.payload().vcfInfo()).isEqualTo("vcf-data");
    }

    @Test
    void inlineKeyboardAttachment_construction() {
        var button = new CallbackButton("Click", "data", null);
        var payload = new InlineKeyboardAttachment.KeyboardPayload(
                java.util.List.of(java.util.List.of(button)));
        var att = new InlineKeyboardAttachment(payload);
        assertThat(att.type()).isEqualTo("inline_keyboard");
        assertThat(att.payload().buttons()).hasSize(1);
    }

    @Test
    void shareAttachment_construction() {
        var att = new ShareAttachment(null, "Title", "Desc", "http://img");
        assertThat(att.type()).isEqualTo("share");
        assertThat(att.title()).isEqualTo("Title");
    }

    @Test
    void locationAttachment_construction() {
        var att = new LocationAttachment(55.75, 37.62);
        assertThat(att.type()).isEqualTo("location");
        assertThat(att.latitude()).isEqualTo(55.75);
        assertThat(att.longitude()).isEqualTo(37.62);
    }

    @Test
    void unknownAttachment_construction() {
        var att = new UnknownAttachment("new_type", "{\"type\":\"new_type\"}");
        assertThat(att.type()).isEqualTo("new_type");
        assertThat(att.rawJson()).contains("new_type");
    }

    @Test
    void exhaustiveSwitch_coversAllTypes() {
        Attachment[] all = {
                new PhotoAttachment(new PhotoAttachment.PhotoPayload("u", "t", 1)),
                new VideoAttachment(new VideoAttachment.VideoPayload("u", "t", null),
                        null, null, null, null),
                new AudioAttachment(new MediaPayload("u", "t")),
                new FileAttachment(new MediaPayload("u", "t"), "f", 0),
                new StickerAttachment(new StickerAttachment.StickerPayload("u", "c"), 1, 1),
                new ContactAttachment(new ContactAttachment.ContactPayload(null, null)),
                new InlineKeyboardAttachment(
                        new InlineKeyboardAttachment.KeyboardPayload(java.util.List.of())),
                new ShareAttachment(null, null, null, null),
                new LocationAttachment(0, 0),
                new UnknownAttachment("x", "{}")
        };
        for (Attachment att : all) {
            String desc = switch (att) {
                case PhotoAttachment p -> "photo:" + p.type();
                case VideoAttachment v -> "video:" + v.type();
                case AudioAttachment a -> "audio:" + a.type();
                case FileAttachment f -> "file:" + f.type();
                case StickerAttachment s -> "sticker:" + s.type();
                case ContactAttachment c -> "contact:" + c.type();
                case InlineKeyboardAttachment k -> "keyboard:" + k.type();
                case ShareAttachment sh -> "share:" + sh.type();
                case LocationAttachment l -> "location:" + l.type();
                case UnknownAttachment u -> "unknown:" + u.type();
            };
            assertThat(desc).isNotBlank();
        }
    }
}
