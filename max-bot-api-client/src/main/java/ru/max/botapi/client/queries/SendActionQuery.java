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
import ru.max.botapi.model.ActionRequestBody;
import ru.max.botapi.model.SimpleQueryResult;

/**
 * Query for {@code POST /chats/{chatId}/actions} — sends a typing or other action indicator
 * to a chat.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * api.sendAction(new ActionRequestBody(SenderAction.TYPING_ON), 123456789L).execute();
 * }</pre>
 */
public class SendActionQuery extends MaxQuery<SimpleQueryResult> {

    /**
     * Creates a SendActionQuery.
     *
     * @param client the MAX client to execute this query
     * @param body   the action request body; must not be {@code null}
     * @param chatId the chat identifier
     */
    public SendActionQuery(MaxClient client, ActionRequestBody body, long chatId) {
        super(client, "/chats/" + chatId + "/actions", HttpMethod.POST, SimpleQueryResult.class);
        this.body = Objects.requireNonNull(body, "body must not be null");
    }
}
