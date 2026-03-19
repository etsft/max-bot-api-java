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
 * Identifies the recipient of a message.
 *
 * @param chatId   chat identifier (null when recipient is identified by other means)
 * @param chatType type of the chat
 */
public record MessageRecipient(
        @Nullable Long chatId,
        ChatType chatType
) {

    /**
     * Creates a MessageRecipient.
     *
     * @param chatType must not be {@code null}
     */
    public MessageRecipient {
        Objects.requireNonNull(chatType, "chatType must not be null");
    }
}
