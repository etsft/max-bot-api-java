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

package ru.max.botapi.spring.longpolling;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.client.MaxClientConfig;
import ru.max.botapi.client.queries.GetUpdatesQuery;
import ru.max.botapi.core.PollingErrorHandler;
import ru.max.botapi.core.UpdateHandler;
import ru.max.botapi.longpolling.MaxLongPollingConsumer;
import ru.max.botapi.model.UpdateList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaxLongPollingLifecycleTest {

    @Test
    void startAndStop_managedByLifecycle() {
        MaxBotAPI api = mock(MaxBotAPI.class);
        MaxClientConfig config = MaxClientConfig.builder()
                .longPollTimeout(Duration.ofSeconds(1))
                .build();
        when(api.config()).thenReturn(config);

        GetUpdatesQuery query = mock(GetUpdatesQuery.class);
        when(api.getUpdates()).thenReturn(query);
        when(query.timeout(1)).thenReturn(query);
        UpdateList emptyResult = new UpdateList(List.of(), null);
        when(query.execute()).thenReturn(emptyResult);

        AtomicBoolean received = new AtomicBoolean(false);
        UpdateHandler handler = update -> received.set(true);

        MaxLongPollingProperties props = new MaxLongPollingProperties();
        MaxLongPollingLifecycle lifecycle =
                new MaxLongPollingLifecycle(api, handler, props, null);

        assertThat(lifecycle.isRunning()).isFalse();
        assertThat(lifecycle.getPhase()).isEqualTo(Integer.MAX_VALUE - 1);

        lifecycle.start();
        assertThat(lifecycle.isRunning()).isTrue();

        lifecycle.stop();
        assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    void startWithCustomPollTimeout() {
        MaxBotAPI api = mock(MaxBotAPI.class);
        MaxClientConfig config = MaxClientConfig.defaults();
        when(api.config()).thenReturn(config);

        GetUpdatesQuery query = mock(GetUpdatesQuery.class);
        when(api.getUpdates()).thenReturn(query);
        when(query.timeout(60)).thenReturn(query);
        UpdateList emptyResult = new UpdateList(List.of(), null);
        when(query.execute()).thenReturn(emptyResult);

        UpdateHandler handler = update -> { };

        MaxLongPollingProperties props = new MaxLongPollingProperties();
        props.setPollTimeout(60);

        MaxLongPollingLifecycle lifecycle =
                new MaxLongPollingLifecycle(api, handler, props, null);

        lifecycle.start();
        assertThat(lifecycle.isRunning()).isTrue();

        lifecycle.stop();
        assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    void startWithUpdateTypes() {
        MaxBotAPI api = mock(MaxBotAPI.class);
        MaxClientConfig config = MaxClientConfig.builder()
                .longPollTimeout(Duration.ofSeconds(1))
                .build();
        when(api.config()).thenReturn(config);

        GetUpdatesQuery query = mock(GetUpdatesQuery.class);
        when(api.getUpdates()).thenReturn(query);
        when(query.timeout(1)).thenReturn(query);
        when(query.types(org.mockito.ArgumentMatchers.anySet()))
                .thenReturn(query);
        UpdateList emptyResult = new UpdateList(List.of(), null);
        when(query.execute()).thenReturn(emptyResult);

        UpdateHandler handler = update -> { };

        MaxLongPollingProperties props = new MaxLongPollingProperties();
        props.setUpdateTypes(List.of("message_created", "message_callback"));

        MaxLongPollingLifecycle lifecycle =
                new MaxLongPollingLifecycle(api, handler, props, null);

        lifecycle.start();
        assertThat(lifecycle.isRunning()).isTrue();

        lifecycle.stop();
        assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    void stopWithCallback_invokesCallback() {
        MaxBotAPI api = mock(MaxBotAPI.class);
        MaxClientConfig config = MaxClientConfig.builder()
                .longPollTimeout(Duration.ofSeconds(1))
                .build();
        when(api.config()).thenReturn(config);

        GetUpdatesQuery query = mock(GetUpdatesQuery.class);
        when(api.getUpdates()).thenReturn(query);
        when(query.timeout(1)).thenReturn(query);
        UpdateList emptyResult = new UpdateList(List.of(), null);
        when(query.execute()).thenReturn(emptyResult);

        UpdateHandler handler = update -> { };

        MaxLongPollingProperties props = new MaxLongPollingProperties();
        MaxLongPollingLifecycle lifecycle =
                new MaxLongPollingLifecycle(api, handler, props, null);

        lifecycle.start();

        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        lifecycle.stop(() -> callbackInvoked.set(true));

        assertThat(callbackInvoked).isTrue();
        assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    void stopBeforeStart_doesNotThrow() {
        MaxBotAPI api = mock(MaxBotAPI.class);
        UpdateHandler handler = update -> { };
        MaxLongPollingProperties props = new MaxLongPollingProperties();

        MaxLongPollingLifecycle lifecycle =
                new MaxLongPollingLifecycle(api, handler, props, null);

        lifecycle.stop();
        assertThat(lifecycle.isRunning()).isFalse();
    }

    @Test
    void pollingErrorHandler_invokedOnException() throws InterruptedException {
        MaxBotAPI api = mock(MaxBotAPI.class);
        MaxClientConfig config = MaxClientConfig.builder()
                .longPollTimeout(Duration.ofSeconds(1))
                .build();
        when(api.config()).thenReturn(config);

        GetUpdatesQuery query = mock(GetUpdatesQuery.class);
        when(api.getUpdates()).thenReturn(query);
        when(query.timeout(1)).thenReturn(query);

        RuntimeException cause = new RuntimeException("network error");
        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<Exception> captured = new AtomicReference<>();

        when(query.execute()).thenThrow(cause);

        UpdateHandler handler = update -> { };
        PollingErrorHandler errorHandler = e -> {
            captured.set(e);
            errorLatch.countDown();
        };

        MaxLongPollingProperties props = new MaxLongPollingProperties();
        MaxLongPollingLifecycle lifecycle =
                new MaxLongPollingLifecycle(api, handler, props, errorHandler);

        lifecycle.start();
        assertThat(errorLatch.await(5, TimeUnit.SECONDS)).isTrue();
        lifecycle.stop();

        assertThat(captured.get()).isSameAs(cause);
    }

    @Test
    void noErrorHandler_defaultLoggingUsed() {
        // When errorHandler is null, the consumer uses default WARN logging.
        // Verify that no NullPointerException is thrown when polling fails.
        MaxBotAPI api = mock(MaxBotAPI.class);
        MaxClientConfig config = MaxClientConfig.builder()
                .longPollTimeout(Duration.ofSeconds(1))
                .build();
        when(api.config()).thenReturn(config);

        GetUpdatesQuery query = mock(GetUpdatesQuery.class);
        when(api.getUpdates()).thenReturn(query);
        when(query.timeout(1)).thenReturn(query);
        when(query.execute()).thenThrow(new RuntimeException("transient error"));

        UpdateHandler handler = update -> { };

        MaxLongPollingProperties props = new MaxLongPollingProperties();
        MaxLongPollingLifecycle lifecycle =
                new MaxLongPollingLifecycle(api, handler, props, null);

        lifecycle.start();
        assertThat(lifecycle.isRunning()).isTrue();
        lifecycle.stop();
        assertThat(lifecycle.isRunning()).isFalse();
    }
}
