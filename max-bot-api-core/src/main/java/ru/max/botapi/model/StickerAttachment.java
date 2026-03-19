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
 * Sticker attachment.
 *
 * @param payload sticker payload with URL and code
 * @param width   sticker width in pixels
 * @param height  sticker height in pixels
 */
public record StickerAttachment(
        StickerPayload payload,
        int width,
        int height
) implements Attachment {

    /** {@inheritDoc} */
    @Override
    public String type() {
        return "sticker";
    }

    /**
     * Creates a StickerAttachment.
     *
     * @param payload must not be {@code null}
     */
    public StickerAttachment {
        Objects.requireNonNull(payload, "payload must not be null");
    }

    /**
     * Payload for a sticker attachment.
     *
     * @param url  sticker image URL
     * @param code sticker pack code
     */
    public record StickerPayload(String url, String code) {

        /**
         * Creates a StickerPayload.
         *
         * @param url  must not be {@code null}
         * @param code must not be {@code null}
         */
        public StickerPayload {
            Objects.requireNonNull(url, "url must not be null");
            Objects.requireNonNull(code, "code must not be null");
        }
    }
}
