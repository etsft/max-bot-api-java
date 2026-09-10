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
import ru.max.botapi.model.SimpleQueryResult;

/**
 * Query for {@code DELETE /messages/{messageId}/comments} — deletes a comment on a post.
 *
 * <p>The bot must be an administrator of the channel with the
 * {@link ru.max.botapi.model.ChatPermission#READ_ALL_MESSAGES} and
 * {@link ru.max.botapi.model.ChatPermission#DELETE} rights. A deleted comment cannot be
 * restored.</p>
 *
 * <p>{@code commentId} is required: the API cannot tell which comment to delete without it. It
 * is therefore a constructor argument, not a fluent setter.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * api.deleteComment("mid.abc123", "mid.def456").execute();
 * }</pre>
 */
public class DeleteCommentQuery extends MaxQuery<SimpleQueryResult> {

    /**
     * Creates a DeleteCommentQuery.
     *
     * @param client    the MAX client to execute this query
     * @param messageId the post identifier (mid); must not be {@code null}
     * @param commentId the identifier of the comment to delete; must not be {@code null}
     */
    public DeleteCommentQuery(MaxClient client, String messageId, String commentId) {
        super(client, "/messages/"
                        + Objects.requireNonNull(messageId, "messageId must not be null")
                        + "/comments",
                HttpMethod.DELETE, SimpleQueryResult.class);
        queryParams.put("comment_id",
                Objects.requireNonNull(commentId, "commentId must not be null"));
    }
}
