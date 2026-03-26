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
 * Configuration for the MAX Bot API client.
 *
 * @param baseUrl              API base URL
 * @param connectTimeout       HTTP connection timeout
 * @param requestTimeout       HTTP request timeout
 * @param longPollTimeout      timeout for long-polling requests
 * @param maxRetries           maximum number of retries for retryable errors
 * @param enableRateLimiting   whether to enable client-side rate limiting
 * @param maxRequestsPerSecond maximum requests per second when rate limiting is enabled
 */
public record MaxClientConfig(
        String baseUrl,
        Duration connectTimeout,
        Duration requestTimeout,
        Duration longPollTimeout,
        int maxRetries,
        boolean enableRateLimiting,
        int maxRequestsPerSecond
) {

    /**
     * Creates a MaxClientConfig.
     *
     * @param baseUrl         must not be {@code null}
     * @param connectTimeout  must not be {@code null}
     * @param requestTimeout  must not be {@code null}
     * @param longPollTimeout must not be {@code null}; must be less than {@code requestTimeout}
     * @throws IllegalArgumentException if {@code longPollTimeout >= requestTimeout}
     */
    public MaxClientConfig {
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
        Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
        Objects.requireNonNull(longPollTimeout, "longPollTimeout must not be null");
        if (longPollTimeout.compareTo(requestTimeout) >= 0) {
            throw new IllegalArgumentException(
                    "longPollTimeout (" + longPollTimeout.toSeconds() + "s) must be less than "
                    + "requestTimeout (" + requestTimeout.toSeconds() + "s); otherwise the HTTP "
                    + "request will time out before the server can respond");
        }
    }

    /**
     * Returns a configuration with sensible defaults.
     *
     * @return default configuration
     */
    public static MaxClientConfig defaults() {
        return new MaxClientConfig(
                "https://platform-api.max.ru",
                Duration.ofSeconds(10),
                Duration.ofSeconds(60),
                Duration.ofSeconds(30),
                3,
                true,
                30
        );
    }

    /**
     * Returns a new builder initialized with default values.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MaxClientConfig}.
     */
    public static class Builder {

        private String baseUrl = "https://platform-api.max.ru";
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration requestTimeout = Duration.ofSeconds(60);
        private Duration longPollTimeout = Duration.ofSeconds(30);
        private int maxRetries = 3;
        private boolean enableRateLimiting = true;
        private int maxRequestsPerSecond = 30;

        Builder() {
        }

        /**
         * Sets the API base URL.
         *
         * @param baseUrl the base URL
         * @return this builder
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = Objects.requireNonNull(baseUrl);
            return this;
        }

        /**
         * Sets the connection timeout.
         *
         * @param connectTimeout the connection timeout
         * @return this builder
         */
        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = Objects.requireNonNull(connectTimeout);
            return this;
        }

        /**
         * Sets the request timeout.
         *
         * @param requestTimeout the request timeout
         * @return this builder
         */
        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = Objects.requireNonNull(requestTimeout);
            return this;
        }

        /**
         * Sets the long-poll timeout.
         *
         * @param longPollTimeout the long-poll timeout
         * @return this builder
         */
        public Builder longPollTimeout(Duration longPollTimeout) {
            this.longPollTimeout = Objects.requireNonNull(longPollTimeout);
            return this;
        }

        /**
         * Sets the maximum number of retries.
         *
         * @param maxRetries the maximum retry count
         * @return this builder
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Sets whether to enable rate limiting.
         *
         * @param enableRateLimiting {@code true} to enable
         * @return this builder
         */
        public Builder enableRateLimiting(boolean enableRateLimiting) {
            this.enableRateLimiting = enableRateLimiting;
            return this;
        }

        /**
         * Sets the maximum requests per second.
         *
         * @param maxRequestsPerSecond the max RPS
         * @return this builder
         */
        public Builder maxRequestsPerSecond(int maxRequestsPerSecond) {
            this.maxRequestsPerSecond = maxRequestsPerSecond;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return the built MaxClientConfig
         */
        public MaxClientConfig build() {
            return new MaxClientConfig(
                    baseUrl, connectTimeout, requestTimeout, longPollTimeout,
                    maxRetries, enableRateLimiting, maxRequestsPerSecond
            );
        }
    }
}
