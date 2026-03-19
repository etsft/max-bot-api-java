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

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An HTTP response from the MAX Bot API.
 *
 * @param statusCode HTTP status code
 * @param body       response body as a string
 * @param headers    response headers
 */
public record MaxResponse(
        int statusCode,
        String body,
        Map<String, List<String>> headers
) {

    /**
     * Creates a MaxResponse.
     *
     * @param body    must not be {@code null}
     * @param headers must not be {@code null}
     */
    public MaxResponse {
        Objects.requireNonNull(body, "body must not be null");
        Objects.requireNonNull(headers, "headers must not be null");
    }
}
