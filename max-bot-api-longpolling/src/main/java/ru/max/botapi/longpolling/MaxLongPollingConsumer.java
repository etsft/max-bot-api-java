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

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.client.queries.GetUpdatesQuery;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateList;

/**
 * Long-polling consumer for the MAX Bot API.
 *
 * <p>Uses a virtual thread to continuously poll the {@code GET /updates} endpoint,
 * dispatching each received {@link Update} to the provided {@link UpdateHandler}.
 * Employs exponential backoff on errors to avoid hammering the API on transient failures.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MaxBotAPI api = MaxBotAPI.create("my-token");
 * MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
 *     .api(api)
 *     .handler(update -> System.out.println("Got: " + update.updateType()))
 *     .build();
 * consumer.start();
 * // ... later:
 * consumer.stop();
 * }</pre>
 *
 * <p>This class implements {@link AutoCloseable}; calling {@link #close()} is equivalent
 * to calling {@link #stop()}.</p>
 */
public class MaxLongPollingConsumer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MaxLongPollingConsumer.class);

    private static final long MAX_BACKOFF_MS = 30_000L;

    private final MaxBotAPI api;
    private final UpdateHandler handler;
    private final Set<String> updateTypes;
    private final int pollTimeout;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private volatile boolean running;
    private volatile Long marker;

    /**
     * Handler interface for received updates and polling errors.
     */
    public interface UpdateHandler {

        /**
         * Called for each received {@link Update}.
         *
         * @param update the received update; never {@code null}
         */
        void onUpdate(Update update);

        /**
         * Called when an error occurs during polling or update handling.
         * The default implementation logs the error at WARN level.
         *
         * @param e the exception that was thrown
         */
        default void onError(Exception e) {
            LOG.warn("Error during long polling", e);
        }
    }

    private MaxLongPollingConsumer(Builder builder) {
        this.api = Objects.requireNonNull(builder.api, "api must not be null");
        this.handler = Objects.requireNonNull(builder.handler, "handler must not be null");
        this.updateTypes = builder.updateTypes == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(builder.updateTypes);
        this.pollTimeout = builder.pollTimeout;
    }

    /**
     * Creates a new {@link Builder} for {@code MaxLongPollingConsumer}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Starts long polling on a virtual thread.
     *
     * <p>Returns immediately; the polling loop runs in the background.
     * Call {@link #stop()} or {@link #close()} to terminate it.</p>
     *
     * @throws IllegalStateException if already running
     */
    public void start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("MaxLongPollingConsumer is already running");
        }
        running = true;
        Thread.startVirtualThread(this::pollLoop);
        LOG.info("Long polling started");
    }

    /**
     * Requests a graceful stop.
     *
     * <p>Sets the running flag to {@code false}. The current in-flight poll request
     * will complete before the loop exits.</p>
     */
    public void stop() {
        running = false;
        LOG.info("Long polling stop requested");
    }

    /**
     * Stops long polling. Equivalent to {@link #stop()}.
     */
    @Override
    public void close() {
        stop();
    }

    /**
     * Returns {@code true} if the consumer is currently active (polling or about to poll).
     *
     * @return {@code true} if running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Returns the current pagination marker, or {@code null} if no poll has completed yet.
     *
     * @return the last received marker, or {@code null}
     */
    public Long getMarker() {
        return marker;
    }

    private void pollLoop() {
        int errorStreak = 0;
        try {
            while (running) {
                try {
                    GetUpdatesQuery query = api.getUpdates().timeout(pollTimeout);
                    if (marker != null) {
                        query.marker(marker);
                    }
                    if (!updateTypes.isEmpty()) {
                        query.types(updateTypes);
                    }
                    UpdateList result = query.execute();
                    errorStreak = 0;
                    for (Update update : result.updates()) {
                        try {
                            handler.onUpdate(update);
                        } catch (Exception e) {
                            handler.onError(e);
                        }
                    }
                    if (result.marker() != null) {
                        marker = result.marker();
                    }
                } catch (Exception e) {
                    handler.onError(e);
                    errorStreak++;
                    sleepOnError(errorStreak);
                }
            }
        } finally {
            // Reset the started flag so the consumer can be restarted after stop().
            started.set(false);
            LOG.info("Long polling loop exited");
        }
    }

    /**
     * Sleeps with exponential backoff based on the number of consecutive errors.
     * Delay: 1s → 2s → 4s → 8s → 16s → 30s (capped).
     *
     * @param errorStreak number of consecutive errors so far
     */
    private void sleepOnError(int errorStreak) {
        // Exponential backoff: 1s, 2s, 4s, 8s, 16s → then capped at MAX_BACKOFF_MS (30s).
        // The inner Math.min caps the shift operand at 4 to prevent long overflow;
        // the outer Math.min applies the hard ceiling.
        long delayMs = Math.min(1000L * (1L << Math.min(errorStreak - 1, 4)), MAX_BACKOFF_MS);
        LOG.debug("Backing off for {}ms after {} consecutive error(s)", delayMs, errorStreak);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

    /**
     * Builder for {@link MaxLongPollingConsumer}.
     */
    public static final class Builder {

        private MaxBotAPI api;
        private UpdateHandler handler;
        private Set<String> updateTypes;
        private Integer pollTimeout;

        private Builder() {
        }

        /**
         * Sets the {@link MaxBotAPI} instance used for polling. Required.
         *
         * @param api the API facade; must not be {@code null}
         * @return this builder
         */
        public Builder api(MaxBotAPI api) {
            this.api = Objects.requireNonNull(api, "api must not be null");
            return this;
        }

        /**
         * Sets the {@link UpdateHandler} that receives updates and errors. Required.
         *
         * @param handler the update handler; must not be {@code null}
         * @return this builder
         */
        public Builder handler(UpdateHandler handler) {
            this.handler = Objects.requireNonNull(handler, "handler must not be null");
            return this;
        }

        /**
         * Filters polling to the specified update types.
         * When {@code null} or empty, all update types are received.
         *
         * @param updateTypes the set of update type strings to filter, may be {@code null}
         * @return this builder
         */
        public Builder updateTypes(Set<String> updateTypes) {
            this.updateTypes = updateTypes;
            return this;
        }

        /**
         * Sets the long-poll timeout in seconds.
         * If not set, defaults to {@code api.config().longPollTimeout()} (30 seconds by default).
         *
         * @param pollTimeout the timeout in seconds; must be positive
         * @return this builder
         * @throws IllegalArgumentException if {@code pollTimeout} is not positive
         */
        public Builder pollTimeout(int pollTimeout) {
            if (pollTimeout <= 0) {
                throw new IllegalArgumentException("pollTimeout must be positive, got: " + pollTimeout);
            }
            this.pollTimeout = pollTimeout;
            return this;
        }

        /**
         * Builds and returns a new {@link MaxLongPollingConsumer}.
         *
         * <p>If {@link #pollTimeout(int)} was not called, the timeout defaults to
         * {@code api.config().longPollTimeout()} (30 seconds by default).</p>
         *
         * @return a new consumer
         * @throws NullPointerException if {@code api} or {@code handler} was not set
         */
        public MaxLongPollingConsumer build() {
            Objects.requireNonNull(api, "api must not be null");
            if (pollTimeout == null) {
                pollTimeout = (int) api.config().longPollTimeout().toSeconds();
            }
            return new MaxLongPollingConsumer(this);
        }
    }
}
