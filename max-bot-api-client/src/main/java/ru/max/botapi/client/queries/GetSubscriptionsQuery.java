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
import ru.max.botapi.model.GetSubscriptionsResult;

/**
 * Query for {@code GET /subscriptions} — retrieves the list of active webhook subscriptions.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * GetSubscriptionsResult result = api.getSubscriptions().execute();
 * }</pre>
 */
public class GetSubscriptionsQuery extends MaxQuery<GetSubscriptionsResult> {

    /**
     * Creates a GetSubscriptionsQuery.
     *
     * @param client the MAX client to execute this query
     */
    public GetSubscriptionsQuery(MaxClient client) {
        super(client, "/subscriptions", HttpMethod.GET, GetSubscriptionsResult.class);
    }
}
