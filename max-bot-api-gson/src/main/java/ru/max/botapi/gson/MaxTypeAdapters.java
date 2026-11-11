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

package ru.max.botapi.gson;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import ru.max.botapi.model.Attachment;
import ru.max.botapi.model.AttachmentRequest;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.ModelTypes;
import ru.max.botapi.model.UnknownAttachment;
import ru.max.botapi.model.UnknownAttachmentRequest;
import ru.max.botapi.model.UnknownButton;
import ru.max.botapi.model.UnknownUpdate;
import ru.max.botapi.model.Update;

/**
 * Registers the adapters for the sealed model hierarchies and {@code UpdateList}.
 *
 * <ul>
 *   <li>{@link Update} — discriminated by {@code update_type}; malformed known types degrade
 *       to {@link UnknownUpdate}. Written as plain records, without the discriminator.</li>
 *   <li>{@link Attachment} — discriminated by {@code type}; malformed known types degrade
 *       to {@link UnknownAttachment}</li>
 *   <li>{@link AttachmentRequest} — discriminated by {@code type}; malformed known types fail</li>
 *   <li>{@link Button} — discriminated by {@code type}; malformed known types degrade
 *       to {@link UnknownButton}</li>
 *   <li>{@code UpdateList} — see {@link UpdateListAdapterFactory}</li>
 * </ul>
 *
 * <p>Note that an {@link UnknownUpdate} produced for a malformed <em>known</em> type returns
 * that known discriminator from {@link UnknownUpdate#updateType()}, so code dispatching on the
 * string must not blindly cast; pattern matching over the sealed hierarchy is safe.</p>
 */
final class MaxTypeAdapters {

    private MaxTypeAdapters() {
    }

    /**
     * Registers all hierarchy adapters on {@code builder}.
     *
     * @param builder the builder to register the adapters on
     */
    static void register(GsonBuilder builder) {
        builder.registerTypeAdapterFactory(new SealedTypeAdapterFactory<>(Update.class, "update_type",
                ModelTypes.updates(), MaxTypeAdapters::unknownUpdate, true, true, Update::updateType));
        builder.registerTypeAdapterFactory(new SealedTypeAdapterFactory<>(Attachment.class, "type",
                ModelTypes.attachments(), (type, node) -> new UnknownAttachment(type, node.toString()),
                true, false, Attachment::type));
        builder.registerTypeAdapterFactory(new SealedTypeAdapterFactory<>(AttachmentRequest.class, "type",
                ModelTypes.attachmentRequests(),
                (type, node) -> new UnknownAttachmentRequest(type, node.toString()),
                false, false, AttachmentRequest::type));
        builder.registerTypeAdapterFactory(new SealedTypeAdapterFactory<>(Button.class, "type",
                ModelTypes.buttons(), MaxTypeAdapters::unknownButton, true, false, Button::type));
        builder.registerTypeAdapterFactory(new UpdateListAdapterFactory());
    }

    private static UnknownUpdate unknownUpdate(String updateType, JsonElement node) {
        return new UnknownUpdate(updateType, longProperty(node, "timestamp"), node.toString());
    }

    private static UnknownButton unknownButton(String type, JsonElement node) {
        JsonPrimitive text = primitiveProperty(node, "text");
        return new UnknownButton(type, text == null ? "" : text.getAsString(), node.toString());
    }

    /**
     * Reads a property as a {@code long} the way Jackson's {@code asLong(0L)} does:
     * a number is truncated toward zero, integer text is parsed, and anything else —
     * including a number outside the {@code long} range — is {@code 0}.
     */
    private static long longProperty(JsonElement node, String name) {
        JsonPrimitive value = primitiveProperty(node, name);
        if (value == null || value.isBoolean()) {
            return 0L;
        }
        String text = value.getAsString().strip();
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            if (!value.isNumber()) {
                return 0L;
            }
            double number = Double.parseDouble(text);
            return number >= Long.MIN_VALUE && number <= Long.MAX_VALUE ? (long) number : 0L;
        }
    }

    private static JsonPrimitive primitiveProperty(JsonElement node, String name) {
        if (!node.isJsonObject()) {
            return null;
        }
        JsonElement value = node.getAsJsonObject().get(name);
        return value != null && value.isJsonPrimitive() ? value.getAsJsonPrimitive() : null;
    }
}
