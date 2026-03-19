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
 * Photo/image attachment.
 *
 * @param payload photo payload with URL, token, and photo ID
 */
public record PhotoAttachment(PhotoPayload payload) implements Attachment {

    /** {@inheritDoc} */
    @Override
    public String type() {
        return "image";
    }

    /**
     * Creates a PhotoAttachment.
     *
     * @param payload must not be {@code null}
     */
    public PhotoAttachment {
        Objects.requireNonNull(payload, "payload must not be null");
    }

    /**
     * Payload for a photo attachment.
     *
     * @param url     photo URL
     * @param token   photo token
     * @param photoId unique photo identifier
     */
    public record PhotoPayload(String url, String token, long photoId) {

        /**
         * Creates a PhotoPayload.
         *
         * @param url   must not be {@code null}
         * @param token must not be {@code null}
         */
        public PhotoPayload {
            Objects.requireNonNull(url, "url must not be null");
            Objects.requireNonNull(token, "token must not be null");
        }
    }
}
