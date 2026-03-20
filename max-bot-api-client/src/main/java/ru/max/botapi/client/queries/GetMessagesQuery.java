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
import java.util.stream.Collectors;

import ru.max.botapi.client.HttpMethod;
import ru.max.botapi.client.MaxClient;
import ru.max.botapi.client.MaxQuery;
import ru.max.botapi.model.MessageList;

/**
 * Query for {@code GET /messages} — retrieves messages, optionally filtered by various criteria.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MessageList messages = api.getMessages().chatId(123456789L).count(20).execute();
 * }</pre>
 */
public class GetMessagesQuery extends MaxQuery<MessageList> {

    /**
     * Creates a GetMessagesQuery.
     *
     * @param client the MAX client to execute this query
     */
    public GetMessagesQuery(MaxClient client) {
        super(client, "/messages", HttpMethod.GET, MessageList.class);
    }

    /**
     * Filters messages by chat ID.
     *
     * @param chatId the chat identifier
     * @return this query for chaining
     */
    public GetMessagesQuery chatId(long chatId) {
        queryParams.put("chat_id", String.valueOf(chatId));
        return this;
    }

    /**
     * Filters messages by user ID.
     *
     * @param userId the user identifier
     * @return this query for chaining
     */
    public GetMessagesQuery userId(long userId) {
        queryParams.put("user_id", String.valueOf(userId));
        return this;
    }

    /**
     * Retrieves specific messages by their IDs.
     *
     * @param messageIds list of message IDs; must not be {@code null}
     * @return this query for chaining
     */
    public GetMessagesQuery messageIds(List<String> messageIds) {
        Objects.requireNonNull(messageIds, "messageIds must not be null");
        queryParams.put("message_ids", String.join(",", messageIds));
        return this;
    }

    /**
     * Sets the start timestamp for the message range (Unix time in milliseconds).
     *
     * @param from the start timestamp
     * @return this query for chaining
     */
    public GetMessagesQuery from(long from) {
        queryParams.put("from", String.valueOf(from));
        return this;
    }

    /**
     * Sets the end timestamp for the message range (Unix time in milliseconds).
     *
     * @param to the end timestamp
     * @return this query for chaining
     */
    public GetMessagesQuery to(long to) {
        queryParams.put("to", String.valueOf(to));
        return this;
    }

    /**
     * Sets the maximum number of messages to return.
     *
     * @param count the maximum count
     * @return this query for chaining
     */
    public GetMessagesQuery count(int count) {
        queryParams.put("count", String.valueOf(count));
        return this;
    }
}
