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
 * A user promoted to administrator of a chat or channel, along with the
 * rights granted to them.
 *
 * <p>Used as an element of {@link ChatAdminsList#admins()} in
 * {@code POST /chats/{chatId}/members/admins}.</p>
 *
 * @param userId      identifier of the user being promoted
 * @param permissions rights granted to this admin; see {@link ChatPermission}
 * @param alias       optional role label shown next to the user's name in chat settings
 */
public record ChatAdmin(
        Long userId,
        List<ChatPermission> permissions,
        @Nullable String alias
) {

    /**
     * Creates a ChatAdmin.
     *
     * @param userId      must not be {@code null}
     * @param permissions must not be {@code null}
     */
    public ChatAdmin {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(permissions, "permissions must not be null");
        permissions = List.copyOf(permissions);
    }

    /**
     * Creates a ChatAdmin without an alias.
     *
     * @param userId      the user identifier
     * @param permissions rights granted to this admin
     */
    public ChatAdmin(Long userId, List<ChatPermission> permissions) {
        this(userId, permissions, null);
    }
}
