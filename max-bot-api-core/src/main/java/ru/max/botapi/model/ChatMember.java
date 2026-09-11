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
 * <p>The MAX API documents {@code name} as a legacy field that will be removed, and
 * {@code first_name} as its replacement. Neither can be relied on today: the documentation's
 * own {@code GET /me} example answers with {@code name} and no {@code first_name}, so both are
 * modelled as nullable and a caller that needs a display name should fall back from one to the
 * other.
 *
 * @param userId           unique user identifier
 * @param name             composite display name
 * @param firstName        first name component of the display name
 * @param lastName         optional last name component
 * @param username         optional username (handle)
 * @param isBot            {@code true} if this member is a bot
 * @param lastActivityTime timestamp of last activity (epoch millis); {@code null} when the
 *                         member's privacy settings hide their online status
 * @param description      optional profile description
 * @param avatarUrl        optional avatar thumbnail URL
 * @param fullAvatarUrl    optional full-size avatar URL
 * @param lastAccessTime   timestamp of last access to the chat (epoch millis)
 * @param isOwner          {@code true} if this member is the chat owner
 * @param isAdmin          {@code true} if this member is an admin
 * @param joinTime         timestamp when the member joined (epoch millis)
 * @param permissions      list of permissions granted to this member;
 *                         unknown permission values are filtered out for forward-compatibility
 * @param alias            label shown next to this member's name in the chat settings; absent
 *                         when an administrator or owner has no label of their own, in which
 *                         case the client substitutes one
 */
public record ChatMember(
        long userId,
        @Nullable String name,
        @Nullable String firstName,
        @Nullable String lastName,
        @Nullable String username,
        boolean isBot,
        @Nullable Long lastActivityTime,
        @Nullable String description,
        @Nullable String avatarUrl,
        @Nullable String fullAvatarUrl,
        long lastAccessTime,
        boolean isOwner,
        boolean isAdmin,
        long joinTime,
        @Nullable List<ChatPermission> permissions,
        @Nullable String alias
) {

    /**
     * Creates a ChatMember.
     *
     * <p>Unknown {@link ChatPermission} values (deserialized as {@code null} by the
     * forward-compatible deserializer) are filtered out so that callers receive a
     * clean list of known permissions without {@code NullPointerException}s.</p>
     */
    public ChatMember {
        if (permissions != null) {
            permissions = permissions.stream()
                    .filter(Objects::nonNull)
                    .toList();
        }
    }
}
