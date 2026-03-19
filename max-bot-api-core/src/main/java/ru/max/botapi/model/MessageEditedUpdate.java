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
 * Update: a message was edited.
 *
 * @param timestamp event timestamp (epoch millis)
 * @param message   the edited message (with updated content)
 */
public record MessageEditedUpdate(long timestamp, Message message) implements Update {

    /** {@inheritDoc} */
    @Override
    public String updateType() {
        return "message_edited";
    }

    /**
     * Creates a MessageEditedUpdate.
     *
     * @param message must not be {@code null}
     */
    public MessageEditedUpdate {
        Objects.requireNonNull(message, "message must not be null");
    }
}
