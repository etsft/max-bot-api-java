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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.longpolling.MaxLongPollingConsumer;

/**
 * Spring {@link SmartLifecycle} that manages the {@link MaxLongPollingConsumer}.
 *
 * <p>Starts the long-polling consumer when the application context is ready
 * and stops it gracefully on shutdown. Using {@code SmartLifecycle} instead
 * of {@code ApplicationReadyEvent} ensures proper ordering and restart support
 * within the Spring lifecycle.</p>
 *
 * <p>The consumer runs on a virtual thread and does not block the main thread.
 * Errors during polling are handled by the consumer's built-in exponential
 * backoff mechanism.</p>
 */
public class MaxLongPollingLifecycle implements SmartLifecycle {

    private static final Logger LOG =
            LoggerFactory.getLogger(MaxLongPollingLifecycle.class);

    private final MaxBotAPI api;
    private final MaxLongPollingConsumer.UpdateHandler handler;
    private final MaxLongPollingProperties properties;
    private volatile MaxLongPollingConsumer consumer;
    private volatile boolean running;

    /**
     * Creates a new long-polling lifecycle manager.
     *
     * @param api        the MAX Bot API instance; must not be {@code null}
     * @param handler    the update handler; must not be {@code null}
     * @param properties the long-polling configuration; must not be {@code null}
     */
    public MaxLongPollingLifecycle(MaxBotAPI api,
                                   MaxLongPollingConsumer.UpdateHandler handler,
                                   MaxLongPollingProperties properties) {
        this.api = Objects.requireNonNull(api, "api must not be null");
        this.handler = Objects.requireNonNull(handler,
                "handler must not be null");
        this.properties = Objects.requireNonNull(properties,
                "properties must not be null");
    }

    /**
     * Starts the long-polling consumer.
     *
     * <p>Builds a new {@link MaxLongPollingConsumer} from the configured
     * properties and starts it on a virtual thread.</p>
     */
    @Override
    public void start() {
        MaxLongPollingConsumer.Builder builder = MaxLongPollingConsumer.builder()
                .api(api)
                .handler(handler);

        if (properties.getPollTimeout() != null) {
            builder.pollTimeout(properties.getPollTimeout());
        }

        if (!properties.getUpdateTypes().isEmpty()) {
            Set<String> types = new HashSet<>(properties.getUpdateTypes());
            builder.updateTypes(types);
        }

        consumer = builder.build();
        consumer.start();
        running = true;
        LOG.info("Long-polling consumer started via Spring lifecycle");
    }

    /**
     * Stops the long-polling consumer gracefully.
     */
    @Override
    public void stop() {
        doStop();
    }

    /**
     * Stops the long-polling consumer gracefully and signals the callback.
     *
     * @param callback the callback to signal when stop completes
     */
    @Override
    public void stop(Runnable callback) {
        doStop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Returns the phase for this lifecycle component.
     * Uses {@code Integer.MAX_VALUE - 1} to start late and stop early,
     * ensuring all other beans are ready before polling begins.
     *
     * @return the lifecycle phase
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 1;
    }

    private void doStop() {
        if (consumer != null) {
            consumer.stop();
            LOG.info("Long-polling consumer stopped via Spring lifecycle");
        }
        running = false;
    }
}
