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

import java.util.List;
import java.util.Objects;

import ru.max.botapi.client.HttpMethod;
import ru.max.botapi.client.MaxClient;
import ru.max.botapi.client.MaxQuery;
import ru.max.botapi.model.CommentList;

/**
 * Query for {@code GET /messages/{messageId}/comments} — retrieves the comments on a post.
 *
 * <p>The bot must be an administrator of the channel with the
 * {@link ru.max.botapi.model.ChatPermission#READ_ALL_MESSAGES} right.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * CommentList comments = api.getComments("mid.abc123").count(20).execute();
 * }</pre>
 */
public class GetCommentsQuery extends MaxQuery<CommentList> {

    /**
     * Creates a GetCommentsQuery.
     *
     * @param client    the MAX client to execute this query
     * @param messageId the post identifier (mid); must not be {@code null}
     */
    public GetCommentsQuery(MaxClient client, String messageId) {
        super(client, "/messages/"
                        + Objects.requireNonNull(messageId, "messageId must not be null")
                        + "/comments",
                HttpMethod.GET, CommentList.class);
    }

    /**
     * Retrieves specific comments by their IDs, ignoring pagination.
     *
     * @param commentIds list of comment IDs; must not be {@code null}
     * @return this query for chaining
     */
    public GetCommentsQuery commentIds(List<String> commentIds) {
        Objects.requireNonNull(commentIds, "commentIds must not be null");
        queryParams.put("comment_ids", String.join(",", commentIds));
        return this;
    }

    /**
     * Requests the comments posted up to this time (Unix time in milliseconds).
     *
     * @param before the end timestamp
     * @return this query for chaining
     */
    public GetCommentsQuery before(long before) {
        queryParams.put("before", String.valueOf(before));
        return this;
    }

    /**
     * Requests the comments posted from this time onwards (Unix time in milliseconds).
     *
     * @param after the start timestamp
     * @return this query for chaining
     */
    public GetCommentsQuery after(long after) {
        queryParams.put("after", String.valueOf(after));
        return this;
    }

    /**
     * Sets how many comments to return, between 1 and 100. The API defaults to 50.
     *
     * @param count the maximum count
     * @return this query for chaining
     */
    public GetCommentsQuery count(int count) {
        queryParams.put("count", String.valueOf(count));
        return this;
    }
}
