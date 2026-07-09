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
 * Update: a user muted notifications for the dialog with the bot.
 *
 * @param timestamp  event timestamp (epoch millis)
 * @param chatId     chat where the dialog was muted
 * @param user       the user who muted the dialog
 * @param mutedUntil epoch millis until which the dialog stays muted
 * @param userLocale locale of the user
 */
public record DialogMutedUpdate(
        long timestamp,
        long chatId,
        User user,
        long mutedUntil,
        @Nullable String userLocale
) implements Update {

    /** {@inheritDoc} */
    @Override
    public String updateType() {
        return "dialog_muted";
    }

    /**
     * Creates a DialogMutedUpdate.
     *
     * @param user must not be {@code null}
     */
    public DialogMutedUpdate {
        Objects.requireNonNull(user, "user must not be null");
    }
}
