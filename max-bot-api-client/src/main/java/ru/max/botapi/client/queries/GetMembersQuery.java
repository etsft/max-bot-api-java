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
import ru.max.botapi.model.ChatMembersList;

/**
 * Query for {@code GET /chats/{chatId}/members} — retrieves the member list of a chat.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ChatMembersList members = api.getMembers(123456789L).count(50).execute();
 * }</pre>
 */
public class GetMembersQuery extends MaxQuery<ChatMembersList> {

    /**
     * Creates a GetMembersQuery.
     *
     * @param client the MAX client to execute this query
     * @param chatId the chat identifier
     */
    public GetMembersQuery(MaxClient client, long chatId) {
        super(client, "/chats/" + chatId + "/members", HttpMethod.GET, ChatMembersList.class);
    }

    /**
     * Filters the result to only the specified user IDs.
     *
     * @param userIds the list of user IDs to retrieve; must not be {@code null}
     * @return this query for chaining
     */
    public GetMembersQuery userIds(List<Long> userIds) {
        Objects.requireNonNull(userIds, "userIds must not be null");
        queryParams.put("user_ids", userIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        return this;
    }

    /**
     * Sets the pagination marker from a previous response.
     *
     * @param marker the pagination marker
     * @return this query for chaining
     */
    public GetMembersQuery marker(long marker) {
        queryParams.put("marker", String.valueOf(marker));
        return this;
    }

    /**
     * Sets the maximum number of members to return.
     *
     * @param count the maximum count
     * @return this query for chaining
     */
    public GetMembersQuery count(int count) {
        queryParams.put("count", String.valueOf(count));
        return this;
    }
}
