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
import ru.max.botapi.model.PinMessageBody;
import ru.max.botapi.model.SimpleQueryResult;

/**
 * Query for {@code PUT /chats/{chatId}/pin} — pins a message in a chat.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * api.pinMessage(new PinMessageBody("msgId123", false), 123456789L).execute();
 * }</pre>
 */
public class PinMessageQuery extends MaxQuery<SimpleQueryResult> {

    /**
     * Creates a PinMessageQuery.
     *
     * @param client the MAX client to execute this query
     * @param body   the pin message body; must not be {@code null}
     * @param chatId the chat identifier
     */
    public PinMessageQuery(MaxClient client, PinMessageBody body, long chatId) {
        super(client, "/chats/" + chatId + "/pin", HttpMethod.PUT, SimpleQueryResult.class);
        this.body = Objects.requireNonNull(body, "body must not be null");
    }
}
