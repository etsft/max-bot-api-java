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
 * File attachment request for outgoing messages.
 *
 * @param payload media request payload with upload token
 */
public record FileAttachmentRequest(MediaRequestPayload payload) implements AttachmentRequest {

    /** {@inheritDoc} */
    @Override
    public String type() {
        return "file";
    }

    /**
     * Creates a FileAttachmentRequest.
     *
     * @param payload must not be {@code null}
     */
    public FileAttachmentRequest {
        Objects.requireNonNull(payload, "payload must not be null");
    }
}
