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

import ru.max.botapi.model.BotAddedUpdate;
import ru.max.botapi.model.BotRemovedUpdate;
import ru.max.botapi.model.BotStartedUpdate;
import ru.max.botapi.model.BotStoppedUpdate;
import ru.max.botapi.model.ChatTitleChangedUpdate;
import ru.max.botapi.model.MessageCallbackUpdate;
import ru.max.botapi.model.MessageChatCreatedUpdate;
import ru.max.botapi.model.MessageConstructedUpdate;
import ru.max.botapi.model.MessageConstructionRequestUpdate;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.MessageEditedUpdate;
import ru.max.botapi.model.MessageRemovedUpdate;
import ru.max.botapi.model.UnknownUpdate;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UserAddedUpdate;
import ru.max.botapi.model.UserRemovedUpdate;

/**
 * Custom Jackson deserializer for the {@link Update} sealed hierarchy.
 *
 * <p>Uses the {@code update_type} JSON field as a discriminator to resolve
 * the concrete update type. Unknown types produce {@link UnknownUpdate}.</p>
 *
 * <p>A <em>known</em> type that cannot be constructed — for example a
 * {@code message_created} whose {@code message} field is absent — also produces
 * {@link UnknownUpdate}, carrying the original discriminator and the raw JSON.
 * This keeps a single malformed update from aborting the whole {@code UpdateList}
 * batch and stalling the long-polling marker. Note that in this case
 * {@link UnknownUpdate#updateType()} returns the known discriminator, so code that
 * dispatches on the {@code updateType()} string must not blindly cast to the
 * concrete update type; pattern matching over the sealed hierarchy is safe.</p>
 */
final class UpdateDeserializer extends StdDeserializer<Update> {

    private static final long serialVersionUID = 1L;

    UpdateDeserializer() {
        super(Update.class);
    }

    @Override
    public Update deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (!node.isObject()) {
            return ctxt.reportInputMismatch(Update.class,
                    "Expected a JSON object for an update, got %s", node.getNodeType());
        }
        String updateType = node.has("update_type") ? node.get("update_type").asText() : "unknown";
        return switch (updateType) {
            case "message_created" -> parseOrUnknown(ctxt, node, MessageCreatedUpdate.class, updateType);
            case "message_callback" -> parseOrUnknown(ctxt, node, MessageCallbackUpdate.class, updateType);
            case "message_edited" -> parseOrUnknown(ctxt, node, MessageEditedUpdate.class, updateType);
            case "message_removed" -> parseOrUnknown(ctxt, node, MessageRemovedUpdate.class, updateType);
            case "bot_added" -> parseOrUnknown(ctxt, node, BotAddedUpdate.class, updateType);
            case "bot_removed" -> parseOrUnknown(ctxt, node, BotRemovedUpdate.class, updateType);
            case "user_added" -> parseOrUnknown(ctxt, node, UserAddedUpdate.class, updateType);
            case "user_removed" -> parseOrUnknown(ctxt, node, UserRemovedUpdate.class, updateType);
            case "bot_started" -> parseOrUnknown(ctxt, node, BotStartedUpdate.class, updateType);
            case "bot_stopped" -> parseOrUnknown(ctxt, node, BotStoppedUpdate.class, updateType);
            case "chat_title_changed" -> parseOrUnknown(ctxt, node, ChatTitleChangedUpdate.class, updateType);
            case "message_construction_request" ->
                    parseOrUnknown(ctxt, node, MessageConstructionRequestUpdate.class, updateType);
            case "message_constructed" -> parseOrUnknown(ctxt, node, MessageConstructedUpdate.class, updateType);
            case "message_chat_created" -> parseOrUnknown(ctxt, node, MessageChatCreatedUpdate.class, updateType);
            default -> unknown(node, updateType);
        };
    }

    /**
     * Reads {@code node} as {@code type}, degrading to {@link UnknownUpdate} on failure.
     *
     * @param ctxt       the active deserialization context
     * @param node       the JSON node of the update
     * @param type       the concrete update type resolved from the discriminator
     * @param updateType the {@code update_type} discriminator value
     * @return the deserialized update, or an {@link UnknownUpdate} fallback
     */
    private static Update parseOrUnknown(DeserializationContext ctxt, JsonNode node,
            Class<? extends Update> type, String updateType) {
        return LenientReads.readOrFallback(ctxt, node, type, updateType, () -> unknown(node, updateType));
    }

    /**
     * Builds the {@link UnknownUpdate} representation of a node.
     *
     * @param node       the JSON node of the update
     * @param updateType the {@code update_type} discriminator value
     * @return the fallback update
     */
    private static UnknownUpdate unknown(JsonNode node, String updateType) {
        long timestamp = node.has("timestamp") ? node.get("timestamp").asLong() : 0L;
        return new UnknownUpdate(updateType, timestamp, node.toString());
    }
}
