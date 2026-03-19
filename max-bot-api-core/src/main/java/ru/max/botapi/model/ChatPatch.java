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
 * Request body for {@code PATCH /chats/{chatId}} to update chat information.
 *
 * <p>All fields are optional; only non-null fields will be updated.</p>
 *
 * @param title  new chat title
 * @param icon   new chat icon
 * @param pin    message ID to pin
 * @param notify whether to notify participants about the change
 */
public record ChatPatch(
        @Nullable String title,
        @Nullable Image icon,
        @Nullable String pin,
        // TODO: Jackson adapter must map this field to "notify" in JSON
        //       via @JsonProperty("notify") or custom naming strategy
        @Nullable Boolean notifyRecipients
) {
}
