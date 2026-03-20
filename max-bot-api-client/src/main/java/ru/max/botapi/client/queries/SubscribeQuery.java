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
import ru.max.botapi.model.SimpleQueryResult;
import ru.max.botapi.model.SubscriptionRequestBody;

/**
 * Query for {@code POST /subscriptions} — registers a webhook URL for receiving updates.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * api.subscribe(new SubscriptionRequestBody("https://example.com/webhook", null, null)).execute();
 * }</pre>
 */
public class SubscribeQuery extends MaxQuery<SimpleQueryResult> {

    /**
     * Creates a SubscribeQuery.
     *
     * @param client the MAX client to execute this query
     * @param body   the subscription request body; must not be {@code null}
     */
    public SubscribeQuery(MaxClient client, SubscriptionRequestBody body) {
        super(client, "/subscriptions", HttpMethod.POST, SimpleQueryResult.class);
        this.body = Objects.requireNonNull(body, "body must not be null");
    }
}
