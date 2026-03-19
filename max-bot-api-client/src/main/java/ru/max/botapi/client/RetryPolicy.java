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
import java.util.Objects;

/**
 * Retry policy with exponential backoff for retryable HTTP status codes.
 *
 * <p>Retries are performed for HTTP 429 (Too Many Requests) and 503 (Service Unavailable).
 * Backoff follows an exponential pattern: 1s, 2s, 4s, etc.</p>
 */
public class RetryPolicy {

    private static final Duration DEFAULT_BASE_DELAY = Duration.ofSeconds(1);
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVICE_UNAVAILABLE = 503;

    private final int maxRetries;
    private final Duration baseDelay;

    /**
     * Creates a RetryPolicy with the given maximum retries and default 1-second base delay.
     *
     * @param maxRetries maximum number of retries (0 for no retries)
     */
    public RetryPolicy(int maxRetries) {
        this(maxRetries, DEFAULT_BASE_DELAY);
    }

    /**
     * Creates a RetryPolicy with configurable base delay.
     *
     * @param maxRetries maximum number of retries (0 for no retries)
     * @param baseDelay  base delay for exponential backoff
     */
    public RetryPolicy(int maxRetries, Duration baseDelay) {
        this.maxRetries = maxRetries;
        this.baseDelay = Objects.requireNonNull(baseDelay, "baseDelay must not be null");
    }

    /**
     * Returns whether the given status code is retryable.
     *
     * @param statusCode HTTP status code
     * @return {@code true} if the request should be retried
     */
    public boolean shouldRetry(int statusCode) {
        return statusCode == HTTP_TOO_MANY_REQUESTS || statusCode == HTTP_SERVICE_UNAVAILABLE;
    }

    /**
     * Returns the delay before the next retry attempt.
     *
     * @param attempt the current attempt number (0-based)
     * @return the backoff duration
     */
    public Duration getDelay(int attempt) {
        long multiplier = 1L << attempt;
        return baseDelay.multipliedBy(multiplier);
    }

    /**
     * Returns the maximum number of retries.
     *
     * @return max retries
     */
    public int maxRetries() {
        return maxRetries;
    }
}
