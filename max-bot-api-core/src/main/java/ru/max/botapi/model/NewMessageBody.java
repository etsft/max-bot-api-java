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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Request body for sending ({@code POST /messages}) or editing ({@code PUT /messages}) a message.
 *
 * <p>Start from {@link #text(String)} and add the rest with the {@code with*} methods:</p>
 * <pre>{@code
 * NewMessageBody body = NewMessageBody.text("<b>Pick one</b>")
 *         .withFormat(TextFormat.HTML)
 *         .withAttachments(InlineKeyboardAttachmentRequest.builder()
 *                 .row(CallbackButton.of("Yes", "yes"), CallbackButton.of("No", "no"))
 *                 .build());
 * }</pre>
 *
 * @param text        text content of the message
 * @param attachments list of attachment requests
 * @param link        link to another message (forward/reply)
 * @param notifyRecipients whether to notify recipients
 * @param format      text format (markdown or HTML)
 */
public record NewMessageBody(
        @Nullable String text,
        @Nullable List<AttachmentRequest> attachments,
        @Nullable NewMessageLink link,
        @Nullable Boolean notifyRecipients,
        @Nullable TextFormat format
) {

    /**
     * Compact constructor for defensive copies.
     */
    public NewMessageBody {
        attachments = attachments == null ? null : List.copyOf(attachments);
    }

    /**
     * Creates a body with only text; the other fields are {@code null}.
     *
     * @param text the message text; must not be {@code null}
     * @return the new body
     */
    public static NewMessageBody text(String text) {
        return new NewMessageBody(Objects.requireNonNull(text, "text must not be null"),
                null, null, null, null);
    }

    /**
     * Creates a body with text in the given format; the other fields are {@code null}.
     *
     * @param text   the message text; must not be {@code null}
     * @param format the text format; must not be {@code null}
     * @return the new body
     */
    public static NewMessageBody text(String text, TextFormat format) {
        return text(text).withFormat(Objects.requireNonNull(format, "format must not be null"));
    }

    /**
     * Creates a body with only attachments, such as a photo without a caption.
     *
     * @param attachments the attachments; must not be {@code null} or contain {@code null}
     * @return the new body
     */
    public static NewMessageBody attachments(AttachmentRequest... attachments) {
        return new NewMessageBody(null, List.of(attachments), null, null, null);
    }

    /**
     * Returns a copy with the given attachments, replacing any present.
     *
     * @param attachments the attachments; must not be {@code null} or contain {@code null}
     * @return the new body
     */
    public NewMessageBody withAttachments(AttachmentRequest... attachments) {
        return new NewMessageBody(text, Arrays.asList(attachments), link, notifyRecipients, format);
    }

    /**
     * Returns a copy with the given link, making the message a reply or a forward.
     *
     * @param link the link, or {@code null} for none
     * @return the new body
     */
    public NewMessageBody withLink(@Nullable NewMessageLink link) {
        return new NewMessageBody(text, attachments, link, notifyRecipients, format);
    }

    /**
     * Returns a copy with the given notification setting.
     *
     * @param notify {@code false} to send silently
     * @return the new body
     */
    public NewMessageBody withNotify(boolean notify) {
        return new NewMessageBody(text, attachments, link, notify, format);
    }

    /**
     * Returns a copy with the given text format.
     *
     * @param format the format, or {@code null} for plain text
     * @return the new body
     */
    public NewMessageBody withFormat(@Nullable TextFormat format) {
        return new NewMessageBody(text, attachments, link, notifyRecipients, format);
    }
}
