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

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import ru.max.botapi.model.Button;
import ru.max.botapi.model.ModelTypes;
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

    ButtonDeserializer() {
        super(Button.class);
    }

    @Override
    public Button deserialize(JsonParser p, DeserializationContext ctxt) {
        JsonNode node = ctxt.readTree(p);
        String type = LenientReads.discriminator(node, "type");
        Class<? extends Button> buttonType = ModelTypes.buttons().get(type);
        return buttonType == null ? unknown(node, type) : parseOrUnknown(ctxt, node, buttonType, type);
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
        String text = node.has("text") ? node.get("text").asString("") : "";
        return new UnknownButton(type, text, node.toString());
    }
}
