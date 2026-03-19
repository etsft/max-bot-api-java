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

/**
 * Request body for sending ({@code POST /messages}) or editing ({@code PUT /messages}) a message.
 *
 * @param text        text content of the message
 * @param attachments list of attachment requests
 * @param link        link to another message (forward/reply)
 * @param notify      whether to notify recipients
 * @param format      text format (markdown or HTML)
 */
public record NewMessageBody(
        @Nullable String text,
        @Nullable List<AttachmentRequest> attachments,
        @Nullable NewMessageLink link,
        // TODO: Jackson adapter must map this field to "notify" in JSON
        //       via @JsonProperty("notify") or custom naming strategy
        @Nullable Boolean notifyRecipients,
        @Nullable TextFormat format
) {

    /**
     * Compact constructor for defensive copies.
     */
    public NewMessageBody {
        attachments = attachments == null ? null : List.copyOf(attachments);
    }
}
