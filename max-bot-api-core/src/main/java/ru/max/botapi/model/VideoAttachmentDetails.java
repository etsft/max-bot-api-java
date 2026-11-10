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
 * <p>Only {@code token} is guaranteed. The remaining fields describe a video MAX may still be
 * transcoding: a poll right after the upload can come back with nothing but the token, and the
 * renditions, size and duration fill in as processing proceeds.</p>
 *
 * @param token     video token
 * @param urls      optional playback URLs, absent while the video is unavailable
 * @param thumbnail optional thumbnail image
 * @param width     optional video width in pixels
 * @param height    optional video height in pixels
 * @param duration  optional duration
 */
public record VideoAttachmentDetails(
        String token,
        @Nullable VideoUrls urls,
        @Nullable Thumbnail thumbnail,
        @Nullable Integer width,
        @Nullable Integer height,
        @Nullable Integer duration
) {

    /**
     * Creates a VideoAttachmentDetails.
     *
     * @param token must not be {@code null}
     */
    public VideoAttachmentDetails {
        Objects.requireNonNull(token, "token must not be null");
    }

    /**
     * Thumbnail image of a video, as returned by {@code GET /videos/{videoToken}}.
     *
     * <p>The API documents all three fields as always present, but they are modelled as
     * optional so that a partial thumbnail of a video still being processed does not fail the
     * whole response.</p>
     *
     * @param photoId unique identifier of the thumbnail image
     * @param token   token of the thumbnail image, usable to reattach it
     * @param url     thumbnail image URL
     */
    public record Thumbnail(
            @Nullable Long photoId,
            @Nullable String token,
            @Nullable String url
    ) {
    }
}
