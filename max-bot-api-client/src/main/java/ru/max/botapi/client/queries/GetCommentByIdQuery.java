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

package ru.max.botapi.client.queries;

import java.util.Objects;

import ru.max.botapi.client.HttpMethod;
import ru.max.botapi.client.MaxClient;
import ru.max.botapi.client.MaxQuery;
import ru.max.botapi.model.CommentMessage;

/**
 * Query for {@code GET /messages/{messageId}/comments/{commentId}} — retrieves one comment.
 *
 * <p>The bot must be an administrator of the channel with the
 * {@link ru.max.botapi.model.ChatPermission#READ_ALL_MESSAGES} right.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * CommentMessage comment = api.getCommentById("mid.abc123", "mid.def456").execute();
 * }</pre>
 */
public class GetCommentByIdQuery extends MaxQuery<CommentMessage> {

    /**
     * Creates a GetCommentByIdQuery.
     *
     * @param client    the MAX client to execute this query
     * @param messageId the post identifier (mid); must not be {@code null}
     * @param commentId the comment identifier (mid); must not be {@code null}
     */
    public GetCommentByIdQuery(MaxClient client, String messageId, String commentId) {
        super(client, "/messages/"
                        + Objects.requireNonNull(messageId, "messageId must not be null")
                        + "/comments/"
                        + Objects.requireNonNull(commentId, "commentId must not be null"),
                HttpMethod.GET, CommentMessage.class);
    }
}
