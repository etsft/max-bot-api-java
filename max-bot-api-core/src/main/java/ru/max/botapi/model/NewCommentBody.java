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

/**
 * Request body for posting or editing a comment on a channel post.
 *
 * <p>Comments support neither attachments nor forwarding, so unlike {@link NewMessageBody}
 * this record has no {@code attachments} component and {@code link} may only be a reply.
 * Mentions and hyperlinks are not rendered in comment text.</p>
 *
 * @param text   comment text, up to 4000 characters
 * @param link   the comment being replied to
 * @param format markup used in {@code text}
 */
public record NewCommentBody(
        @Nullable String text,
        @Nullable NewMessageLink link,
        @Nullable TextFormat format
) {

    /**
     * Creates a comment body carrying only text.
     *
     * @param text comment text
     */
    public NewCommentBody(@Nullable String text) {
        this(text, null, null);
    }
}
