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
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.SimpleQueryResult;

/**
 * Query for {@code PUT /messages} — edits a previously sent message.
 *
 * <p>{@code messageId} is a required parameter: there is no meaningful edit operation without
 * specifying which message to modify. The MAX API will reject the request with HTTP 400 if it is
 * absent. It is therefore a constructor argument, not a fluent setter.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * api.editMessage(new NewMessageBody("Updated text"), "mid.abc123").execute();
 * }</pre>
 */
public class EditMessageQuery extends MaxQuery<SimpleQueryResult> {

    /**
     * Creates an EditMessageQuery.
     *
     * @param client    the MAX client to execute this query
     * @param body      the updated message body; must not be {@code null}
     * @param messageId the ID of the message to edit; must not be {@code null}
     */
    public EditMessageQuery(MaxClient client, NewMessageBody body, String messageId) {
        super(client, "/messages", HttpMethod.PUT, SimpleQueryResult.class);
        this.body = Objects.requireNonNull(body, "body must not be null");
        queryParams.put("message_id",
                Objects.requireNonNull(messageId, "messageId must not be null"));
    }
}
