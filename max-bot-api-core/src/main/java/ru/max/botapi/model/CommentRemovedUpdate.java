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
 * Update: a user or bot deleted a comment on a channel post.
 *
 * <p>A bot never receives this event for its own actions.</p>
 *
 * <p>Unlike {@link CommentCreatedUpdate} and {@link CommentEditedUpdate}, this event carries
 * no comment object: the comment is gone, and only its identifiers arrive.</p>
 *
 * @param timestamp event timestamp (epoch millis)
 * @param messageId ID of the removed comment
 * @param chatId    chat the comment was removed from
 * @param userId    user who removed the comment
 * @param postId    ID of the channel post the comment belonged to
 */
public record CommentRemovedUpdate(
        long timestamp,
        String messageId,
        long chatId,
        long userId,
        @Nullable String postId
) implements Update {

    /** {@inheritDoc} */
    @Override
    public String updateType() {
        return "comment_removed";
    }

    /**
     * Creates a CommentRemovedUpdate.
     *
     * @param messageId must not be {@code null}
     */
    public CommentRemovedUpdate {
        Objects.requireNonNull(messageId, "messageId must not be null");
    }
}
