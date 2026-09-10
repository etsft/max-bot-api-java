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
 * Update: the user unmuted notifications in their dialog with the bot.
 *
 * <p>The MAX documentation names this event but does not spell out its payload, so every
 * component beyond the timestamp is modelled as optional. Unknown JSON properties are
 * ignored, so a richer payload than this deserializes without error.</p>
 *
 * @param timestamp event timestamp (epoch millis)
 * @param chatId    the dialog the event happened in
 * @param user      the user on the other side of the dialog
 */
public record DialogUnmutedUpdate(
        long timestamp,
        @Nullable Long chatId,
        @Nullable User user
) implements Update {

    /** {@inheritDoc} */
    @Override
    public String updateType() {
        return "dialog_unmuted";
    }
}
