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
 * Update: a chat was created from a message (via {@link ChatButton}).
 *
 * @param timestamp    event timestamp (epoch millis)
 * @param chat         the created chat
 * @param messageId    ID of the message that triggered chat creation
 * @param startPayload optional payload from the chat button
 */
public record MessageChatCreatedUpdate(
        long timestamp,
        Chat chat,
        String messageId,
        @Nullable String startPayload
) implements Update {

    /** {@inheritDoc} */
    @Override
    public String updateType() {
        return "message_chat_created";
    }

    /**
     * Creates a MessageChatCreatedUpdate.
     *
     * @param chat      must not be {@code null}
     * @param messageId must not be {@code null}
     */
    public MessageChatCreatedUpdate {
        Objects.requireNonNull(chat, "chat must not be null");
        Objects.requireNonNull(messageId, "messageId must not be null");
    }
}
