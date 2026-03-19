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
 * Sealed interface representing an update event from the MAX Bot API.
 *
 * <p>Discriminated by the {@code update_type} field in JSON. {@link UnknownUpdate}
 * serves as a forward-compatibility fallback for unrecognized update types.</p>
 */
public sealed interface Update permits
        MessageCreatedUpdate, MessageCallbackUpdate, MessageEditedUpdate,
        MessageRemovedUpdate, BotAddedUpdate, BotRemovedUpdate,
        UserAddedUpdate, UserRemovedUpdate, BotStartedUpdate, BotStoppedUpdate,
        ChatTitleChangedUpdate, MessageConstructionRequestUpdate,
        MessageConstructedUpdate, MessageChatCreatedUpdate,
        UnknownUpdate {

    /**
     * Returns the update type discriminator string.
     *
     * @return update type (e.g., "message_created", "bot_added")
     */
    String updateType();

    /**
     * Returns the timestamp of this update event (epoch millis).
     *
     * @return timestamp in epoch milliseconds
     */
    long timestamp();
}
