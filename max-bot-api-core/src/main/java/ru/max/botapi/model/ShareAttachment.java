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

/**
 * Share/link-preview attachment.
 *
 * @param payload     share payload with URL and token
 * @param title       optional link title
 * @param description optional link description
 * @param imageUrl    optional preview image URL
 */
public record ShareAttachment(
        @Nullable SharePayload payload,
        @Nullable String title,
        @Nullable String description,
        @Nullable String imageUrl
) implements Attachment {

    /** {@inheritDoc} */
    @Override
    public String type() {
        return "share";
    }

    /**
     * Payload for a share attachment.
     *
     * @param url   shared URL
     * @param token share token
     */
    public record SharePayload(
            @Nullable String url,
            @Nullable String token
    ) {
    }
}
