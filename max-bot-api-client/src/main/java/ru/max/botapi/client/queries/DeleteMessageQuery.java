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
 * Query for {@code DELETE /messages} — deletes a message by its ID.
 *
 * <p>The message ID is a required query parameter passed in the constructor.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * api.deleteMessage("mid.abc123").execute();
 * }</pre>
 */
public class DeleteMessageQuery extends MaxQuery<SimpleQueryResult> {

    /**
     * Creates a DeleteMessageQuery.
     *
     * @param client    the MAX client to execute this query
     * @param messageId the ID of the message to delete; must not be {@code null}
     */
    public DeleteMessageQuery(MaxClient client, String messageId) {
        super(client, "/messages", HttpMethod.DELETE, SimpleQueryResult.class);
        queryParams.put("message_id",
                Objects.requireNonNull(messageId, "messageId must not be null"));
    }
}
