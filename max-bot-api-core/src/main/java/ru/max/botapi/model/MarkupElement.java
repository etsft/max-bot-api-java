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
 * A markup/formatting element within a message body.
 *
 * <p>Most markup types carry nothing beyond their range. Two carry a target as well:
 * {@code link} sets {@code url}, and {@code user_mention} sets {@code userLink} for a user
 * with a public name or {@code userId} for one without.</p>
 *
 * @param type     the type of markup (e.g., "strong", "emphasized", "link", "user_mention")
 * @param from     starting character offset
 * @param length   number of characters covered by this markup
 * @param url      target of a {@code link}; absent for other types
 * @param userLink {@code @username} of the user a {@code user_mention} refers to
 * @param userId   ID of the user a {@code user_mention} refers to, when that user has no
 *                 public name
 */
public record MarkupElement(
        String type,
        int from,
        int length,
        @Nullable String url,
        @Nullable String userLink,
        @Nullable Long userId
) {

    /**
     * Creates a MarkupElement.
     *
     * @param type must not be {@code null}
     */
    public MarkupElement {
        Objects.requireNonNull(type, "type must not be null");
    }
}
