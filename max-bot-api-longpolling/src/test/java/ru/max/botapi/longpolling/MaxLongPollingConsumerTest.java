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

package ru.max.botapi.longpolling;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.max.botapi.client.JdkHttpMaxTransportClient;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.client.MaxClient;
import ru.max.botapi.client.MaxClientConfig;
import ru.max.botapi.jackson.JacksonMaxSerializer;
import ru.max.botapi.model.Update;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MaxLongPollingConsumer} using WireMock.
 */
@WireMockTest
class MaxLongPollingConsumerTest {

    private static final String TOKEN = "test-token";
    private static final String CONTENT_JSON = "application/json";

    private MaxBotAPI api;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        MaxClientConfig config = MaxClientConfig.builder()
                .baseUrl(wmInfo.getHttpBaseUrl())
                .enableRateLimiting(false)
                .maxRetries(0)
                .build();
        JacksonMaxSerializer serializer = new JacksonMaxSerializer();
        JdkHttpMaxTransportClient transport = new JdkHttpMaxTransportClient(TOKEN, config);
        MaxClient client = new MaxClient(transport, serializer, config);
        api = new MaxBotAPI(client);
    }

    @Test
    void pollsUpdatesAndDispatchesToHandler() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        List<Update> received = new CopyOnWriteArrayList<>();

        stubFor(get(urlPathEqualTo("/updates"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "updates": [
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
                                        "body": {"mid": "msg_lp_001", "seq": 1, "text": "Hello!"}
                                      }
                                    }
                                  ],
                                  "marker": null
                                }
                                """)));

        MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
                .api(api)
                .pollTimeout(1)
                .handler(new MaxLongPollingConsumer.UpdateHandler() {
                    @Override
                    public void onUpdate(Update update) {
                        received.add(update);
                        latch.countDown();
                    }
                })
                .build();

        consumer.start();

        boolean received1 = latch.await(5, TimeUnit.SECONDS);
        consumer.stop();

        assertThat(received1).isTrue();
        assertThat(received).isNotEmpty();
    }

    @Test
    void advancesMarkerBetweenPolls() throws Exception {
        CountDownLatch secondPollLatch = new CountDownLatch(1);
        AtomicInteger callCount = new AtomicInteger(0);

        stubFor(get(urlPathEqualTo("/updates"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "updates": [
                                    {
                                      "update_type": "bot_started",
                                      "timestamp": 1700001000000,
                                      "chat_id": 60001,
                                      "user": {
                                        "user_id": 99001,
                                        "name": "Alice",
                                        "is_bot": false,
                                        "last_activity_time": 1700000100000
                                      }
                                    }
                                  ],
                                  "marker": 9999
                                }
                                """)));

        MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
                .api(api)
                .pollTimeout(1)
                .handler(new MaxLongPollingConsumer.UpdateHandler() {
                    @Override
                    public void onUpdate(Update update) {
                        if (callCount.incrementAndGet() >= 2) {
                            secondPollLatch.countDown();
                        }
                    }
                })
                .build();

        consumer.start();

        boolean secondPollReceived = secondPollLatch.await(10, TimeUnit.SECONDS);
        consumer.stop();

        assertThat(secondPollReceived).isTrue();
        assertThat(consumer.getMarker()).isEqualTo(9999L);

        verify(moreThanOrExactly(2), getRequestedFor(urlPathEqualTo("/updates")));
    }

    @Test
    void stopsGracefully() throws Exception {
        stubFor(get(urlPathEqualTo("/updates"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "updates": [],
                                  "marker": null
                                }
                                """)));

        MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
                .api(api)
                .pollTimeout(1)
                .handler(update -> {
                })
                .build();

        consumer.start();
        assertThat(consumer.isRunning()).isTrue();

        Thread.sleep(200);
        consumer.stop();
        assertThat(consumer.isRunning()).isFalse();
    }

    @Test
    void recoversFromErrorWithBackoff() throws Exception {
        CountDownLatch errorLatch = new CountDownLatch(1);
        List<Exception> errors = new CopyOnWriteArrayList<>();

        stubFor(get(urlPathEqualTo("/updates"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"code\": \"server_error\", \"message\": \"Internal error\"}")));

        MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
                .api(api)
                .pollTimeout(1)
                .handler(new MaxLongPollingConsumer.UpdateHandler() {
                    @Override
                    public void onUpdate(Update update) {
                    }

                    @Override
                    public void onError(Exception e) {
                        errors.add(e);
                        errorLatch.countDown();
                    }
                })
                .build();

        consumer.start();

        boolean errorOccurred = errorLatch.await(5, TimeUnit.SECONDS);
        consumer.stop();

        assertThat(errorOccurred).isTrue();
        assertThat(errors).isNotEmpty();
    }

    @Test
    void filtersUpdateTypes() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        stubFor(get(urlPathEqualTo("/updates"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "updates": [
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
                                        "body": {"mid": "msg_flt_001", "seq": 1, "text": "Hi!"}
                                      }
                                    }
                                  ],
                                  "marker": null
                                }
                                """)));

        MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
                .api(api)
                .pollTimeout(1)
                .updateTypes(Set.of("message_created"))
                .handler(update -> latch.countDown())
                .build();

        consumer.start();
        boolean done = latch.await(5, TimeUnit.SECONDS);
        consumer.stop();

        assertThat(done).isTrue();
        verify(getRequestedFor(urlPathEqualTo("/updates"))
                .withQueryParam("types", containing("message_created")));
    }

    @Test
    void doubleStartThrowsIllegalStateException() throws Exception {
        stubFor(get(urlPathEqualTo("/updates"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "updates": [],
                                  "marker": null
                                }
                                """)));

        MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
                .api(api)
                .pollTimeout(1)
                .handler(update -> { })
                .build();

        consumer.start();
        try {
            assertThatThrownBy(consumer::start)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already running");
        } finally {
            consumer.stop();
        }
    }

    @Test
    void handlerExceptionDoesNotStopPolling() throws Exception {
        CountDownLatch secondUpdateLatch = new CountDownLatch(2);
        AtomicInteger updateCount = new AtomicInteger(0);

        stubFor(get(urlPathEqualTo("/updates"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "updates": [
                                    {
                                      "update_type": "message_created",
                                      "timestamp": 1700001000000,
                                      "message": {
                                        "sender": {
                                          "user_id": 99001,
                                          "name": "Bob",
                                          "is_bot": false,
                                          "last_activity_time": 1700000100000
                                        },
                                        "recipient": {"chat_id": 50001, "chat_type": "chat"},
                                        "timestamp": 1700001000000,
                                        "body": {"mid": "msg_err_001", "seq": 1, "text": "Test!"}
                                      }
                                    }
                                  ],
                                  "marker": null
                                }
                                """)));

        MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
                .api(api)
                .pollTimeout(1)
                .handler(update -> {
                    updateCount.incrementAndGet();
                    secondUpdateLatch.countDown();
                    throw new RuntimeException("Handler error");
                })
                .build();

        consumer.start();
        boolean reached = secondUpdateLatch.await(10, TimeUnit.SECONDS);
        consumer.stop();

        assertThat(reached).isTrue();
        assertThat(updateCount.get()).isGreaterThanOrEqualTo(2);
    }
}
