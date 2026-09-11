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
 * Update: the user muted notifications in their dialog with the bot.
 *
 * <p>Every component beyond the timestamp is modelled as optional, so a payload that omits
 * one still deserializes. Unknown JSON properties are ignored.</p>
 *
 * @param timestamp  event timestamp (epoch millis)
 * @param chatId     the dialog the event happened in
 * @param user       the user on the other side of the dialog
 * @param mutedUntil when the dialog is muted until (epoch millis)
 * @param userLocale the user's current language, as an IETF BCP 47 tag
 */
public record DialogMutedUpdate(
        long timestamp,
        @Nullable Long chatId,
        @Nullable User user,
        @Nullable Long mutedUntil,
        @Nullable String userLocale
) implements Update {

    /** {@inheritDoc} */
    @Override
    public String updateType() {
        return "dialog_muted";
    }
}
