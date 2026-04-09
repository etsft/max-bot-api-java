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

package ru.max.botapi.core;

/**
 * Functional interface for handling errors that occur during long polling.
 *
 * <p>Implement this interface and declare it as a Spring bean to receive
 * notifications of polling errors (network failures, API errors, update
 * handler exceptions). The consumer will apply exponential backoff and
 * continue polling after each error.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * @Bean
 * PollingErrorHandler pollingErrorHandler() {
 *     return e -> log.error("Polling error: {}", e.getMessage(), e);
 * }
 * }</pre>
 *
 * <p>When no bean is declared, errors are logged at WARN level by default.</p>
 *
 * @see UpdateHandler
 */
@FunctionalInterface
public interface PollingErrorHandler {

    /**
     * Called when an error occurs during polling or update handling.
     *
     * @param e the exception that was thrown; never {@code null}
     */
    void onError(Exception e);
}
