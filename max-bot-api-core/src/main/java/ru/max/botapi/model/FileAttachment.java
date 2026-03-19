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
 * File attachment.
 *
 * @param payload  media payload with URL and token
 * @param filename original filename
 * @param size     file size in bytes
 */
public record FileAttachment(
        MediaPayload payload,
        String filename,
        long size
) implements Attachment {

    /** {@inheritDoc} */
    @Override
    public String type() {
        return "file";
    }

    /**
     * Creates a FileAttachment.
     *
     * @param payload  must not be {@code null}
     * @param filename must not be {@code null}
     */
    public FileAttachment {
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(filename, "filename must not be null");
    }
}
