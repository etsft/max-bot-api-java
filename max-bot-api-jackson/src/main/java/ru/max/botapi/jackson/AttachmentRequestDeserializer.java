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

package ru.max.botapi.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import ru.max.botapi.model.AttachmentRequest;
import ru.max.botapi.model.AudioAttachmentRequest;
import ru.max.botapi.model.ContactAttachmentRequest;
import ru.max.botapi.model.FileAttachmentRequest;
import ru.max.botapi.model.ImageAttachmentRequest;
import ru.max.botapi.model.InlineKeyboardAttachmentRequest;
import ru.max.botapi.model.LocationAttachmentRequest;
import ru.max.botapi.model.ShareAttachmentRequest;
import ru.max.botapi.model.StickerAttachmentRequest;
import ru.max.botapi.model.UnknownAttachmentRequest;
import ru.max.botapi.model.VideoAttachmentRequest;

/**
 * Custom Jackson deserializer for the {@link AttachmentRequest} sealed hierarchy.
 *
 * <p>Uses the {@code type} JSON field as a discriminator to resolve the concrete
 * attachment request type. Unknown types produce {@link UnknownAttachmentRequest}.</p>
 */
final class AttachmentRequestDeserializer extends StdDeserializer<AttachmentRequest> {

    private static final long serialVersionUID = 1L;

    AttachmentRequestDeserializer() {
        super(AttachmentRequest.class);
    }

    @Override
    public AttachmentRequest deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        String type = node.has("type") ? node.get("type").asText() : "unknown";
        return switch (type) {
            case "image" -> ctxt.readTreeAsValue(node, ImageAttachmentRequest.class);
            case "video" -> ctxt.readTreeAsValue(node, VideoAttachmentRequest.class);
            case "audio" -> ctxt.readTreeAsValue(node, AudioAttachmentRequest.class);
            case "file" -> ctxt.readTreeAsValue(node, FileAttachmentRequest.class);
            case "sticker" -> ctxt.readTreeAsValue(node, StickerAttachmentRequest.class);
            case "contact" -> ctxt.readTreeAsValue(node, ContactAttachmentRequest.class);
            case "inline_keyboard" -> ctxt.readTreeAsValue(node, InlineKeyboardAttachmentRequest.class);
            case "share" -> ctxt.readTreeAsValue(node, ShareAttachmentRequest.class);
            case "location" -> ctxt.readTreeAsValue(node, LocationAttachmentRequest.class);
            default -> new UnknownAttachmentRequest(type, node.toString());
        };
    }
}
