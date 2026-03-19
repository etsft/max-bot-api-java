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
 * Paginated list of chat members.
 *
 * @param members list of members in the current page
 * @param marker  cursor for the next page, {@code null} if no more pages
 */
public record ChatMembersList(
        List<ChatMember> members,
        @Nullable Long marker
) {

    /**
     * Creates a ChatMembersList.
     *
     * @param members must not be {@code null}
     */
    public ChatMembersList {
        Objects.requireNonNull(members, "members must not be null");
        members = List.copyOf(members);
    }
}
