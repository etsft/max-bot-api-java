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

import ru.max.botapi.client.HttpMethod;
import ru.max.botapi.client.MaxClient;
import ru.max.botapi.client.MaxQuery;
import ru.max.botapi.model.ChatList;

/**
 * Query for {@code GET /chats} — retrieves the list of chats the bot is a member of.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ChatList chats = api.getChats().count(10).execute();
 * }</pre>
 */
public class GetChatsQuery extends MaxQuery<ChatList> {

    /**
     * Creates a GetChatsQuery.
     *
     * @param client the MAX client to execute this query
     */
    public GetChatsQuery(MaxClient client) {
        super(client, "/chats", HttpMethod.GET, ChatList.class);
    }

    /**
     * Sets the maximum number of chats to return.
     *
     * @param count the maximum count (must be positive)
     * @return this query for chaining
     */
    public GetChatsQuery count(int count) {
        queryParams.put("count", String.valueOf(count));
        return this;
    }

    /**
     * Sets the pagination marker from a previous response.
     *
     * @param marker the pagination marker
     * @return this query for chaining
     */
    public GetChatsQuery marker(long marker) {
        queryParams.put("marker", String.valueOf(marker));
        return this;
    }
}
