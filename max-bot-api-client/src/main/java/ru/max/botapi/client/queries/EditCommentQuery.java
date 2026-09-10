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
import ru.max.botapi.model.NewCommentBody;
import ru.max.botapi.model.SimpleQueryResult;

/**
 * Query for {@code PUT /messages/{messageId}/comments} — edits a comment on a channel post.
 *
 * <p>A bot may edit its own comments; editing a comment posted as the channel additionally
 * requires the {@link ru.max.botapi.model.ChatPermission#EDIT} right.</p>
 *
 * <p>{@code commentId} is required: the API cannot tell which comment to edit without it. It is
 * therefore a constructor argument, not a fluent setter.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * api.editComment(new NewCommentBody("Fixed"), "mid.abc123", "mid.def456").execute();
 * }</pre>
 */
public class EditCommentQuery extends MaxQuery<SimpleQueryResult> {

    /**
     * Creates an EditCommentQuery.
     *
     * @param client    the MAX client to execute this query
     * @param body      the updated comment; must not be {@code null}
     * @param messageId the post identifier (mid); must not be {@code null}
     * @param commentId the identifier of the comment to edit; must not be {@code null}
     */
    public EditCommentQuery(MaxClient client, NewCommentBody body, String messageId,
            String commentId) {
        super(client, "/messages/"
                        + Objects.requireNonNull(messageId, "messageId must not be null")
                        + "/comments",
                HttpMethod.PUT, SimpleQueryResult.class);
        this.body = Objects.requireNonNull(body, "body must not be null");
        queryParams.put("comment_id",
                Objects.requireNonNull(commentId, "commentId must not be null"));
    }
}
