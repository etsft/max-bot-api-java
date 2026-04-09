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
import ru.max.botapi.model.AddMembersResult;
import ru.max.botapi.model.UserIdsList;

/**
 * Query for {@code POST /chats/{chatId}/members} — adds members to a chat.
 *
 * <p>When all users are added successfully, {@link AddMembersResult#success()} is {@code true}.
 * When some or all users could not be added (e.g. due to privacy settings),
 * {@link AddMembersResult#success()} is {@code false} and
 * {@link AddMembersResult#failedUserIds()} lists the affected users with
 * per-user error details in {@link AddMembersResult#failedUserDetails()}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * AddMembersResult result = api.addMembers(new UserIdsList(List.of(111L, 222L)), 123456789L).execute();
 * if (!result.success()) {
 *     result.failedUserDetails().forEach(f ->
 *         System.out.println(f.errorCode() + " for " + f.userIds()));
 * }
 * }</pre>
 */
public class AddMembersQuery extends MaxQuery<AddMembersResult> {

    /**
     * Creates an AddMembersQuery.
     *
     * @param client  the MAX client to execute this query
     * @param userIds the list of user IDs to add; must not be {@code null}
     * @param chatId  the chat identifier
     */
    public AddMembersQuery(MaxClient client, UserIdsList userIds, long chatId) {
        super(client, "/chats/" + chatId + "/members", HttpMethod.POST, AddMembersResult.class);
        this.body = Objects.requireNonNull(userIds, "userIds must not be null");
    }
}
