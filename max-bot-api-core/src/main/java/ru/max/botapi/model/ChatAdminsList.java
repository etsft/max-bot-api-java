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
 * Request body for {@code POST /chats/{chatId}/members/admins} — promotes
 * members to administrators with the given per-admin permissions.
 *
 * @param admins the admins to promote, each with their own {@link ChatPermission} set
 */
public record ChatAdminsList(List<ChatAdmin> admins) {

    /**
     * Creates a ChatAdminsList.
     *
     * @param admins must not be {@code null}
     */
    public ChatAdminsList {
        Objects.requireNonNull(admins, "admins must not be null");
        admins = List.copyOf(admins);
    }
}
