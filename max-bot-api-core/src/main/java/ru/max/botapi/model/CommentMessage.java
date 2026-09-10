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
 * A comment on a channel post.
 *
 * <p>Returned by the {@code /messages/{messageId}/comments} methods. It differs from
 * {@link Message} in two ways: a comment has no attachments, and it cannot be forwarded.</p>
 *
 * @param sender    the author (null when the comment was posted as the channel)
 * @param recipient the recipient; for a comment this is the channel, and
 *                  {@link MessageRecipient#postId()} identifies the post being commented on
 * @param timestamp comment timestamp (epoch millis)
 * @param link      the comment this one replies to
 * @param body      the comment content
 */
public record CommentMessage(
        @Nullable User sender,
        MessageRecipient recipient,
        long timestamp,
        @Nullable CommentLinkedMessage link,
        CommentMessageBody body
) {

    /**
     * Creates a CommentMessage.
     *
     * @param recipient must not be {@code null}
     * @param body      must not be {@code null}
     */
    public CommentMessage {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(body, "body must not be null");
    }
}
