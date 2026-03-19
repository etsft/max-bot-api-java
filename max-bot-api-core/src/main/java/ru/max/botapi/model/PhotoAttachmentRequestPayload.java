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

import java.util.Map;

/**
 * Payload for image/photo attachment requests.
 *
 * <p>At least one of {@code token}, {@code url}, or {@code photos} should be provided.</p>
 *
 * @param token  upload token for previously uploaded photo
 * @param url    external URL of the photo
 * @param photos map of size keys to token references for multi-size photos
 */
public record PhotoAttachmentRequestPayload(
        @Nullable String token,
        @Nullable String url,
        @Nullable Map<String, TokenRef> photos
) {

    /**
     * Compact constructor for defensive copies.
     */
    public PhotoAttachmentRequestPayload {
        photos = photos == null ? null : Map.copyOf(photos);
    }

    /**
     * A reference to an uploaded photo token.
     *
     * @param token the upload token
     */
    public record TokenRef(String token) {
    }
}
