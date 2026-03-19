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
 * Contact attachment.
 *
 * @param payload contact payload with VCF info and/or MAX user reference
 */
public record ContactAttachment(ContactPayload payload) implements Attachment {

    /** {@inheritDoc} */
    @Override
    public String type() {
        return "contact";
    }

    /**
     * Creates a ContactAttachment.
     *
     * @param payload must not be {@code null}
     */
    public ContactAttachment {
        Objects.requireNonNull(payload, "payload must not be null");
    }

    /**
     * Payload for a contact attachment.
     *
     * @param vcfInfo vCard string
     * @param tamInfo MAX Messenger user reference
     */
    public record ContactPayload(
            @Nullable String vcfInfo,
            @Nullable User tamInfo
    ) {
    }
}
