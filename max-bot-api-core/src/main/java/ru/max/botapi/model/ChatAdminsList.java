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
 * Request/response body for managing chat administrators.
 *
 * @param userIds list of user IDs to add/remove as admins
 */
public record ChatAdminsList(List<Long> userIds) {

    /**
     * Creates a ChatAdminsList.
     *
     * @param userIds must not be {@code null}
     */
    public ChatAdminsList {
        Objects.requireNonNull(userIds, "userIds must not be null");
        userIds = List.copyOf(userIds);
    }
}
