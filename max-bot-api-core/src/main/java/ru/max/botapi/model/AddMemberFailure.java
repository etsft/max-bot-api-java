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

package ru.max.botapi.model;

import java.util.List;
import java.util.Objects;

/**
 * Per-error detail for a user that could not be added to a chat.
 *
 * <p>Returned as part of {@link AddMembersResult#failedUserDetails()} when
 * {@code POST /chats/{chatId}/members} fails for one or more users.</p>
 *
 * <p>Example error codes observed from the API:</p>
 * <ul>
 *   <li>{@code "add.participant.privacy"} — user's privacy settings prevent being added by a bot</li>
 * </ul>
 *
 * @param errorCode error code describing why the users could not be added
 * @param userIds   IDs of users affected by this error
 */
public record AddMemberFailure(
        String errorCode,
        List<Long> userIds
) {

    /**
     * Creates an AddMemberFailure.
     *
     * @param errorCode must not be {@code null}
     * @param userIds   must not be {@code null}
     */
    public AddMemberFailure {
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        Objects.requireNonNull(userIds, "userIds must not be null");
        userIds = List.copyOf(userIds);
    }
}
