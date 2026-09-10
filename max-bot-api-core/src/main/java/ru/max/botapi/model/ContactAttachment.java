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
     * <p>When the contact arrives because the user pressed a {@link RequestContactButton},
     * {@code hash} is present and lets the bot confirm that the phone number really is the
     * one attached to that user's MAX account: it equals
     * {@code HMAC-SHA256(access_token, vcfInfo)}, with the {@code \r\n} sequences of
     * {@code vcfInfo} turned into real line breaks before hashing. A contact shared any other
     * way carries no hash, and then the number cannot be verified.
     *
     * @param vcfInfo vCard string describing the contact
     * @param maxInfo MAX user reference
     * @param hash    HMAC of {@code vcfInfo}, present only for a shared contact
     */
    public record ContactPayload(
            @Nullable String vcfInfo,
            @Nullable User maxInfo,
            @Nullable String hash
    ) {
    }
}
