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
 * Simple result with success flag and optional message.
 *
 * <p>MAX answers some refusals with HTTP 200 and {@code success:false} rather than with an
 * error status, so such a result is not an exception. Tell refusals apart by {@link #code()},
 * not by {@link #message()}: the code is a stable machine-readable value such as
 * {@code not.found} or {@code chat.denied}, while the message is free text that may change.
 * The code is not guaranteed even when {@code success} is {@code false}, so it may be
 * {@code null}. Errors answered with an HTTP 4xx status are reported as
 * {@code MaxApiException} instead, with the code in its {@code errorCode()}.</p>
 *
 * @param success whether the operation was successful
 * @param message optional human-readable message
 * @param code    machine-readable reason of an unsuccessful request, for example
 *                {@code not.found} or {@code chat.denied}; {@code null} on success and
 *                possibly also when {@code success} is {@code false}
 */
public record SimpleQueryResult(
        boolean success,
        @Nullable String message,
        @Nullable String code
) {

    /**
     * Creates a SimpleQueryResult without a code.
     *
     * @param success whether the operation was successful
     * @param message optional human-readable message
     */
    public SimpleQueryResult(boolean success, @Nullable String message) {
        this(success, message, null);
    }
}
