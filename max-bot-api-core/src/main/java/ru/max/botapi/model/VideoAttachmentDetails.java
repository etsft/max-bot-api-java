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
 * Detailed information about a video attachment returned by {@code GET /videos/{videoToken}}.
 *
 * @param url       video URL
 * @param token     video token
 * @param thumbnail optional thumbnail object with its own URL
 * @param width     optional video width in pixels
 * @param height    optional video height in pixels
 * @param duration  optional duration in seconds
 */
public record VideoAttachmentDetails(
        String url,
        String token,
        @Nullable VideoThumbnail thumbnail,
        @Nullable Integer width,
        @Nullable Integer height,
        @Nullable Integer duration
) {

    /**
     * Creates a VideoAttachmentDetails.
     *
     * @param url   must not be {@code null}
     * @param token must not be {@code null}
     */
    public VideoAttachmentDetails {
        Objects.requireNonNull(url, "url must not be null");
        Objects.requireNonNull(token, "token must not be null");
    }
}
