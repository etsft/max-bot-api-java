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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.core.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MaxClient}.
 */
class MaxClientTest {

    private static final String USER_JSON = "{\"name\":\"Test\",\"user_id\":1}";

    /** Simple stub serializer for testing. */
    private final MaxSerializer stubSerializer = new MaxSerializer() {
        @Override
        public <T> String serialize(T object) {
            return "{\"test\":true}";
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T deserialize(String json, Class<T> type) {
            return (T) json;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T deserialize(String json, TypeReference<T> type) {
            return (T) json;
        }
    };

    private MaxTransportClient stubTransport(int statusCode, String body) {
        return stubTransport(statusCode, body, Map.of());
    }

    private MaxTransportClient stubTransport(int statusCode, String body,
                                             Map<String, List<String>> headers) {
        return new MaxTransportClient() {
            @Override
            public MaxResponse execute(MaxRequest request) {
                return new MaxResponse(statusCode, body, headers);
            }

            @Override
            public CompletableFuture<MaxResponse> executeAsync(MaxRequest request) {
                return CompletableFuture.completedFuture(execute(request));
            }

            @Override
            public void close() {
            }
        };
    }

    private MaxRequest simpleRequest() {
        return new MaxRequest(HttpMethod.GET, "/me", Map.of(), null, Map.of());
    }

    @Test
    void executeReturnsDeserializedResponse() {
        MaxClient client = new MaxClient(stubTransport(200, USER_JSON), stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).build());
        String result = client.execute(simpleRequest(), String.class);
        assertThat(result).isEqualTo(USER_JSON);
    }

    @Test
    void executeWithTypeReference() {
        MaxClient client = new MaxClient(stubTransport(200, USER_JSON), stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).build());
        String result = client.execute(simpleRequest(), new TypeReference<>() { });
        assertThat(result).isEqualTo(USER_JSON);
    }

