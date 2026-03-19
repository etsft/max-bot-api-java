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
 * Callback data from an inline keyboard button press.
 *
 * <p>Extracted as a top-level type (rather than an inner record of
 * {@link MessageCallbackUpdate}) for import ergonomics and reuse
 * in callback answer scenarios ({@code POST /answers}).</p>
 *
 * @param timestamp  callback timestamp (epoch millis)
 * @param callbackId unique callback identifier
 * @param payload    callback payload data
 * @param user       the user who pressed the button
 */
public record Callback(
        long timestamp,
        String callbackId,
        @Nullable String payload,
        User user
) {

    /**
     * Creates a Callback.
     *
     * @param callbackId must not be {@code null}
     * @param user       must not be {@code null}
     */
    public Callback {
        Objects.requireNonNull(callbackId, "callbackId must not be null");
        Objects.requireNonNull(user, "user must not be null");
    }
}
