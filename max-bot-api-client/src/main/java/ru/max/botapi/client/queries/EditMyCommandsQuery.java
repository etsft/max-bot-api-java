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
import ru.max.botapi.model.BotCommandsPatch;
import ru.max.botapi.model.BotCommandsResult;

/**
 * Query for {@code PATCH /me/commands} — adds, changes or removes the bot's commands.
 *
 * <p>The commands are the hints the user sees after typing {@code /}. The list replaces the
 * previous one wholesale, so an empty list removes every command.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * api.editMyCommands(new BotCommandsPatch(List.of(
 *         new BotCommand("start", "Start the bot")))).execute();
 * }</pre>
 */
public class EditMyCommandsQuery extends MaxQuery<BotCommandsResult> {

    /**
     * Creates an EditMyCommandsQuery.
     *
     * @param client the MAX client to execute this query
     * @param patch  the commands to set; must not be {@code null}
     */
    public EditMyCommandsQuery(MaxClient client, BotCommandsPatch patch) {
        super(client, "/me/commands", HttpMethod.PATCH, BotCommandsResult.class);
        this.body = Objects.requireNonNull(patch, "patch must not be null");
    }
}
