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

import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import ru.max.botapi.model.Attachment;
import ru.max.botapi.model.AttachmentRequest;
import ru.max.botapi.model.AudioAttachment;
import ru.max.botapi.model.AudioAttachmentRequest;
import ru.max.botapi.model.BotAddedUpdate;
import ru.max.botapi.model.BotRemovedUpdate;
import ru.max.botapi.model.BotStartedUpdate;
import ru.max.botapi.model.BotStoppedUpdate;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.CallbackButton;
import ru.max.botapi.model.ChatButton;
import ru.max.botapi.model.ChatTitleChangedUpdate;
import ru.max.botapi.model.ClipboardButton;
import ru.max.botapi.model.CommentCreatedUpdate;
import ru.max.botapi.model.CommentEditedUpdate;
import ru.max.botapi.model.CommentRemovedUpdate;
import ru.max.botapi.model.ContactAttachment;
import ru.max.botapi.model.ContactAttachmentRequest;
import ru.max.botapi.model.DialogClearedUpdate;
import ru.max.botapi.model.DialogMutedUpdate;
import ru.max.botapi.model.DialogRemovedUpdate;
import ru.max.botapi.model.DialogUnmutedUpdate;
import ru.max.botapi.model.FileAttachment;
import ru.max.botapi.model.FileAttachmentRequest;
import ru.max.botapi.model.ImageAttachmentRequest;
import ru.max.botapi.model.InlineKeyboardAttachment;
import ru.max.botapi.model.InlineKeyboardAttachmentRequest;
import ru.max.botapi.model.LinkButton;
import ru.max.botapi.model.LocationAttachment;
import ru.max.botapi.model.LocationAttachmentRequest;
import ru.max.botapi.model.MessageButton;
import ru.max.botapi.model.MessageCallbackUpdate;
import ru.max.botapi.model.MessageChatCreatedUpdate;
import ru.max.botapi.model.MessageConstructedUpdate;
import ru.max.botapi.model.MessageConstructionRequestUpdate;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.MessageEditedUpdate;
import ru.max.botapi.model.MessageRemovedUpdate;
import ru.max.botapi.model.OpenAppButton;
import ru.max.botapi.model.PhotoAttachment;
import ru.max.botapi.model.RequestContactButton;
import ru.max.botapi.model.RequestGeoLocationButton;
import ru.max.botapi.model.ShareAttachment;
import ru.max.botapi.model.ShareAttachmentRequest;
import ru.max.botapi.model.StickerAttachment;
import ru.max.botapi.model.StickerAttachmentRequest;
import ru.max.botapi.model.UnknownAttachment;
import ru.max.botapi.model.UnknownAttachmentRequest;
import ru.max.botapi.model.UnknownButton;
import ru.max.botapi.model.UnknownUpdate;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UserAddedUpdate;
import ru.max.botapi.model.UserRemovedUpdate;
import ru.max.botapi.model.VideoAttachment;
import ru.max.botapi.model.VideoAttachmentRequest;

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

    @SuppressWarnings("deprecation") // still dispatches the legacy types the API may send
    private static final Map<String, Class<? extends Update>> UPDATE_TYPES = Map.ofEntries(
            Map.entry("message_created", MessageCreatedUpdate.class),
            Map.entry("message_callback", MessageCallbackUpdate.class),
            Map.entry("message_edited", MessageEditedUpdate.class),
            Map.entry("message_removed", MessageRemovedUpdate.class),
            Map.entry("bot_added", BotAddedUpdate.class),
            Map.entry("bot_removed", BotRemovedUpdate.class),
            Map.entry("user_added", UserAddedUpdate.class),
            Map.entry("user_removed", UserRemovedUpdate.class),
            Map.entry("bot_started", BotStartedUpdate.class),
            Map.entry("bot_stopped", BotStoppedUpdate.class),
            Map.entry("chat_title_changed", ChatTitleChangedUpdate.class),
            Map.entry("message_construction_request", MessageConstructionRequestUpdate.class),
            Map.entry("message_constructed", MessageConstructedUpdate.class),
            Map.entry("message_chat_created", MessageChatCreatedUpdate.class),
            Map.entry("dialog_cleared", DialogClearedUpdate.class),
            Map.entry("dialog_muted", DialogMutedUpdate.class),
            Map.entry("dialog_unmuted", DialogUnmutedUpdate.class),
            Map.entry("dialog_removed", DialogRemovedUpdate.class),
            Map.entry("comment_created", CommentCreatedUpdate.class),
            Map.entry("comment_edited", CommentEditedUpdate.class),
            Map.entry("comment_removed", CommentRemovedUpdate.class));

    private static final Map<String, Class<? extends Attachment>> ATTACHMENT_TYPES = Map.of(
            "image", PhotoAttachment.class,
            "video", VideoAttachment.class,
            "audio", AudioAttachment.class,
            "file", FileAttachment.class,
            "sticker", StickerAttachment.class,
            "contact", ContactAttachment.class,
            "inline_keyboard", InlineKeyboardAttachment.class,
            "share", ShareAttachment.class,
            "location", LocationAttachment.class);

    private static final Map<String, Class<? extends AttachmentRequest>> ATTACHMENT_REQUEST_TYPES = Map.of(
            "image", ImageAttachmentRequest.class,
            "video", VideoAttachmentRequest.class,
            "audio", AudioAttachmentRequest.class,
            "file", FileAttachmentRequest.class,
            "sticker", StickerAttachmentRequest.class,
            "contact", ContactAttachmentRequest.class,
            "inline_keyboard", InlineKeyboardAttachmentRequest.class,
            "share", ShareAttachmentRequest.class,
            "location", LocationAttachmentRequest.class);

    @SuppressWarnings("deprecation") // still dispatches the legacy types the API may send
    private static final Map<String, Class<? extends Button>> BUTTON_TYPES = Map.of(
            "callback", CallbackButton.class,
            "link", LinkButton.class,
            "request_contact", RequestContactButton.class,
            "request_geo_location", RequestGeoLocationButton.class,
            "chat", ChatButton.class,
            "open_app", OpenAppButton.class,
            "message", MessageButton.class,
            "clipboard", ClipboardButton.class);

    private MaxTypeAdapters() {
    }

    /**
     * Registers all hierarchy adapters on {@code builder}.
     *
     * @param builder the builder to register the adapters on
     */
    static void register(GsonBuilder builder) {
        builder.registerTypeAdapterFactory(new SealedTypeAdapterFactory<>(Update.class, "update_type",
                UPDATE_TYPES, MaxTypeAdapters::unknownUpdate, true, true, null));
        builder.registerTypeAdapterFactory(new SealedTypeAdapterFactory<>(Attachment.class, "type",
                ATTACHMENT_TYPES, (type, node) -> new UnknownAttachment(type, node.toString()),
                true, false, Attachment::type));
        builder.registerTypeAdapterFactory(new SealedTypeAdapterFactory<>(AttachmentRequest.class, "type",
                ATTACHMENT_REQUEST_TYPES, (type, node) -> new UnknownAttachmentRequest(type, node.toString()),
                false, false, AttachmentRequest::type));
        builder.registerTypeAdapterFactory(new SealedTypeAdapterFactory<>(Button.class, "type",
                BUTTON_TYPES, MaxTypeAdapters::unknownButton, true, false, Button::type));
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
