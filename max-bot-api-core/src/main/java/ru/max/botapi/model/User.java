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

/**
 * Represents a user in MAX Messenger.
 *
 * <p>The MAX API documents {@code name} as a legacy field that will be removed, and
 * {@code first_name} as its replacement. Neither can be relied on today: the documentation's
 * own {@code GET /me} example answers with {@code name} and no {@code first_name}, so both are
 * modelled as nullable and a caller that needs a display name should fall back from one to the
 * other.
 *
 * @param userId           unique user identifier
 * @param name             composite display name (e.g. "Ivan Petrov")
 * @param firstName        first name
 * @param lastName         optional last name
 * @param username         optional username (handle)
 * @param isBot            {@code true} if this user is a bot
 * @param lastActivityTime timestamp of last activity (epoch millis)
 */
public record User(
        long userId,
        @Nullable String name,
        @Nullable String firstName,
        @Nullable String lastName,
        @Nullable String username,
        boolean isBot,
        long lastActivityTime
) {
}
