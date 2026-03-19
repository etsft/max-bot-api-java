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
 * Extended bot information returned by {@code GET /me}.
 *
 * @param userId          unique user identifier
 * @param name            display name
 * @param username        optional username (handle)
 * @param isBot           {@code true} if this user is a bot
 * @param lastActivityTime timestamp of last activity (epoch millis)
 * @param description     optional bot description
 * @param avatarUrl       optional avatar thumbnail URL
 * @param fullAvatarUrl   optional full-size avatar URL
 * @param commands        optional list of bot commands
 */
public record BotInfo(
        long userId,
        String name,
        @Nullable String username,
        boolean isBot,
        long lastActivityTime,
        @Nullable String description,
        @Nullable String avatarUrl,
        @Nullable String fullAvatarUrl,
        @Nullable List<BotCommand> commands
) {

    /**
     * Creates a BotInfo.
     *
     * @param name must not be {@code null}
     */
    public BotInfo {
        Objects.requireNonNull(name, "name must not be null");
        commands = commands == null ? null : List.copyOf(commands);
    }
}
