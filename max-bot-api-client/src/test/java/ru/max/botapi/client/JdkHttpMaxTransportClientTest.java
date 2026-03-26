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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link JdkHttpMaxTransportClient} using a local HTTP server.
 */
class JdkHttpMaxTransportClientTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/me", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            String method = exchange.getRequestMethod();
            String response = "{\"method\":\"" + method + "\""
                    + ",\"auth\":\"" + auth + "\""
                    + (contentType != null ? ",\"content_type\":\"" + contentType + "\"" : "")
                    + "}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.createContext("/echo", exchange -> {
            byte[] reqBody = exchange.getRequestBody().readAllBytes();
            String response = new String(reqBody, StandardCharsets.UTF_8);
            if (response.isEmpty()) {
                response = "{}";
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private JdkHttpMaxTransportClient createClient(String token) {
        MaxClientConfig config = MaxClientConfig.builder()
                .baseUrl(baseUrl)
                .connectTimeout(Duration.ofSeconds(5))
                .requestTimeout(Duration.ofSeconds(5))
                .longPollTimeout(Duration.ofSeconds(4))
                .build();
        return new JdkHttpMaxTransportClient(token, config);
    }

    @Test
    void executeGet() {
        try (JdkHttpMaxTransportClient client = createClient("test-token")) {
            MaxRequest request = new MaxRequest(HttpMethod.GET, "/me", Map.of(), null, Map.of());
            MaxResponse response = client.execute(request);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"method\":\"GET\"");
            assertThat(response.body()).contains("\"auth\":\"test-token\"");
        }
    }

    @Test
    void executePost() {
        try (JdkHttpMaxTransportClient client = createClient("my-token")) {
            MaxRequest request = new MaxRequest(HttpMethod.POST, "/echo",
                    Map.of(), "{\"text\":\"hello\"}", Map.of());
            MaxResponse response = client.execute(request);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("hello");
        }
    }

    @Test
    void executePut() {
        try (JdkHttpMaxTransportClient client = createClient("my-token")) {
            MaxRequest request = new MaxRequest(HttpMethod.PUT, "/me",
                    Map.of(), "{}", Map.of());
            MaxResponse response = client.execute(request);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"method\":\"PUT\"");
            assertThat(response.body()).contains("\"content_type\":\"application/json\"");
        }
    }

    @Test
    void executePatch() {
        try (JdkHttpMaxTransportClient client = createClient("my-token")) {
            MaxRequest request = new MaxRequest(HttpMethod.PATCH, "/me",
                    Map.of(), "{}", Map.of());
            MaxResponse response = client.execute(request);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"method\":\"PATCH\"");
        }
    }

    @Test
    void executeDelete() {
        try (JdkHttpMaxTransportClient client = createClient("my-token")) {
            MaxRequest request = new MaxRequest(HttpMethod.DELETE, "/me",
                    Map.of(), null, Map.of());
            MaxResponse response = client.execute(request);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"method\":\"DELETE\"");
        }
    }

    @Test
    void executeWithQueryParams() {
        try (JdkHttpMaxTransportClient client = createClient("my-token")) {
            MaxRequest request = new MaxRequest(HttpMethod.GET, "/me",
                    Map.of("key", "value", "num", "42"), null, Map.of());
            MaxResponse response = client.execute(request);
            assertThat(response.statusCode()).isEqualTo(200);
        }
    }

    @Test
    void executeWithCustomHeaders() {
        try (JdkHttpMaxTransportClient client = createClient("my-token")) {
            MaxRequest request = new MaxRequest(HttpMethod.GET, "/me",
                    Map.of(), null, Map.of("X-Custom", "custom-value"));
            MaxResponse response = client.execute(request);
            assertThat(response.statusCode()).isEqualTo(200);
        }
    }

    @Test
    void executeAsync() throws Exception {
        try (JdkHttpMaxTransportClient client = createClient("my-token")) {
            MaxRequest request = new MaxRequest(HttpMethod.GET, "/me", Map.of(), null, Map.of());
            CompletableFuture<MaxResponse> future = client.executeAsync(request);
            MaxResponse response = future.get();
            assertThat(response.statusCode()).isEqualTo(200);
        }
    }

    @Test
    void executeThrowsOnConnectionFailure() {
        MaxClientConfig config = MaxClientConfig.builder()
                .baseUrl("http://localhost:1") // Invalid port
                .connectTimeout(Duration.ofMillis(100))
                .requestTimeout(Duration.ofMillis(200))
                .longPollTimeout(Duration.ofMillis(100))
                .build();
        try (JdkHttpMaxTransportClient client =
                     new JdkHttpMaxTransportClient("token", config)) {
            MaxRequest request = new MaxRequest(
                    HttpMethod.GET, "/me", Map.of(), null, Map.of());
            assertThatThrownBy(() -> client.execute(request))
                    .isInstanceOf(MaxClientException.class);
        }
    }

    @Test
    void constructionWithDefaultConfig() {
        try (JdkHttpMaxTransportClient transport =
                     new JdkHttpMaxTransportClient("test-token")) {
            assertThat(transport).isNotNull();
        }
    }

    @Test
    void nullAccessTokenThrows() {
        assertThatThrownBy(() -> new JdkHttpMaxTransportClient(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullConfigThrows() {
        assertThatThrownBy(() -> new JdkHttpMaxTransportClient("token", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void executePostWithNullBody() {
        try (JdkHttpMaxTransportClient client = createClient("my-token")) {
            MaxRequest request = new MaxRequest(HttpMethod.POST, "/echo",
                    Map.of(), null, Map.of());
            MaxResponse response = client.execute(request);
            assertThat(response.statusCode()).isEqualTo(200);
        }
    }
}
