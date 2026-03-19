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
 * The body/content of a message.
 *
 * @param mid         message identifier
 * @param seq         sequential number of the message
 * @param text        text content of the message
 * @param attachments list of attachments
 * @param markup      list of markup/formatting elements
 */
public record MessageBody(
        String mid,
        long seq,
        @Nullable String text,
        @Nullable List<Attachment> attachments,
        @Nullable List<MarkupElement> markup
) {

    /**
     * Creates a MessageBody.
     *
     * @param mid must not be {@code null}
     */
    public MessageBody {
        Objects.requireNonNull(mid, "mid must not be null");
        attachments = attachments == null ? null : List.copyOf(attachments);
        markup = markup == null ? null : List.copyOf(markup);
    }
}
