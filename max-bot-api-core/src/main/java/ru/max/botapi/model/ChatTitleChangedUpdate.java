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
 * Update: the chat title was changed.
 *
 * @param timestamp event timestamp (epoch millis)
 * @param chatId    chat whose title was changed
 * @param user      the user who changed the title
 * @param title     the new chat title
 */
public record ChatTitleChangedUpdate(
        long timestamp,
        long chatId,
        User user,
        String title
) implements Update {

    /** {@inheritDoc} */
    @Override
    public String updateType() {
        return "chat_title_changed";
    }

    /**
     * Creates a ChatTitleChangedUpdate.
     *
     * @param user  must not be {@code null}
     * @param title must not be {@code null}
     */
    public ChatTitleChangedUpdate {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(title, "title must not be null");
    }
}
