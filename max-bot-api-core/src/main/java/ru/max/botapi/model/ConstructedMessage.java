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
 * A message constructed by the bot (message construction flow).
 *
 * <p>This is a distinct API type from {@link Message}, containing only
 * a subset of fields: sender, timestamp, link, and body.</p>
 *
 * @param sender    the sender of the constructed message
 * @param timestamp message timestamp (epoch millis)
 * @param link      optional link to another message
 * @param body      the message content
 */
public record ConstructedMessage(
        @Nullable User sender,
        long timestamp,
        @Nullable LinkedMessage link,
        MessageBody body
) {

    /**
     * Creates a ConstructedMessage.
     *
     * @param body must not be {@code null}
     */
    public ConstructedMessage {
        Objects.requireNonNull(body, "body must not be null");
    }
}
