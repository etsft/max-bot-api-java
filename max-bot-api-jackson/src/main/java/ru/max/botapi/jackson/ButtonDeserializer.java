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

import ru.max.botapi.model.Button;
import ru.max.botapi.model.CallbackButton;
import ru.max.botapi.model.ChatButton;
import ru.max.botapi.model.ClipboardButton;
import ru.max.botapi.model.LinkButton;
import ru.max.botapi.model.MessageButton;
import ru.max.botapi.model.OpenAppButton;
import ru.max.botapi.model.RequestContactButton;
import ru.max.botapi.model.RequestGeoLocationButton;
import ru.max.botapi.model.UnknownButton;

/**
 * Custom Jackson deserializer for the {@link Button} sealed hierarchy.
 *
 * <p>Uses the {@code type} JSON field as a discriminator to resolve the concrete
 * button type. Unknown types produce {@link UnknownButton}.</p>
 *
 * <p>A <em>known</em> type that cannot be constructed — a required field missing,
 * for example — also produces {@link UnknownButton} with the original discriminator
 * and the raw JSON, so that one odd button does not fail the whole keyboard.</p>
 */
final class ButtonDeserializer extends StdDeserializer<Button> {

    private static final long serialVersionUID = 1L;

    ButtonDeserializer() {
        super(Button.class);
    }

    @Override
    @SuppressWarnings("deprecation") // still dispatches the legacy types the API may send
    public Button deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        String type = node.has("type") ? node.get("type").asText() : "unknown";
        return switch (type) {
            case "callback" -> parseOrUnknown(ctxt, node, CallbackButton.class, type);
            case "link" -> parseOrUnknown(ctxt, node, LinkButton.class, type);
            case "request_contact" -> parseOrUnknown(ctxt, node, RequestContactButton.class, type);
            case "request_geo_location" -> parseOrUnknown(ctxt, node, RequestGeoLocationButton.class, type);
            case "chat" -> parseOrUnknown(ctxt, node, ChatButton.class, type);
            case "open_app" -> parseOrUnknown(ctxt, node, OpenAppButton.class, type);
            case "message" -> parseOrUnknown(ctxt, node, MessageButton.class, type);
            case "clipboard" -> parseOrUnknown(ctxt, node, ClipboardButton.class, type);
            default -> unknown(node, type);
        };
    }

    /**
     * Reads {@code node} as {@code buttonType}, degrading to {@link UnknownButton} on failure.
     *
     * @param ctxt       the active deserialization context
     * @param node       the JSON node of the button
     * @param buttonType the concrete button type resolved from the discriminator
     * @param type       the {@code type} discriminator value
     * @return the deserialized button, or an {@link UnknownButton} fallback
     */
    private static Button parseOrUnknown(DeserializationContext ctxt, JsonNode node,
            Class<? extends Button> buttonType, String type) {
        return LenientReads.readOrFallback(ctxt, node, buttonType, type, () -> unknown(node, type));
    }

    /**
     * Builds the {@link UnknownButton} representation of a node.
     *
     * @param node the JSON node of the button
     * @param type the {@code type} discriminator value
     * @return the fallback button
     */
    private static UnknownButton unknown(JsonNode node, String type) {
        String text = node.has("text") ? node.get("text").asText() : "";
        return new UnknownButton(type, text, node.toString());
    }
}
