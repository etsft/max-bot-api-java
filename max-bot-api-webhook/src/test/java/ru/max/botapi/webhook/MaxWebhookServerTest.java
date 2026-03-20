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

package ru.max.botapi.webhook;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.max.botapi.client.JdkHttpMaxTransportClient;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.client.MaxClient;
import ru.max.botapi.client.MaxClientConfig;
import ru.max.botapi.jackson.JacksonMaxSerializer;
import ru.max.botapi.model.Update;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MaxWebhookServer}.
 */
@WireMockTest
class MaxWebhookServerTest {

    private static final String CONTENT_JSON = "application/json";
    private static final String UPDATE_JSON = """
            {
              "update_type": "message_created",
              "timestamp": 1700001000000,
              "message": {
                "sender": {
                  "user_id": 99001,
                  "name": "John Doe",
                  "is_bot": false,
                  "last_activity_time": 1700000100000
                },
                "recipient": {"chat_id": 50001, "chat_type": "chat"},
                "timestamp": 1700001000000,
                "body": {"mid": "wh_msg_001", "seq": 1, "text": "Webhook test"}
              }
            }
            """;

    private int webhookPort;
    private String webhookBaseUrl;
    private MaxWebhookServer server;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() throws IOException {
        webhookPort = findFreePort();
        webhookBaseUrl = "http://localhost:" + webhookPort;
        httpClient = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void receivesUpdateFromPost() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CopyOnWriteArrayList<Update> received = new CopyOnWriteArrayList<>();

        server = MaxWebhookServer.builder()
                .handler(update -> {
                    received.add(update);
                    latch.countDown();
                })
                .serializer(new JacksonMaxSerializer())
                .port(webhookPort)
                .build();
        server.start();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookBaseUrl + "/webhook"))
                .header("Content-Type", CONTENT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(UPDATE_JSON))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        boolean handlerCalled = latch.await(5, TimeUnit.SECONDS);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(handlerCalled).isTrue();
        assertThat(received).hasSize(1);
    }

    @Test
    void rejects405OnGetRequest() throws Exception {
        server = MaxWebhookServer.builder()
                .handler(update -> {
                })
                .serializer(new JacksonMaxSerializer())
                .port(webhookPort)
                .build();
        server.start();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookBaseUrl + "/webhook"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(405);
    }

    @Test
    void rejectsInvalidSecret() throws Exception {
        server = MaxWebhookServer.builder()
                .handler(update -> {
                })
                .serializer(new JacksonMaxSerializer())
                .secret("my-secret")
                .port(webhookPort)
                .build();
        server.start();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookBaseUrl + "/webhook"))
                .header("Content-Type", CONTENT_JSON)
                .header("X-Max-Bot-Api-Secret", "wrong-secret")
                .POST(HttpRequest.BodyPublishers.ofString(UPDATE_JSON))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void acceptsValidSecret() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        server = MaxWebhookServer.builder()
                .handler(update -> latch.countDown())
                .serializer(new JacksonMaxSerializer())
                .secret("my-secret")
                .port(webhookPort)
                .build();
        server.start();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookBaseUrl + "/webhook"))
                .header("Content-Type", CONTENT_JSON)
                .header("X-Max-Bot-Api-Secret", "my-secret")
                .POST(HttpRequest.BodyPublishers.ofString(UPDATE_JSON))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        boolean called = latch.await(5, TimeUnit.SECONDS);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(called).isTrue();
    }

    @Test
    void returnsOkOnHandlerException() throws Exception {
        server = MaxWebhookServer.builder()
                .handler(update -> {
                    throw new RuntimeException("Handler error");
                })
                .serializer(new JacksonMaxSerializer())
                .port(webhookPort)
                .build();
        server.start();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookBaseUrl + "/webhook"))
                .header("Content-Type", CONTENT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(UPDATE_JSON))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void registerWebhookCallsSubscribeApi(WireMockRuntimeInfo wmInfo) throws Exception {
        stubFor(post(urlPathEqualTo("/subscriptions"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        MaxBotAPI api = createApi(wmInfo);

        server = MaxWebhookServer.builder()
                .handler(update -> {
                })
                .serializer(new JacksonMaxSerializer())
                .port(webhookPort)
                .build();
        server.start();

        server.register(api, "https://example.com/webhook", null);

        verify(postRequestedFor(urlPathEqualTo("/subscriptions")));
    }

    @Test
    void unregisterWebhookCallsUnsubscribeApi(WireMockRuntimeInfo wmInfo) throws Exception {
        stubFor(delete(urlPathEqualTo("/subscriptions"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        MaxBotAPI api = createApi(wmInfo);

        server = MaxWebhookServer.builder()
                .handler(update -> {
                })
                .serializer(new JacksonMaxSerializer())
                .port(webhookPort)
                .build();
        server.start();

        server.unregister(api, "https://example.com/webhook");

        verify(com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor(
                urlPathEqualTo("/subscriptions")));
    }

    private MaxBotAPI createApi(WireMockRuntimeInfo wmInfo) {
        MaxClientConfig config = MaxClientConfig.builder()
                .baseUrl(wmInfo.getHttpBaseUrl())
                .enableRateLimiting(false)
                .maxRetries(0)
                .build();
        JacksonMaxSerializer serializer = new JacksonMaxSerializer();
        JdkHttpMaxTransportClient transport = new JdkHttpMaxTransportClient("test-token", config);
        MaxClient client = new MaxClient(transport, serializer, config);
        return new MaxBotAPI(client);
    }

    @Test
    void getPortAndPathReflectConfiguration() throws Exception {
        server = MaxWebhookServer.builder()
                .handler(update -> {
                })
                .serializer(new JacksonMaxSerializer())
                .port(webhookPort)
                .path("/mypath")
                .build();
        server.start();

        assertThat(server.getPort()).isEqualTo(webhookPort);
        assertThat(server.getPath()).isEqualTo("/mypath");
    }

    @Test
    void builderPathValidationRejectsPathWithoutLeadingSlash() {
        assertThatThrownBy(() -> MaxWebhookServer.builder()
                .handler(update -> {
                })
                .serializer(new JacksonMaxSerializer())
                .path("noSlash")
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingSecretHeaderReturns401WhenSecretConfigured() throws Exception {
        server = MaxWebhookServer.builder()
                .handler(update -> {
                })
                .serializer(new JacksonMaxSerializer())
                .secret("my-secret")
                .port(webhookPort)
                .build();
        server.start();

        // send request without any secret header at all
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookBaseUrl + "/webhook"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(UPDATE_JSON))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void timingSafeComparisonRejectsNullSecretHeader() throws Exception {
        // Ensure that a request with no secret header returns 401 (null-safe path through
        // constantTimeEquals) and does not throw NullPointerException.
        server = MaxWebhookServer.builder()
                .handler(update -> { })
                .serializer(new JacksonMaxSerializer())
                .secret("safe-secret")
                .port(webhookPort)
                .build();
        server.start();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookBaseUrl + "/webhook"))
                .header("Content-Type", "application/json")
                // No X-Max-Bot-Api-Secret header — headerSecret will be null
                .POST(HttpRequest.BodyPublishers.ofString(UPDATE_JSON))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
