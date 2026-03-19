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
 * Update: a user was removed from a chat.
 *
 * @param timestamp event timestamp (epoch millis)
 * @param chatId    chat where the user was removed
 * @param user      the removed user
 * @param adminId   ID of the admin who removed the user (null if user left voluntarily)
 * @param isChannel {@code true} if the chat is a channel
 */
public record UserRemovedUpdate(
        long timestamp,
        long chatId,
        User user,
        @Nullable Long adminId,
        boolean isChannel
) implements Update {

    /** {@inheritDoc} */
    @Override
    public String updateType() {
        return "user_removed";
    }

    /**
     * Creates a UserRemovedUpdate.
     *
     * @param user must not be {@code null}
     */
    public UserRemovedUpdate {
        Objects.requireNonNull(user, "user must not be null");
    }
}
