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
import ru.max.botapi.model.Message;

/**
 * Query for {@code GET /messages/{messageId}} — retrieves a specific message by its ID.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Message message = api.getMessageById("mid.abc123").execute();
 * }</pre>
 */
public class GetMessageByIdQuery extends MaxQuery<Message> {

    /**
     * Creates a GetMessageByIdQuery.
     *
     * @param client    the MAX client to execute this query
     * @param messageId the message identifier; must not be {@code null}
     */
    public GetMessageByIdQuery(MaxClient client, String messageId) {
        super(client, "/messages/" + Objects.requireNonNull(messageId, "messageId must not be null"),
                HttpMethod.GET, Message.class);
    }
}
