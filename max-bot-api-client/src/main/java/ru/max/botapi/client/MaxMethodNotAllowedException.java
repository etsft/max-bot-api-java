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
 * Exception thrown when the MAX Bot API returns HTTP 405 (Method Not Allowed).
 *
 * <p>Indicates that the HTTP method used is not allowed for the requested endpoint.
 * This typically indicates a client-side bug in request construction.</p>
 */
public class MaxMethodNotAllowedException extends MaxApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a MaxMethodNotAllowedException.
     *
     * @param statusCode   HTTP status code (405)
     * @param errorMessage error message
     * @param errorCode    optional API error code
     */
    public MaxMethodNotAllowedException(int statusCode, String errorMessage,
            @Nullable String errorCode) {
        super(statusCode, errorMessage, errorCode);
    }
}
