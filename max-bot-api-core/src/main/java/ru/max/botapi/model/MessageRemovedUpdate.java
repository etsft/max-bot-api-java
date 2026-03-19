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
 * Update: a message was removed.
 *
 * @param timestamp event timestamp (epoch millis)
 * @param messageId ID of the removed message
 * @param chatId    chat where the message was removed
 * @param userId    user who removed the message
 */
public record MessageRemovedUpdate(
        long timestamp,
        String messageId,
        long chatId,
        long userId
) implements Update {

    /** {@inheritDoc} */
    @Override
    public String updateType() {
        return "message_removed";
    }

    /**
     * Creates a MessageRemovedUpdate.
     *
     * @param messageId must not be {@code null}
     */
    public MessageRemovedUpdate {
        Objects.requireNonNull(messageId, "messageId must not be null");
    }
}
