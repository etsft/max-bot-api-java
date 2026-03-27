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

import ru.max.botapi.model.Nullable;

/**
 * Exception thrown when the MAX Bot API returns an error response (4xx/5xx).
 */
public class MaxApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * HTTP status code
     */
    private final int statusCode;

    /**
     * Human-readable error message
     */
    private final String errorMessage;

    /**
     * Optional API error code
     */
    private final @Nullable String errorCode;

    /**
     * Creates a MaxApiException.
     *
     * @param statusCode   HTTP status code
     * @param errorMessage human-readable error message
     * @param errorCode    optional API error code
     */
    public MaxApiException(int statusCode, String errorMessage, @Nullable String errorCode) {
        super("HTTP " + statusCode + ": " + errorMessage);
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    /**
     * Returns the HTTP status code.
     *
     * @return status code
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Returns the error message from the API.
     *
     * @return error message
     */
    public String errorMessage() {
        return errorMessage;
    }

    /**
     * Returns the optional error code from the API.
     *
     * @return error code, or {@code null}
     */
    public @Nullable String errorCode() {
        return errorCode;
    }
}
