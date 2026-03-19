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
 * Request body for {@code PATCH /me} to update bot information.
 *
 * <p>All fields are optional; only non-null fields will be updated.</p>
 *
 * @param name        new bot display name
 * @param description new bot description
 * @param commands    new list of bot commands
 * @param photo       new bot avatar photo
 */
public record BotPatch(
        @Nullable String name,
        @Nullable String description,
        @Nullable List<BotCommand> commands,
        @Nullable PhotoAttachmentRequestPayload photo
) {

    /**
     * Compact constructor for defensive copies.
     */
    public BotPatch {
        commands = commands == null ? null : List.copyOf(commands);
    }
}
