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
 * Video attachment received in an incoming message.
 *
 * @param payload   video payload with URL, token, and internal ID
 * @param thumbnail optional thumbnail object with its own URL
 * @param width     optional video width in pixels
 * @param height    optional video height in pixels
 * @param duration  optional video duration in seconds
 */
public record VideoAttachment(
        VideoPayload payload,
        @Nullable VideoThumbnail thumbnail,
        @Nullable Integer width,
        @Nullable Integer height,
        @Nullable Integer duration
) implements Attachment {

    /** {@inheritDoc} */
    @Override
    public String type() {
        return "video";
    }

    /**
     * Creates a VideoAttachment.
     *
     * @param payload must not be {@code null}
     */
    public VideoAttachment {
        Objects.requireNonNull(payload, "payload must not be null");
    }

    /**
     * Payload of an incoming video attachment.
     *
     * @param url   video streaming URL
     * @param token video token (stable reference for the video)
     * @param id    optional internal video ID
     */
    public record VideoPayload(String url, String token, @Nullable Long id) {

        /**
         * Creates a VideoPayload.
         *
         * @param url   must not be {@code null}
         * @param token must not be {@code null}
         */
        public VideoPayload {
            Objects.requireNonNull(url, "url must not be null");
            Objects.requireNonNull(token, "token must not be null");
        }
    }
}
