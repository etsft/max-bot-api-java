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
 * A message in MAX Messenger.
 *
 * @param sender      the sender (null for anonymous channel posts)
 * @param recipient   the recipient chat/user
 * @param timestamp   message timestamp (epoch millis)
 * @param link        link to a forwarded/replied message
 * @param body        the message content
 * @param stat        view statistics (null for non-channel messages)
 * @param url         public URL of the message (null for private chats)
 * @param constructor the bot that constructed this message (message construction flow)
 */
public record Message(
        @Nullable User sender,
        MessageRecipient recipient,
        long timestamp,
        @Nullable LinkedMessage link,
        MessageBody body,
        @Nullable MessageStat stat,
        @Nullable String url,
        @Nullable User constructor
) {

    /**
     * Creates a Message.
     *
     * @param recipient must not be {@code null}
     * @param body      must not be {@code null}
     */
    public Message {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(body, "body must not be null");
    }
}
