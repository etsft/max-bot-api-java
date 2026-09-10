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
 * The comment a reply was addressed to.
 *
 * <p>Comments cannot be forwarded, so unlike {@link LinkedMessage} the link type is always
 * {@link MessageLinkType#REPLY}.</p>
 *
 * @param type    type of link; always {@link MessageLinkType#REPLY} for comments
 * @param sender  the original sender (null when the comment was posted as the channel)
 * @param chatId  the channel the comment belongs to
 * @param message the body of the linked comment
 */
public record CommentLinkedMessage(
        MessageLinkType type,
        @Nullable User sender,
        @Nullable Long chatId,
        CommentMessageBody message
) {

    /**
     * Creates a CommentLinkedMessage.
     *
     * @param type    must not be {@code null}
     * @param message must not be {@code null}
     */
    public CommentLinkedMessage {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
