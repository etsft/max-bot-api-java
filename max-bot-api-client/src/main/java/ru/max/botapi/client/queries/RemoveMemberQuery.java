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
import ru.max.botapi.model.SimpleQueryResult;

/**
 * Query for {@code DELETE /chats/{chatId}/members} — removes a member from a chat.
 *
 * <p>{@code userId} is a required parameter: the MAX API needs to know which member to remove.
 * It is therefore a constructor argument, not a fluent setter. The optional {@code block}
 * parameter can be set via {@link #block(boolean)}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * api.removeMember(123456789L, 999L).block(true).execute();
 * }</pre>
 */
public class RemoveMemberQuery extends MaxQuery<SimpleQueryResult> {

    /**
     * Creates a RemoveMemberQuery.
     *
     * @param client the MAX client to execute this query
     * @param chatId the chat identifier
     * @param userId the user identifier of the member to remove
     */
    public RemoveMemberQuery(MaxClient client, long chatId, long userId) {
        super(client, "/chats/" + chatId + "/members", HttpMethod.DELETE, SimpleQueryResult.class);
        queryParams.put("user_id", String.valueOf(userId));
    }

    /**
     * Sets whether to block the user after removal.
     *
     * @param block {@code true} to block the user
     * @return this query for chaining
     */
    public RemoveMemberQuery block(boolean block) {
        queryParams.put("block", String.valueOf(block));
        return this;
    }
}
