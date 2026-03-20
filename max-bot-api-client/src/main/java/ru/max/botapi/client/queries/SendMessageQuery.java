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
import ru.max.botapi.model.SendMessageResult;

/**
 * Query for {@code POST /messages} — sends a message to a user or chat.
 *
 * <p>Either {@code userId} or {@code chatId} must be provided as an optional parameter.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * SendMessageResult result = api.sendMessage(new NewMessageBody("Hello!"))
 *     .chatId(123456789L)
 *     .execute();
 * }</pre>
 */
public class SendMessageQuery extends MaxQuery<SendMessageResult> {

    /**
     * Creates a SendMessageQuery.
     *
     * @param client the MAX client to execute this query
     * @param body   the message body; must not be {@code null}
     */
    public SendMessageQuery(MaxClient client, NewMessageBody body) {
        super(client, "/messages", HttpMethod.POST, SendMessageResult.class);
        this.body = Objects.requireNonNull(body, "body must not be null");
    }

    /**
     * Sets the recipient user ID (for direct messages).
     *
     * @param userId the user identifier
     * @return this query for chaining
     */
    public SendMessageQuery userId(long userId) {
        queryParams.put("user_id", String.valueOf(userId));
        return this;
    }

    /**
     * Sets the recipient chat ID.
     *
     * @param chatId the chat identifier
     * @return this query for chaining
     */
    public SendMessageQuery chatId(long chatId) {
        queryParams.put("chat_id", String.valueOf(chatId));
        return this;
    }

    /**
     * Sets whether to disable link preview in the message.
     *
     * @param disable {@code true} to disable link preview
     * @return this query for chaining
     */
    public SendMessageQuery disableLinkPreview(boolean disable) {
        queryParams.put("disable_link_preview", String.valueOf(disable));
        return this;
    }
}