    @Test
    void executeAsyncWithClass() throws Exception {
        MaxClient client = new MaxClient(stubTransport(200, USER_JSON), stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).build());
        String result = client.executeAsync(simpleRequest(), String.class).get();
        assertThat(result).isEqualTo(USER_JSON);
    }

    @Test
    void executeAsyncWithTypeReference() throws Exception {
        MaxClient client = new MaxClient(stubTransport(200, USER_JSON), stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).build());
        String result = client.executeAsync(simpleRequest(), new TypeReference<String>() { }).get();
        assertThat(result).isEqualTo(USER_JSON);
    }

    @Test
    void serializeNullReturnsNull() {
        MaxClient client = new MaxClient(stubTransport(200, ""), stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).build());
        assertThat(client.serialize(null)).isNull();
    }

    @Test
    void serializeObjectReturnsJson() {
        MaxClient client = new MaxClient(stubTransport(200, ""), stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).build());
        assertThat(client.serialize("test")).isEqualTo("{\"test\":true}");
    }

    @Test
    void throwsMaxApiExceptionOn400() {
        MaxClient client = new MaxClient(stubTransport(400, "Bad Request"), stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).build());
        assertThatThrownBy(() -> client.execute(simpleRequest(), String.class))
                .isInstanceOf(MaxApiException.class)
                .satisfies(ex -> {
                    MaxApiException apiEx = (MaxApiException) ex;
                    assertThat(apiEx.statusCode()).isEqualTo(400);
                });
    }

    @Test
    void throwsMaxApiExceptionOn401() {
        MaxClient client = new MaxClient(stubTransport(401, "Unauthorized"), stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).build());
        assertThatThrownBy(() -> client.execute(simpleRequest(), String.class))
                .isInstanceOf(MaxApiException.class);
    }

    @Test
    void throwsMaxApiExceptionOn404() {
        MaxClient client = new MaxClient(stubTransport(404, "Not Found"), stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).build());
        assertThatThrownBy(() -> client.execute(simpleRequest(), String.class))
                .isInstanceOf(MaxApiException.class);
    }

    @Test
    void throwsMaxRateLimitExceptionOn429() {
        MaxClient client = new MaxClient(
                stubTransport(429, "Too Many Requests",
                        Map.of("Retry-After", List.of("5"))),
                stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).maxRetries(0).build());
        assertThatThrownBy(() -> client.execute(simpleRequest(), String.class))
                .isInstanceOf(MaxRateLimitException.class)
                .satisfies(ex -> {
                    MaxRateLimitException rle = (MaxRateLimitException) ex;
                    assertThat(rle.statusCode()).isEqualTo(429);
                });
    }

    @Test
    void throwsMaxApiExceptionOn500() {
        MaxClient client = new MaxClient(stubTransport(500, "Server Error"), stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).build());
        assertThatThrownBy(() -> client.execute(simpleRequest(), String.class))
                .isInstanceOf(MaxApiException.class);
    }

    @Test
    void throwsMaxApiExceptionOnEmptyBody() {
        MaxClient client = new MaxClient(stubTransport(500, ""), stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).build());
        assertThatThrownBy(() -> client.execute(simpleRequest(), String.class))
                .isInstanceOf(MaxApiException.class);
    }

    @Test
    void retriesOn429ThenSucceeds() {
        AtomicInteger callCount = new AtomicInteger();
        MaxTransportClient transport = new MaxTransportClient() {
            @Override
            public MaxResponse execute(MaxRequest request) {
                if (callCount.incrementAndGet() == 1) {
                    return new MaxResponse(429, "Too Many Requests",
                            Map.of("Retry-After", List.of("0")));
                }
                return new MaxResponse(200, USER_JSON, Map.of());
            }

            @Override
            public CompletableFuture<MaxResponse> executeAsync(MaxRequest request) {
                return CompletableFuture.completedFuture(execute(request));
            }

            @Override
            public void close() {
            }
        };
        MaxClient client = new MaxClient(transport, stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).maxRetries(3).build());
        String result = client.execute(simpleRequest(), String.class);
        assertThat(result).isEqualTo(USER_JSON);
        assertThat(callCount.get()).isEqualTo(2);
    }

    @Test
    void retriesOn503ThenSucceeds() {
        AtomicInteger callCount = new AtomicInteger();
        MaxTransportClient transport = new MaxTransportClient() {
            @Override
            public MaxResponse execute(MaxRequest request) {
                if (callCount.incrementAndGet() == 1) {
                    return new MaxResponse(503, "Service Unavailable", Map.of());
                }
                return new MaxResponse(200, USER_JSON, Map.of());
            }

            @Override
            public CompletableFuture<MaxResponse> executeAsync(MaxRequest request) {
                return CompletableFuture.completedFuture(execute(request));
            }

            @Override
            public void close() {
            }
        };
        // Use zero base delay so the test does not sleep for 1 second
        RetryPolicy fastPolicy = new RetryPolicy(3, Duration.ZERO);
        MaxClient client = new MaxClient(transport, stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).maxRetries(3).build(),
                fastPolicy);
        String result = client.execute(simpleRequest(), String.class);
        assertThat(result).isEqualTo(USER_JSON);
        assertThat(callCount.get()).isEqualTo(2);
    }

    @Test
    void retryWithInvalidRetryAfterHeaderFallsBackToExponential() {
        AtomicInteger callCount = new AtomicInteger();
        MaxTransportClient transport = new MaxTransportClient() {
            @Override
            public MaxResponse execute(MaxRequest request) {
                if (callCount.incrementAndGet() == 1) {
                    return new MaxResponse(429, "Rate limited",
                            Map.of("Retry-After", List.of("not_a_number")));
                }
                return new MaxResponse(200, USER_JSON, Map.of());
            }

            @Override
            public CompletableFuture<MaxResponse> executeAsync(MaxRequest request) {
                return CompletableFuture.completedFuture(execute(request));
            }

            @Override
            public void close() {
            }
        };
        MaxClient client = new MaxClient(transport, stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).maxRetries(1).build());
        String result = client.execute(simpleRequest(), String.class);
        assertThat(result).isEqualTo(USER_JSON);
    }

    @Test
    void retryExhaustedThrows429() {
        MaxClient client = new MaxClient(
                stubTransport(429, "Too Many Requests", Map.of()),
                stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).maxRetries(0).build());
        assertThatThrownBy(() -> client.execute(simpleRequest(), String.class))
                .isInstanceOf(MaxRateLimitException.class);
    }

    @Test
    void rateLimiterApplied() {
        MaxClient client = new MaxClient(stubTransport(200, USER_JSON), stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(true)
                        .maxRequestsPerSecond(1000).build());
        String result = client.execute(simpleRequest(), String.class);
        assertThat(result).isEqualTo(USER_JSON);
    }

    @Test
    void nullTransportThrows() {
        assertThatThrownBy(() -> new MaxClient(null, stubSerializer, MaxClientConfig.defaults()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullSerializerThrows() {
        assertThatThrownBy(() -> new MaxClient(
                stubTransport(200, ""), null, MaxClientConfig.defaults()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullConfigThrows() {
        assertThatThrownBy(() -> new MaxClient(
                stubTransport(200, ""), stubSerializer, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void parsesJsonErrorBodyForErrorCodeAndMessage() {
        String errorBody = "{\"code\":\"bad_request\",\"message\":\"Invalid request body\"}";
        MaxClient client = new MaxClient(stubTransport(400, errorBody), stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).build());
        assertThatThrownBy(() -> client.execute(simpleRequest(), String.class))
                .isInstanceOf(MaxApiException.class)
                .satisfies(ex -> {
                    MaxApiException apiEx = (MaxApiException) ex;
                    assertThat(apiEx.statusCode()).isEqualTo(400);
                    assertThat(apiEx.errorCode()).isEqualTo("bad_request");
                    assertThat(apiEx.errorMessage()).isEqualTo("Invalid request body");
                });
    }

    @Test
    void throwsMaxMethodNotAllowedExceptionOn405() {
        String errorBody = "{\"code\":\"method_not_allowed\",\"message\":\"Method not allowed\"}";
        MaxClient client = new MaxClient(stubTransport(405, errorBody), stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).build());
        assertThatThrownBy(() -> client.execute(simpleRequest(), String.class))
                .isInstanceOf(MaxMethodNotAllowedException.class)
                .satisfies(ex -> {
                    MaxApiException apiEx = (MaxApiException) ex;
                    assertThat(apiEx.statusCode()).isEqualTo(405);
                    assertThat(apiEx.errorCode()).isEqualTo("method_not_allowed");
                });
    }

    @Test
    void throwsAttachmentNotReadyExceptionOn409WithAttachmentNotReadyCode() {
        String errorBody = "{\"code\":\"attachment_not_ready\",\"message\":\"Attachment not ready\"}";
        MaxClient client = new MaxClient(stubTransport(409, errorBody), stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).build());
        assertThatThrownBy(() -> client.execute(simpleRequest(), String.class))
                .isInstanceOf(AttachmentNotReadyException.class)
                .satisfies(ex -> {
                    MaxApiException apiEx = (MaxApiException) ex;
                    assertThat(apiEx.statusCode()).isEqualTo(409);
                    assertThat(apiEx.errorCode()).isEqualTo("attachment_not_ready");
                    assertThat(apiEx.errorMessage()).isEqualTo("Attachment not ready");
                });
    }

    @Test
    void throwsMaxApiExceptionOn409WithOtherCode() {
        String errorBody = "{\"code\":\"conflict\",\"message\":\"Resource conflict\"}";
        MaxClient client = new MaxClient(stubTransport(409, errorBody), stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).build());
        assertThatThrownBy(() -> client.execute(simpleRequest(), String.class))
                .isInstanceOf(MaxApiException.class)
                .isNotInstanceOf(AttachmentNotReadyException.class)
                .satisfies(ex -> {
                    MaxApiException apiEx = (MaxApiException) ex;
                    assertThat(apiEx.statusCode()).isEqualTo(409);
                });
    }

    @Test
    void rawBodyUsedAsMessageWhenNotJson() {
        MaxClient client = new MaxClient(stubTransport(500, "Internal Server Error"),
                stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).build());
        assertThatThrownBy(() -> client.execute(simpleRequest(), String.class))
                .isInstanceOf(MaxApiException.class)
                .satisfies(ex -> {
                    MaxApiException apiEx = (MaxApiException) ex;
                    assertThat(apiEx.errorMessage()).isEqualTo("Internal Server Error");
                    assertThat(apiEx.errorCode()).isNull();
                });
    }
}
