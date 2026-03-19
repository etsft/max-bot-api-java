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

package ru.max.botapi.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson mix-in that maps the {@code notifyRecipients} Java field to {@code "notify"} in JSON.
 *
 * <p>The Java field is named {@code notifyRecipients} because {@code notify} conflicts with
 * {@link Object#notify()}. This mix-in ensures the JSON wire format uses the correct
 * {@code "notify"} key expected by the MAX Bot API.</p>
 */
abstract class NotifyFieldMixIn {

    /**
     * Maps to {@code "notify"} in JSON.
     *
     * @return whether to notify recipients
     */
    @JsonProperty("notify")
    abstract Boolean notifyRecipients();
}
