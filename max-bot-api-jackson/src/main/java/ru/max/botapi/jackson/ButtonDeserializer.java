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
 */
final class ButtonDeserializer extends StdDeserializer<Button> {

    private static final long serialVersionUID = 1L;

    ButtonDeserializer() {
        super(Button.class);
    }

    @Override
    public Button deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        String type = node.has("type") ? node.get("type").asText() : "unknown";
        return switch (type) {
            case "callback" -> ctxt.readTreeAsValue(node, CallbackButton.class);
            case "link" -> ctxt.readTreeAsValue(node, LinkButton.class);
            case "request_contact" -> ctxt.readTreeAsValue(node, RequestContactButton.class);
            case "request_geo_location" -> ctxt.readTreeAsValue(node, RequestGeoLocationButton.class);
            case "chat" -> ctxt.readTreeAsValue(node, ChatButton.class);
            case "open_app" -> ctxt.readTreeAsValue(node, OpenAppButton.class);
            case "message" -> ctxt.readTreeAsValue(node, MessageButton.class);
            default -> {
                String text = node.has("text") ? node.get("text").asText() : "";
                yield new UnknownButton(type, text, node.toString());
            }
        };
    }
}
