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
 * Update: a user pressed an inline keyboard button (callback).
 *
 * @param timestamp  event timestamp (epoch millis)
 * @param callback   callback data
 * @param message    the message containing the pressed button
 * @param userLocale locale of the user who pressed the button
 */
public record MessageCallbackUpdate(
        long timestamp,
        Callback callback,
        @Nullable Message message,
        @Nullable String userLocale
) implements Update {

    /** {@inheritDoc} */
    @Override
    public String updateType() {
        return "message_callback";
    }

    /**
     * Creates a MessageCallbackUpdate.
     *
     * @param callback must not be {@code null}
     */
    public MessageCallbackUpdate {
        Objects.requireNonNull(callback, "callback must not be null");
    }
}
