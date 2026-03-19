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

package ru.max.botapi.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for MAX Bot API query builders.
 *
 * <p>Subclasses define the HTTP method, path, response type, and optional
 * query parameters and body. Calling {@link #execute()} sends the request
 * synchronously; {@link #enqueue()} sends it asynchronously.</p>
 *
 * @param <T> the response type
 */
public abstract class MaxQuery<T> {

    /** The client used to execute this query. */
    protected final MaxClient client;

    /** The API path. */
    protected final String path;

    /** The HTTP method. */
    protected final HttpMethod method;

    /** The response type class. */
    protected final Class<T> responseType;

    /** Mutable query parameters, snapshot-copied before execution. */
    protected final Map<String, String> queryParams = new LinkedHashMap<>();

    /** The request body object, serialized by the client before sending. */
    protected Object body;

    /**
     * Creates a MaxQuery.
     *
     * @param client       the client to execute this query
     * @param path         the API path
     * @param method       the HTTP method
     * @param responseType the response class
     */
    protected MaxQuery(MaxClient client, String path, HttpMethod method, Class<T> responseType) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.path = Objects.requireNonNull(path, "path must not be null");
        this.method = Objects.requireNonNull(method, "method must not be null");
        this.responseType = Objects.requireNonNull(responseType, "responseType must not be null");
    }

    /**
     * Executes this query synchronously.
     *
     * @return the deserialized response
     * @throws MaxApiException    if the API returns an error
     * @throws MaxClientException if a transport error occurs
     */
    public T execute() {
        MaxRequest request = buildRequest();
        return client.execute(request, responseType);
    }

    /**
     * Executes this query asynchronously.
     *
     * @return a future with the deserialized response
     */
    public CompletableFuture<T> enqueue() {
        MaxRequest request = buildRequest();
        return client.executeAsync(request, responseType);
    }

    /**
     * Returns the API path.
     *
     * @return path
     */
    public String path() {
        return path;
    }

    /**
     * Returns the HTTP method.
     *
     * @return method
     */
    public HttpMethod method() {
        return method;
    }

    /**
     * Returns an unmodifiable copy of the current query parameters.
     *
     * @return query parameters
     */
    public Map<String, String> queryParams() {
        return Map.copyOf(queryParams);
    }

    /**
     * Returns the body object.
     *
     * @return body, may be {@code null}
     */
    public Object body() {
        return body;
    }

    private MaxRequest buildRequest() {
        return new MaxRequest(
                method,
                path,
                Map.copyOf(queryParams),
                client.serialize(body),
                Map.of()
        );
    }
}
