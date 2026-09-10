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
import ru.max.botapi.model.SendCommentResult;

/**
 * Query for {@code POST /messages/{messageId}/comments} — posts a comment on a channel post.
 *
 * <p>Comments must be enabled in the channel settings, and the bot must be an administrator
 * with the {@link ru.max.botapi.model.ChatPermission#READ_ALL_MESSAGES} and
 * {@link ru.max.botapi.model.ChatPermission#WRITE} rights.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * api.sendComment(new NewCommentBody("Thanks!"), "mid.abc123").execute();
 * }</pre>
 */
public class SendCommentQuery extends MaxQuery<SendCommentResult> {

    /**
     * Creates a SendCommentQuery.
     *
     * @param client    the MAX client to execute this query
     * @param body      the comment to post; must not be {@code null}
     * @param messageId the post identifier (mid); must not be {@code null}
     */
    public SendCommentQuery(MaxClient client, NewCommentBody body, String messageId) {
        super(client, "/messages/"
                        + Objects.requireNonNull(messageId, "messageId must not be null")
                        + "/comments",
                HttpMethod.POST, SendCommentResult.class);
        this.body = Objects.requireNonNull(body, "body must not be null");
    }
}
