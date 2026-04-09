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
 * A member of a chat with extended role information.
 *
 * @param userId           unique user identifier
 * @param name             composite display name
 * @param firstName        first name component of the display name
 * @param lastName         optional last name component
 * @param username         optional username (handle)
 * @param isBot            {@code true} if this member is a bot
 * @param lastActivityTime timestamp of last activity (epoch millis)
 * @param description      optional profile description
 * @param avatarUrl        optional avatar thumbnail URL
 * @param fullAvatarUrl    optional full-size avatar URL
 * @param lastAccessTime   timestamp of last access to the chat (epoch millis)
 * @param isOwner          {@code true} if this member is the chat owner
 * @param isAdmin          {@code true} if this member is an admin
 * @param joinTime         timestamp when the member joined (epoch millis)
 * @param permissions      list of permissions granted to this member;
 *                         unknown permission values are filtered out for forward-compatibility
 */
public record ChatMember(
        long userId,
        String name,
        @Nullable String firstName,
        @Nullable String lastName,
        @Nullable String username,
        boolean isBot,
        long lastActivityTime,
        @Nullable String description,
        @Nullable String avatarUrl,
        @Nullable String fullAvatarUrl,
        long lastAccessTime,
        boolean isOwner,
        boolean isAdmin,
        long joinTime,
        @Nullable List<ChatPermission> permissions
) {

    /**
     * Creates a ChatMember.
     *
     * <p>Unknown {@link ChatPermission} values (deserialized as {@code null} by the
     * forward-compatible deserializer) are filtered out so that callers receive a
     * clean list of known permissions without {@code NullPointerException}s.</p>
     *
     * @param name must not be {@code null}
     */
    public ChatMember {
        Objects.requireNonNull(name, "name must not be null");
        if (permissions != null) {
            permissions = permissions.stream()
                    .filter(Objects::nonNull)
                    .toList();
        }
    }
}
