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

package ru.max.botapi.model;

import java.util.Map;

/**
 * The discriminator values of the sealed model hierarchies and the record each one maps to.
 *
 * <p>This is the single source of truth for the serialization adapters: a type the MAX API adds
 * is registered here once and both {@code max-bot-api-jackson} and {@code max-bot-api-gson} pick
 * it up. A discriminator missing from these tables reads as the hierarchy's {@code Unknown*}
 * fallback type. Custom {@link ru.max.botapi.core.MaxSerializer} implementations can use the
 * tables the same way.</p>
 *
 * <p>All returned maps are immutable.</p>
 */
public final class ModelTypes {

    @SuppressWarnings("deprecation") // still dispatches the legacy types the API may send
    private static final Map<String, Class<? extends Update>> UPDATES = Map.ofEntries(
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
            Map.entry("comment_removed", CommentRemovedUpdate.class),
            Map.entry("bot_admin_permissions_changed", BotAdminPermissionsChangedUpdate.class));

    private static final Map<String, Class<? extends Attachment>> ATTACHMENTS = Map.of(
            "image", PhotoAttachment.class,
            "video", VideoAttachment.class,
            "audio", AudioAttachment.class,
            "file", FileAttachment.class,
            "sticker", StickerAttachment.class,
            "contact", ContactAttachment.class,
            "inline_keyboard", InlineKeyboardAttachment.class,
            "share", ShareAttachment.class,
            "location", LocationAttachment.class);

    private static final Map<String, Class<? extends AttachmentRequest>> ATTACHMENT_REQUESTS = Map.of(
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
    private static final Map<String, Class<? extends Button>> BUTTONS = Map.of(
            "callback", CallbackButton.class,
            "link", LinkButton.class,
            "request_contact", RequestContactButton.class,
            "request_geo_location", RequestGeoLocationButton.class,
            "chat", ChatButton.class,
            "open_app", OpenAppButton.class,
            "message", MessageButton.class,
            "clipboard", ClipboardButton.class);

    private ModelTypes() {
    }

    /**
     * Returns the {@link Update} types keyed by their {@code update_type} value.
     *
     * @return the immutable discriminator table, without {@link UnknownUpdate}
     */
    public static Map<String, Class<? extends Update>> updates() {
        return UPDATES;
    }

    /**
     * Returns the {@link Attachment} types keyed by their {@code type} value.
     *
     * @return the immutable discriminator table, without {@link UnknownAttachment}
     */
    public static Map<String, Class<? extends Attachment>> attachments() {
        return ATTACHMENTS;
    }

    /**
     * Returns the {@link AttachmentRequest} types keyed by their {@code type} value.
     *
     * @return the immutable discriminator table, without {@link UnknownAttachmentRequest}
     */
    public static Map<String, Class<? extends AttachmentRequest>> attachmentRequests() {
        return ATTACHMENT_REQUESTS;
    }

    /**
     * Returns the {@link Button} types keyed by their {@code type} value.
     *
     * @return the immutable discriminator table, without {@link UnknownButton}
     */
    public static Map<String, Class<? extends Button>> buttons() {
        return BUTTONS;
    }
}
