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
 * A linked (forwarded or replied-to) message reference.
 *
 * @param type    type of link (forward or reply)
 * @param sender  the original sender (null if unknown/deleted)
 * @param chatId  the original chat ID
 * @param message the body of the linked message
 */
public record LinkedMessage(
        MessageLinkType type,
        @Nullable User sender,
        @Nullable Long chatId,
        MessageBody message
) {

    /**
     * Creates a LinkedMessage.
     *
     * @param type    must not be {@code null}
     * @param message must not be {@code null}
     */
    public LinkedMessage {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
