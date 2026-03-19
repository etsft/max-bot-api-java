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

import java.util.List;
import java.util.Objects;

/**
 * Inline keyboard attachment containing rows of buttons.
 *
 * @param payload keyboard payload with button rows
 */
public record InlineKeyboardAttachment(KeyboardPayload payload) implements Attachment {

    /** {@inheritDoc} */
    @Override
    public String type() {
        return "inline_keyboard";
    }

    /**
     * Creates an InlineKeyboardAttachment.
     *
     * @param payload must not be {@code null}
     */
    public InlineKeyboardAttachment {
        Objects.requireNonNull(payload, "payload must not be null");
    }

    /**
     * Payload for an inline keyboard attachment.
     *
     * @param buttons rows of buttons (each inner list is a row)
     */
    public record KeyboardPayload(List<List<Button>> buttons) {

        /**
         * Creates a KeyboardPayload.
         *
         * @param buttons must not be {@code null}
         */
        public KeyboardPayload {
            Objects.requireNonNull(buttons, "buttons must not be null");
            buttons = buttons.stream()
                    .map(List::copyOf)
                    .toList();
        }
    }
}
