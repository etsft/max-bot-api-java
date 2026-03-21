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

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Default {@link MaxTransportClient} implementation using {@code java.net.http.HttpClient}
 * with virtual thread executor.
 */
public class JdkHttpMaxTransportClient implements MaxTransportClient {

    private final ExecutorService virtualThreadExecutor;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String accessToken;
    private final Duration requestTimeout;

    /**
     * Creates a transport client with default configuration.
     *
     * @param accessToken the bot access token
     */
    public JdkHttpMaxTransportClient(String accessToken) {
        this(accessToken, MaxClientConfig.defaults());
    }

    /**
     * Creates a transport client with the given configuration.
     *
     * @param accessToken the bot access token
     * @param config      client configuration
     */
    public JdkHttpMaxTransportClient(String accessToken, MaxClientConfig config) {
        this.accessToken = Objects.requireNonNull(accessToken, "accessToken must not be null");
        Objects.requireNonNull(config, "config must not be null");
        this.baseUrl = config.baseUrl();
        this.requestTimeout = config.requestTimeout();
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .executor(virtualThreadExecutor)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public MaxResponse execute(MaxRequest request) throws MaxClientException {
        try {
            HttpRequest httpRequest = buildHttpRequest(request);
            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());
            return new MaxResponse(response.statusCode(), response.body(),
                    response.headers().map());
        } catch (IOException e) {
            throw new MaxClientException("HTTP request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MaxClientException("HTTP request interrupted", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<MaxResponse> executeAsync(MaxRequest request) {
        HttpRequest httpRequest = buildHttpRequest(request);
        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> new MaxResponse(
                        response.statusCode(), response.body(),
                        response.headers().map()));
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        virtualThreadExecutor.shutdown();
    }

    private HttpRequest buildHttpRequest(MaxRequest request) {
        URI uri = buildUri(request.path(), request.queryParams());
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", accessToken)
                .timeout(requestTimeout);

        for (Map.Entry<String, String> header : request.headers().entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }

        HttpRequest.BodyPublisher bodyPublisher = request.body() != null
                ? HttpRequest.BodyPublishers.ofString(request.body())
                : HttpRequest.BodyPublishers.noBody();

        return switch (request.method()) {
            case GET -> builder.GET().build();
            case POST -> builder.header("Content-Type", "application/json")
                    .POST(bodyPublisher).build();
            case PUT -> builder.header("Content-Type", "application/json")
                    .PUT(bodyPublisher).build();
            case PATCH -> builder.header("Content-Type", "application/json")
                    .method("PATCH", bodyPublisher).build();
            case DELETE -> builder.DELETE().build();
        };
    }

    private URI buildUri(String path, Map<String, String> queryParams) {
        StringBuilder sb = new StringBuilder(baseUrl).append(path);
        if (!queryParams.isEmpty()) {
            sb.append('?');
            boolean first = true;
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (!first) {
                    sb.append('&');
                }
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                sb.append('=');
                sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                first = false;
            }
        }
        return URI.create(sb.toString());
    }
}
