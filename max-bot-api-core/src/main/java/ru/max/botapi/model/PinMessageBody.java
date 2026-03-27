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

import java.util.Objects;

/**
 * Request body for {@code PUT /chats/{chatId}/pin}.
 *
 * @param messageId ID of the message to pin
 * @param notifyRecipients whether to notify chat participants about the pin
 */
public record PinMessageBody(
        String messageId,
        // TODO: Jackson adapter must map this field to "notify" in JSON
        //       via @JsonProperty("notify") or custom naming strategy
        @Nullable Boolean notifyRecipients
) {

    /**
     * Creates a PinMessageBody.
     *
     * @param messageId must not be {@code null}
     */
    public PinMessageBody {
        Objects.requireNonNull(messageId, "messageId must not be null");
    }
}
