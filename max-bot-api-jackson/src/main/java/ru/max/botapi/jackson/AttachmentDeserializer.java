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

import ru.max.botapi.model.Attachment;
import ru.max.botapi.model.AudioAttachment;
import ru.max.botapi.model.ContactAttachment;
import ru.max.botapi.model.FileAttachment;
import ru.max.botapi.model.InlineKeyboardAttachment;
import ru.max.botapi.model.LocationAttachment;
import ru.max.botapi.model.PhotoAttachment;
import ru.max.botapi.model.ShareAttachment;
import ru.max.botapi.model.StickerAttachment;
import ru.max.botapi.model.UnknownAttachment;
import ru.max.botapi.model.VideoAttachment;

/**
 * Custom Jackson deserializer for the {@link Attachment} sealed hierarchy.
 *
 * <p>Uses the {@code type} JSON field as a discriminator to resolve the concrete
 * attachment type. Unknown types produce {@link UnknownAttachment}.</p>
 */
final class AttachmentDeserializer extends StdDeserializer<Attachment> {

    private static final long serialVersionUID = 1L;

    AttachmentDeserializer() {
        super(Attachment.class);
    }

    @Override
    public Attachment deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        String type = node.has("type") ? node.get("type").asText() : "unknown";
        return switch (type) {
            case "image" -> ctxt.readTreeAsValue(node, PhotoAttachment.class);
            case "video" -> ctxt.readTreeAsValue(node, VideoAttachment.class);
            case "audio" -> ctxt.readTreeAsValue(node, AudioAttachment.class);
            case "file" -> ctxt.readTreeAsValue(node, FileAttachment.class);
            case "sticker" -> ctxt.readTreeAsValue(node, StickerAttachment.class);
            case "contact" -> ctxt.readTreeAsValue(node, ContactAttachment.class);
            case "inline_keyboard" -> ctxt.readTreeAsValue(node, InlineKeyboardAttachment.class);
            case "share" -> ctxt.readTreeAsValue(node, ShareAttachment.class);
            case "location" -> ctxt.readTreeAsValue(node, LocationAttachment.class);
            default -> new UnknownAttachment(type, node.toString());
        };
    }
}
