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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Inline keyboard attachment request for outgoing messages.
 *
 * <p>Build one row at a time:</p>
 * <pre>{@code
 * InlineKeyboardAttachmentRequest keyboard = InlineKeyboardAttachmentRequest.builder()
 *         .row(CallbackButton.of("Yes", "yes"), CallbackButton.of("No", "no"))
 *         .row(new LinkButton("Docs", "https://dev.max.ru"))
 *         .build();
 * }</pre>
 *
 * @param payload keyboard payload with button rows
 */
public record InlineKeyboardAttachmentRequest(
        InlineKeyboardAttachment.KeyboardPayload payload
) implements AttachmentRequest {

    /** {@inheritDoc} */
    @Override
    public String type() {
        return "inline_keyboard";
    }

    /**
     * Creates an InlineKeyboardAttachmentRequest.
     *
     * @param payload must not be {@code null}
     */
    public InlineKeyboardAttachmentRequest {
        Objects.requireNonNull(payload, "payload must not be null");
    }

    /**
     * Returns a builder that collects the keyboard row by row.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link InlineKeyboardAttachmentRequest}.
     */
    public static final class Builder {

        private final List<List<Button>> rows = new ArrayList<>();

        private Builder() {
        }

        /**
         * Adds a row of buttons below the rows added so far.
         *
         * @param buttons the buttons, left to right; at least one, none {@code null}
         * @return this builder
         * @throws IllegalArgumentException if no buttons are given
         */
        public Builder row(Button... buttons) {
            if (buttons.length == 0) {
                throw new IllegalArgumentException("a keyboard row needs at least one button");
            }
            rows.add(List.of(buttons));
            return this;
        }

        /**
         * Builds the keyboard.
         *
         * @return the keyboard attachment request
         * @throws IllegalStateException if no row was added
         */
        public InlineKeyboardAttachmentRequest build() {
            if (rows.isEmpty()) {
                throw new IllegalStateException("a keyboard needs at least one row");
            }
            return new InlineKeyboardAttachmentRequest(new InlineKeyboardAttachment.KeyboardPayload(rows));
        }
    }
}
