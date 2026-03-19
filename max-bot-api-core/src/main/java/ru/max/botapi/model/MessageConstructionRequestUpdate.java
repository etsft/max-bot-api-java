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
 * Update: a message construction request from a user (message construction flow).
 *
 * @param timestamp  event timestamp (epoch millis)
 * @param user       the user requesting message construction
 * @param userLocale locale of the user
 * @param sessionId  construction session identifier
 * @param data       optional data from the previous construction step
 * @param input      optional input from the user (raw JSON string)
 */
public record MessageConstructionRequestUpdate(
        long timestamp,
        User user,
        @Nullable String userLocale,
        String sessionId,
        @Nullable String data,
        @Nullable String input
) implements Update {

    /** {@inheritDoc} */
    @Override
    public String updateType() {
        return "message_construction_request";
    }

    /**
     * Creates a MessageConstructionRequestUpdate.
     *
     * @param user      must not be {@code null}
     * @param sessionId must not be {@code null}
     */
    public MessageConstructionRequestUpdate {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
    }
}
