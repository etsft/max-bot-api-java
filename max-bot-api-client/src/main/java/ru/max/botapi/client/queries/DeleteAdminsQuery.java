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
 * Query for {@code DELETE /chats/{chatId}/members/admins/{userId}} — demotes an admin to
 * regular member.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * api.deleteAdmins(123456789L, 999L).execute();
 * }</pre>
 */
public class DeleteAdminsQuery extends MaxQuery<SimpleQueryResult> {

    /**
     * Creates a DeleteAdminsQuery.
     *
     * @param client the MAX client to execute this query
     * @param chatId the chat identifier
     * @param userId the user identifier of the admin to demote
     */
    public DeleteAdminsQuery(MaxClient client, long chatId, long userId) {
        super(client, "/chats/" + chatId + "/members/admins/" + userId,
                HttpMethod.DELETE, SimpleQueryResult.class);
    }
}
