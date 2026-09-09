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
 *
 * <p>A <em>known</em> type that cannot be constructed — a required payload field
 * missing, for example — also produces {@link UnknownAttachment} with the original
 * discriminator and the raw JSON, so that one odd attachment does not fail the whole
 * message it belongs to.</p>
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
            case "image" -> parseOrUnknown(ctxt, node, PhotoAttachment.class, type);
            case "video" -> parseOrUnknown(ctxt, node, VideoAttachment.class, type);
            case "audio" -> parseOrUnknown(ctxt, node, AudioAttachment.class, type);
            case "file" -> parseOrUnknown(ctxt, node, FileAttachment.class, type);
            case "sticker" -> parseOrUnknown(ctxt, node, StickerAttachment.class, type);
            case "contact" -> parseOrUnknown(ctxt, node, ContactAttachment.class, type);
            case "inline_keyboard" -> parseOrUnknown(ctxt, node, InlineKeyboardAttachment.class, type);
            case "share" -> parseOrUnknown(ctxt, node, ShareAttachment.class, type);
            case "location" -> parseOrUnknown(ctxt, node, LocationAttachment.class, type);
            default -> new UnknownAttachment(type, node.toString());
        };
    }

    /**
     * Reads {@code node} as {@code attachmentType}, degrading to {@link UnknownAttachment} on failure.
     *
     * @param ctxt           the active deserialization context
     * @param node           the JSON node of the attachment
     * @param attachmentType the concrete attachment type resolved from the discriminator
     * @param type           the {@code type} discriminator value
     * @return the deserialized attachment, or an {@link UnknownAttachment} fallback
     */
    private static Attachment parseOrUnknown(DeserializationContext ctxt, JsonNode node,
            Class<? extends Attachment> attachmentType, String type) {
        return LenientReads.readOrFallback(ctxt, node, attachmentType, type,
                () -> new UnknownAttachment(type, node.toString()));
    }
}
