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

import java.util.concurrent.CompletableFuture;

/**
 * Transport-level HTTP client interface for the MAX Bot API.
 *
 * <p>Implementations handle raw HTTP request/response exchange, including
 * authentication header injection and URI construction.</p>
 */
public interface MaxTransportClient extends AutoCloseable {

    @Override
    void close();

    /**
     * Executes the given request synchronously.
     *
     * @param request the API request
     * @return the API response
     * @throws MaxClientException if a transport error occurs
     */
    MaxResponse execute(MaxRequest request) throws MaxClientException;

    /**
     * Executes the given request asynchronously.
     *
     * @param request the API request
     * @return a future that completes with the API response
     */
    CompletableFuture<MaxResponse> executeAsync(MaxRequest request);
}
