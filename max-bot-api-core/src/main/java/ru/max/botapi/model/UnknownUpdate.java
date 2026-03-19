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
 * Forward-compatibility fallback for unrecognized update types.
 *
 * @param updateType the unrecognized update type discriminator
 * @param timestamp  event timestamp (epoch millis)
 * @param rawJson    the raw JSON string of the update
 */
public record UnknownUpdate(String updateType, long timestamp, String rawJson) implements Update {

    /**
     * Creates an UnknownUpdate.
     *
     * @param updateType must not be {@code null}
     * @param rawJson    must not be {@code null}
     */
    public UnknownUpdate {
        Objects.requireNonNull(updateType, "updateType must not be null");
        Objects.requireNonNull(rawJson, "rawJson must not be null");
    }
}
