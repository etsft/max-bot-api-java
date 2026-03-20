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
import ru.max.botapi.model.BotInfo;

/**
 * Query for {@code GET /me} — retrieves the current bot's profile information.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * BotInfo info = api.getMyInfo().execute();
 * }</pre>
 */
public class GetMyInfoQuery extends MaxQuery<BotInfo> {

    /**
     * Creates a GetMyInfoQuery.
     *
     * @param client the MAX client to execute this query
     */
    public GetMyInfoQuery(MaxClient client) {
        super(client, "/me", HttpMethod.GET, BotInfo.class);
    }
}
