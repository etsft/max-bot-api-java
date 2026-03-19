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

import java.time.Duration;

import ru.max.botapi.model.Nullable;

/**
 * Exception thrown when the MAX Bot API returns HTTP 429 (Too Many Requests).
 */
public class MaxRateLimitException extends MaxApiException {

    private static final long serialVersionUID = 1L;

    private final @Nullable Duration retryAfter;

    /**
     * Creates a MaxRateLimitException.
     *
     * @param statusCode   HTTP status code (429)
     * @param errorMessage error message
     * @param errorCode    optional API error code
     * @param retryAfter   suggested wait duration before retry, or {@code null}
     */
    public MaxRateLimitException(
            int statusCode,
            String errorMessage,
            @Nullable String errorCode,
            @Nullable Duration retryAfter
    ) {
        super(statusCode, errorMessage, errorCode);
        this.retryAfter = retryAfter;
    }

    /**
     * Returns the suggested wait duration before retrying.
     *
     * @return retry-after duration, or {@code null} if not specified
     */
    public @Nullable Duration retryAfter() {
        return retryAfter;
    }
}
