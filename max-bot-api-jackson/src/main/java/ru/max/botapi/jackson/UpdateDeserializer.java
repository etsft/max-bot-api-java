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
 */
final class UpdateDeserializer extends StdDeserializer<Update> {

    private static final long serialVersionUID = 1L;

    UpdateDeserializer() {
        super(Update.class);
    }

    @Override
    public Update deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        String updateType = node.has("update_type") ? node.get("update_type").asText() : "unknown";
        return switch (updateType) {
            case "message_created" -> ctxt.readTreeAsValue(node, MessageCreatedUpdate.class);
            case "message_callback" -> ctxt.readTreeAsValue(node, MessageCallbackUpdate.class);
            case "message_edited" -> ctxt.readTreeAsValue(node, MessageEditedUpdate.class);
            case "message_removed" -> ctxt.readTreeAsValue(node, MessageRemovedUpdate.class);
            case "bot_added" -> ctxt.readTreeAsValue(node, BotAddedUpdate.class);
            case "bot_removed" -> ctxt.readTreeAsValue(node, BotRemovedUpdate.class);
            case "user_added" -> ctxt.readTreeAsValue(node, UserAddedUpdate.class);
            case "user_removed" -> ctxt.readTreeAsValue(node, UserRemovedUpdate.class);
            case "bot_started" -> ctxt.readTreeAsValue(node, BotStartedUpdate.class);
            case "bot_stopped" -> ctxt.readTreeAsValue(node, BotStoppedUpdate.class);
            case "chat_title_changed" -> ctxt.readTreeAsValue(node, ChatTitleChangedUpdate.class);
            case "message_construction_request" ->
                    ctxt.readTreeAsValue(node, MessageConstructionRequestUpdate.class);
            case "message_constructed" -> ctxt.readTreeAsValue(node, MessageConstructedUpdate.class);
            case "message_chat_created" -> ctxt.readTreeAsValue(node, MessageChatCreatedUpdate.class);
            default -> {
                long timestamp = node.has("timestamp") ? node.get("timestamp").asLong() : 0L;
                yield new UnknownUpdate(updateType, timestamp, node.toString());
            }
        };
    }
}
