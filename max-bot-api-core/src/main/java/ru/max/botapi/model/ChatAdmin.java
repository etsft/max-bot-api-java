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
 * One entry of a {@link ChatAdminsList}: the user or bot to promote and the rights to grant.
 *
 * <p>The rights are not independent. {@code EDIT}, {@code DELETE}, {@code WRITE} and
 * {@code PIN_MESSAGE} are only accepted when {@link ChatPermission#READ_ALL_MESSAGES} is
 * already held or granted in the same call, otherwise the API rejects the request. A bot
 * without {@code READ_ALL_MESSAGES} also stops receiving updates over a webhook.</p>
 *
 * @param userId      identifier of the user or bot receiving the rights
 * @param permissions rights to grant; must not be {@code null}
 * @param alias       optional label shown for this administrator
 */
public record ChatAdmin(
        long userId,
        List<ChatPermission> permissions,
        @Nullable String alias
) {

    /**
     * Creates a ChatAdmin.
     *
     * @param permissions must not be {@code null}
     */
    public ChatAdmin {
        Objects.requireNonNull(permissions, "permissions must not be null");
        permissions = List.copyOf(permissions);
    }

    /**
     * Creates a ChatAdmin without an alias.
     *
     * @param userId      identifier of the user or bot receiving the rights
     * @param permissions rights to grant; must not be {@code null}
     */
    public ChatAdmin(long userId, List<ChatPermission> permissions) {
        this(userId, permissions, null);
    }
}
