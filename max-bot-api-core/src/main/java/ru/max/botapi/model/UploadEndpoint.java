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

package ru.max.botapi.model;

import java.util.Objects;

/**
 * Upload endpoint returned by {@code POST /uploads}.
 *
 * @param url   URL to upload the file to
 * @param token optional pre-assigned token
 */
public record UploadEndpoint(
        String url,
        @Nullable String token
) {

    /**
     * Creates an UploadEndpoint.
     *
     * @param url must not be {@code null}
     */
    public UploadEndpoint {
        Objects.requireNonNull(url, "url must not be null");
    }
}
