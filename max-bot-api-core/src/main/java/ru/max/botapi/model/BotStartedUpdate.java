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
 * Update: a user started a conversation with the bot (pressed "Start").
 *
 * @param timestamp  event timestamp (epoch millis)
 * @param chatId     chat where the bot was started
 * @param user       the user who started the bot
 * @param payload    optional deep-link payload
 * @param userLocale locale of the user
 */
public record BotStartedUpdate(
        long timestamp,
        long chatId,
        User user,
        @Nullable String payload,
        @Nullable String userLocale
) implements Update {

    /** {@inheritDoc} */
    @Override
    public String updateType() {
        return "bot_started";
    }

    /**
     * Creates a BotStartedUpdate.
     *
     * @param user must not be {@code null}
     */
    public BotStartedUpdate {
        Objects.requireNonNull(user, "user must not be null");
    }
}
