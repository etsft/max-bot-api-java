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

/**
 * Update: the bot's administrator permissions in a chat changed.
 *
 * <p>Arrives both when the bot is promoted to (or demoted from) administrator and when its set
 * of administrator permissions is edited.</p>
 *
 * @param timestamp   event timestamp (epoch millis)
 * @param chatId      chat where the permissions changed
 * @param userId      user or bot who changed the permissions
 * @param botId       bot whose permissions changed
 * @param isChannel   {@code true} if the chat is a channel
 * @param isAdmin     {@code true} if the bot is an administrator after the change
 * @param permissions the bot's administrator permissions after the change, if sent;
 *                    unknown permission values are filtered out for forward-compatibility
 */
public record BotAdminPermissionsChangedUpdate(
        long timestamp,
        long chatId,
        long userId,
        long botId,
        boolean isChannel,
        boolean isAdmin,
        @Nullable List<ChatPermission> permissions
) implements Update {

    /** {@inheritDoc} */
    @Override
    public String updateType() {
        return "bot_admin_permissions_changed";
    }

    /**
     * Creates a BotAdminPermissionsChangedUpdate.
     *
     * <p>{@code null} entries in {@code permissions} (unknown values read by the serializer)
     * are dropped.</p>
     */
    public BotAdminPermissionsChangedUpdate {
        permissions = ModelLists.withoutNullsOrNull(permissions);
    }
}
