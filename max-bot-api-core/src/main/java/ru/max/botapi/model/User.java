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
 * Represents a user in MAX Messenger.
 *
 * @param userId          unique user identifier
 * @param name            display name of the user
 * @param username        optional username (handle)
 * @param isBot           {@code true} if this user is a bot
 * @param lastActivityTime timestamp of last activity (epoch millis)
 */
public record User(
        long userId,
        String name,
        @Nullable String username,
        boolean isBot,
        long lastActivityTime
) {

    /**
     * Creates a User.
     *
     * @param name must not be {@code null}
     */
    public User {
        Objects.requireNonNull(name, "name must not be null");
    }
}
