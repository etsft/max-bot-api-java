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
import java.util.Set;
import java.util.stream.Collectors;

import ru.max.botapi.client.HttpMethod;
import ru.max.botapi.client.MaxClient;
import ru.max.botapi.client.MaxQuery;
import ru.max.botapi.model.UpdateList;

/**
 * Query for {@code GET /updates} — long-polls for incoming bot updates.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * UpdateList updates = api.getUpdates()
 *     .limit(100)
 *     .timeout(30)
 *     .marker(lastMarker)
 *     .execute();
 * }</pre>
 */
public class GetUpdatesQuery extends MaxQuery<UpdateList> {

    /**
     * Creates a GetUpdatesQuery.
     *
     * @param client the MAX client to execute this query
     */
    public GetUpdatesQuery(MaxClient client) {
        super(client, "/updates", HttpMethod.GET, UpdateList.class);
    }

    /**
     * Sets the maximum number of updates to retrieve.
     *
     * @param limit the update limit
     * @return this query for chaining
     */
    public GetUpdatesQuery limit(int limit) {
        queryParams.put("limit", String.valueOf(limit));
        return this;
    }

    /**
     * Sets the long-poll timeout in seconds.
     *
     * @param timeout the timeout in seconds
     * @return this query for chaining
     */
    public GetUpdatesQuery timeout(int timeout) {
        queryParams.put("timeout", String.valueOf(timeout));
        return this;
    }

    /**
     * Sets the pagination marker from a previous response to acknowledge processed updates.
     *
     * @param marker the pagination marker
     * @return this query for chaining
     */
    public GetUpdatesQuery marker(long marker) {
        queryParams.put("marker", String.valueOf(marker));
        return this;
    }

    /**
     * Filters updates to only the specified update types.
     *
     * @param types the set of update type strings; must not be {@code null}
     * @return this query for chaining
     */
    public GetUpdatesQuery types(Set<String> types) {
        Objects.requireNonNull(types, "types must not be null");
        if (!types.isEmpty()) {
            queryParams.put("types", String.join(",", types));
        }
        return this;
    }
}
