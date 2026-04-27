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
import java.util.Objects;

/**
 * Result of a successful upload of {@link UploadType#IMAGE}.
 *
 * <p>The MAX platform returns a JSON response of the form:</p>
 * <pre>{@code
 * { "photos": { "<server-key>": { "token": "..." } } }
 * }</pre>
 *
 * <p>The {@code photos} map is normally a single entry but the API reserves the right to
 * return multiple sizes. Pass the full map verbatim to
 * {@code PhotoAttachmentRequestPayload(null, null, photos)} when constructing the
 * outgoing image attachment.</p>
 *
 * @param photos map of server-generated keys to per-size token references
 */
public record ImageUploadedInfo(
        Map<String, PhotoAttachmentRequestPayload.TokenRef> photos
) implements UploadedInfo {

    /**
     * Creates an ImageUploadedInfo with a defensive copy of the {@code photos} map.
     *
     * @param photos must not be {@code null}
     */
    public ImageUploadedInfo {
        Objects.requireNonNull(photos, "photos must not be null");
        photos = Map.copyOf(photos);
    }
}
