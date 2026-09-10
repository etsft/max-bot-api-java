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
 * Update: a user or bot deleted a comment on a channel post.
 *
 * <p>A bot never receives this event for its own actions.</p>
 *
 * <p>The MAX documentation names this event and points at {@code message.recipient.post_id}
 * and {@code message.body.text}, but does not publish the full payload, so {@code message} is
 * modelled as optional. Unknown JSON properties are ignored, so a richer payload than this
 * deserializes without error.</p>
 *
 * @param timestamp event timestamp (epoch millis)
 * @param message   the comment the event is about; {@link MessageRecipient#postId()} identifies
 *                  the post it belongs to
 */
public record CommentRemovedUpdate(
        long timestamp,
        @Nullable CommentMessage message
) implements Update {

    /** {@inheritDoc} */
    @Override
    public String updateType() {
        return "comment_removed";
    }
}
