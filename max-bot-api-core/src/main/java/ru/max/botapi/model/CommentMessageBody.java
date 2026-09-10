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
 * The body of a comment on a channel post.
 *
 * <p>Unlike {@link MessageBody} a comment carries no attachments, so this record has no
 * {@code attachments} component.</p>
 *
 * @param mid    comment identifier
 * @param seq    sequential number of the comment
 * @param text   text content of the comment
 * @param markup list of markup/formatting elements
 */
public record CommentMessageBody(
        String mid,
        long seq,
        @Nullable String text,
        @Nullable List<MarkupElement> markup
) {

    /**
     * Creates a CommentMessageBody.
     *
     * @param mid must not be {@code null}
     */
    public CommentMessageBody {
        Objects.requireNonNull(mid, "mid must not be null");
        markup = markup == null ? null : List.copyOf(markup);
    }
}
