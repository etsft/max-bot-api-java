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
 * Update: the bot was added to a chat.
 *
 * @param timestamp event timestamp (epoch millis)
 * @param chatId    chat where the bot was added
 * @param user      the user who added the bot
 * @param isChannel {@code true} if added to a channel
 */
public record BotAddedUpdate(
        long timestamp,
        long chatId,
        User user,
        boolean isChannel
) implements Update {

    /** {@inheritDoc} */
    @Override
    public String updateType() {
        return "bot_added";
    }

    /**
     * Creates a BotAddedUpdate.
     *
     * @param user must not be {@code null}
     */
    public BotAddedUpdate {
        Objects.requireNonNull(user, "user must not be null");
    }
}
