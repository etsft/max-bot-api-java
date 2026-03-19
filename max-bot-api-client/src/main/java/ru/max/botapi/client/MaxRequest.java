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

import java.util.Map;
import java.util.Objects;

import ru.max.botapi.model.Nullable;

/**
 * An HTTP request to be sent to the MAX Bot API.
 *
 * @param method      HTTP method
 * @param path        API path (e.g., "/messages")
 * @param queryParams query parameters to append to the URL
 * @param body        JSON request body (null for GET/DELETE)
 * @param headers     additional headers
 */
public record MaxRequest(
        HttpMethod method,
        String path,
        Map<String, String> queryParams,
        @Nullable String body,
        Map<String, String> headers
) {

    /**
     * Creates a MaxRequest.
     *
     * @param method      must not be {@code null}
     * @param path        must not be {@code null}
     * @param queryParams must not be {@code null}
     * @param headers     must not be {@code null}
     */
    public MaxRequest {
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(queryParams, "queryParams must not be null");
        Objects.requireNonNull(headers, "headers must not be null");
        queryParams = Map.copyOf(queryParams);
        headers = Map.copyOf(headers);
    }
}
