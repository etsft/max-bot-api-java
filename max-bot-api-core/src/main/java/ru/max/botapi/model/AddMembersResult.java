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
 * Result of {@code POST /chats/{chatId}/members}.
 *
 * <p>When all users are added successfully, {@link #success()} is {@code true} and
 * {@link #failedUserIds()} is empty. When some or all users could not be added —
 * for example due to privacy settings — {@link #success()} is {@code false} and
 * {@link #failedUserIds()} lists the affected user IDs with per-user error details
 * in {@link #failedUserDetails()}.</p>
 *
 * @param success           {@code true} if all users were added successfully
 * @param failedUserIds     IDs of users that could not be added; empty on full success
 * @param failedUserDetails per-error details for failed users; empty on full success
 */
public record AddMembersResult(
        boolean success,
        @Nullable List<Long> failedUserIds,
        @Nullable List<AddMemberFailure> failedUserDetails
) {

    /**
     * Creates an AddMembersResult.
     */
    public AddMembersResult {
        failedUserIds = failedUserIds == null ? null : List.copyOf(failedUserIds);
        failedUserDetails = failedUserDetails == null ? null : List.copyOf(failedUserDetails);
    }
}
