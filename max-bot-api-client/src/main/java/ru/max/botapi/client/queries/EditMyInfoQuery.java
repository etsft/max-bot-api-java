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
import ru.max.botapi.model.BotInfo;
import ru.max.botapi.model.BotPatch;

/**
 * Query for {@code PATCH /me} — edits the current bot's profile information.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * BotInfo updated = api.editMyInfo(new BotPatch("New Bot Name", null, null)).execute();
 * }</pre>
 */
public class EditMyInfoQuery extends MaxQuery<BotInfo> {

    /**
     * Creates an EditMyInfoQuery.
     *
     * @param client   the MAX client to execute this query
     * @param botPatch the patch object containing fields to update; must not be {@code null}
     */
    public EditMyInfoQuery(MaxClient client, BotPatch botPatch) {
        super(client, "/me", HttpMethod.PATCH, BotInfo.class);
        this.body = Objects.requireNonNull(botPatch, "botPatch must not be null");
    }
}
