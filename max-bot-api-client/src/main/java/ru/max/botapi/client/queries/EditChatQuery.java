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
import ru.max.botapi.model.Chat;
import ru.max.botapi.model.ChatPatch;

/**
 * Query for {@code PATCH /chats/{chatId}} — edits chat properties such as title or description.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Chat updated = api.editChat(new ChatPatch("New Title", null, null), 123456789L).execute();
 * }</pre>
 */
public class EditChatQuery extends MaxQuery<Chat> {

    /**
     * Creates an EditChatQuery.
     *
     * @param client    the MAX client to execute this query
     * @param chatPatch the patch object with fields to update; must not be {@code null}
     * @param chatId    the chat identifier
     */
    public EditChatQuery(MaxClient client, ChatPatch chatPatch, long chatId) {
        super(client, "/chats/" + chatId, HttpMethod.PATCH, Chat.class);
        this.body = Objects.requireNonNull(chatPatch, "chatPatch must not be null");
    }
}
