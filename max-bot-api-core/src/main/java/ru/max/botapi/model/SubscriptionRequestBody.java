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

import java.util.List;
import java.util.Objects;

/**
 * Request body for creating a webhook subscription.
 *
 * @param url         webhook callback URL
 * @param updateTypes update types to subscribe to; {@code null} means all types
 * @param secret      optional secret for request verification
 */
public record SubscriptionRequestBody(
        String url,
        @Nullable List<UpdateType> updateTypes,
        @Nullable String secret
) {

    /**
     * Creates a SubscriptionRequestBody.
     *
     * @param url must not be {@code null}
     */
    public SubscriptionRequestBody {
        Objects.requireNonNull(url, "url must not be null");
        updateTypes = updateTypes == null ? null : List.copyOf(updateTypes);
    }
}
