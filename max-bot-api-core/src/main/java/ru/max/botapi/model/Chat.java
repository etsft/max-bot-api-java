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
import java.util.Objects;

/**
 * Represents a chat (dialog, group chat, or channel) in MAX Messenger.
 *
 * @param chatId           unique chat identifier
 * @param type             type of the chat
 * @param status           current chat status
 * @param title            chat title (null for dialogs)
 * @param icon             chat icon image
 * @param lastEventTime    timestamp of the last event (epoch millis)
 * @param participantsCount number of participants
 * @param ownerId          owner user ID
 * @param participants     map of participant identifiers to user IDs
 * @param isPublic         whether the chat is public
 * @param link             public invite link
 * @param description      chat description
 * @param dialogWithUser   for dialogs, the other user
 * @param messagesCount    total messages count
 * @param chatMessageId    last message ID in the chat
 * @param pinnedMessage    currently pinned message
 */
public record Chat(
        long chatId,
        ChatType type,
        ChatStatus status,
        @Nullable String title,
        @Nullable Image icon,
        long lastEventTime,
        int participantsCount,
        @Nullable Long ownerId,
        @Nullable Map<String, Long> participants,
        boolean isPublic,
        @Nullable String link,
        @Nullable String description,
        @Nullable UserWithPhoto dialogWithUser,
        @Nullable Integer messagesCount,
        @Nullable String chatMessageId,
        @Nullable Message pinnedMessage
) {

    /**
     * Creates a Chat.
     *
     * @param type   must not be {@code null}
     * @param status must not be {@code null}
     */
    public Chat {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(status, "status must not be null");
        participants = participants == null ? null : Map.copyOf(participants);
    }
}
