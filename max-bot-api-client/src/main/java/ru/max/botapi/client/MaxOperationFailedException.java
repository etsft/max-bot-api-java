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

package ru.max.botapi.client;

import java.util.Objects;

import ru.max.botapi.model.Nullable;

/**
 * Thrown when MAX answers HTTP 200 but refuses the operation with {@code success:false}.
 *
 * <p>Such a refusal is an ordinary result for the API calls themselves — they return a
 * {@code SimpleQueryResult} whose {@code success()} is {@code false} — so the client never
 * throws this. Higher-level helpers that cannot go on after a refusal, such as webhook
 * registration, throw it instead, carrying the {@link #errorCode()} and
 * {@link #errorMessage()} from the response.</p>
 *
 * <p>It is deliberately not a {@link MaxApiException}: that one stands for an HTTP error
 * status, and a {@code catch} written for it should not also receive refusals answered with
 * HTTP 200. It extends {@link IllegalStateException}, which these helpers threw before.</p>
 */
public class MaxOperationFailedException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    /**
     * Machine-readable reason, for example {@code not.found}
     */
    private final @Nullable String errorCode;

    /**
     * Human-readable reason
     */
    private final @Nullable String errorMessage;

    /**
     * Creates a MaxOperationFailedException.
     *
     * @param operation    what was refused, for the exception message, for example
     *                     {@code "webhook registration"}; must not be {@code null}
     * @param errorCode    the {@code code} from the response, or {@code null} if it had none
     * @param errorMessage the {@code message} from the response, or {@code null} if it had none
     */
    public MaxOperationFailedException(String operation, @Nullable String errorCode,
                                       @Nullable String errorMessage) {
        super(describe(Objects.requireNonNull(operation, "operation must not be null"),
                errorCode, errorMessage));
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * Builds the exception message, leaving out the parts MAX did not send.
     *
     * @param operation    what was refused
     * @param errorCode    the code, or {@code null}
     * @param errorMessage the message, or {@code null}
     * @return e.g. {@code MAX API rejected webhook unregistration: code=not.found, message=...}
     */
    private static String describe(String operation, @Nullable String errorCode,
                                   @Nullable String errorMessage) {
        StringBuilder text = new StringBuilder("MAX API rejected ").append(operation);
        String separator = ": ";
        if (errorCode != null) {
            text.append(separator).append("code=").append(errorCode);
            separator = ", ";
        }
        if (errorMessage != null) {
            text.append(separator).append("message=").append(errorMessage);
        }
        return text.toString();
    }

    /**
     * Returns the machine-readable reason of the refusal. Identify refusals by this value,
     * not by {@link #errorMessage()}.
     *
     * @return the error code, for example {@code not.found}, or {@code null} if MAX sent none
     */
    public @Nullable String errorCode() {
        return errorCode;
    }

    /**
     * Returns the human-readable reason of the refusal.
     *
     * @return the error message, or {@code null} if MAX sent none
     */
    public @Nullable String errorMessage() {
        return errorMessage;
    }
}
