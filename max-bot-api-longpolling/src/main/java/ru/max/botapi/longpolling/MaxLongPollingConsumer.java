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
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.client.queries.GetUpdatesQuery;
import ru.max.botapi.core.UpdateHandler;
import ru.max.botapi.model.Nullable;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateList;
import ru.max.botapi.model.UpdateType;

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
 * <p>By default each update is handled on the polling thread, in order, and the next poll
 * starts only after the whole batch is handled, so a slow handler (an upload, a call to another
 * service) delays every later update. A {@link Builder#dispatchExecutor(Executor) dispatch
 * executor} runs the handler there instead and lets polling go on.</p>
 *
 * <p>This class implements {@link AutoCloseable}; calling {@link #close()} is equivalent
 * to calling {@link #stop()}.</p>
 */
public class MaxLongPollingConsumer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MaxLongPollingConsumer.class);

    private static final long MAX_BACKOFF_MS = 30_000L;

    /** Consecutive failures after which the loop is reported as stalled. */
    private static final int STALL_WARNING_THRESHOLD = 3;

    private static final Consumer<Exception> DEFAULT_ERROR_HANDLER =
            e -> LOG.warn("Error during long polling", e);

    private final MaxBotAPI api;
    private final UpdateHandler handler;
    private final Consumer<Exception> errorHandler;
    private final Set<UpdateType> updateTypes;
    private final int pollTimeout;
    private final @Nullable Executor dispatchExecutor;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private volatile boolean running;
    private volatile Long marker;

    private MaxLongPollingConsumer(Builder builder) {
        this.api = Objects.requireNonNull(builder.api, "api must not be null");
        this.handler = Objects.requireNonNull(builder.handler, "handler must not be null");
        this.errorHandler = builder.errorHandler != null ? builder.errorHandler : DEFAULT_ERROR_HANDLER;
        this.updateTypes = builder.updateTypes == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(builder.updateTypes.isEmpty()
                        ? EnumSet.noneOf(UpdateType.class)
                        : EnumSet.copyOf(builder.updateTypes));
        this.pollTimeout = builder.pollTimeout;
        this.dispatchExecutor = builder.dispatchExecutor;
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
                            if (dispatchExecutor == null) {
                                handler.onUpdate(update);
                            } else {
                                dispatchExecutor.execute(() -> handleDispatched(update));
                            }
                        } catch (Exception e) {
                            reportError(e);
                        }
                    }
                    if (result.marker() != null) {
                        marker = result.marker();
                    }
                } catch (Exception e) {
                    reportError(e);
                    errorStreak++;
                    if (errorStreak >= STALL_WARNING_THRESHOLD) {
                        LOG.warn("Long polling has failed {} times in a row; the marker is still {}. "
                                + "The same batch is likely being replayed.", errorStreak, marker);
                    }
                    sleepOnError(errorStreak);
                }
            }
        } catch (Throwable t) {
            // An Error (from the handler or the JVM) ends the loop. Log it loudly: otherwise
            // the bot silently stops receiving updates.
            LOG.error("Long polling stopped by an unrecoverable error; no further updates "
                    + "will be received", t);
            throw t;
        } finally {
            // Reflect the real state, and reset the started flag so the consumer can be
            // restarted after stop().
            running = false;
            started.set(false);
            LOG.info("Long polling loop exited");
        }
    }

    /**
     * Passes an exception to the user's error handler.
     *
     * <p>An exception thrown by the error handler itself is logged and swallowed: letting it
     * escape would skip the marker update (replaying the batch) or end the polling loop.</p>
     *
     * @param e the exception to report
     */
    private void handleDispatched(Update update) {
        try {
            handler.onUpdate(update);
        } catch (Exception e) {
            reportError(e);
        }
    }

    private void reportError(Exception e) {
        try {
            errorHandler.accept(e);
        } catch (Exception handlerFailure) {
            LOG.warn("The long polling error handler threw an exception; ignoring it",
                    handlerFailure);
        }
    }

    /**
     * Sleeps with exponential backoff based on the number of consecutive errors.
     * Delay: 1s → 2s → 4s → 8s → 16s → 30s (capped).
     *
     * @param errorStreak number of consecutive errors so far
     */
    private void sleepOnError(int errorStreak) {
        long delayMs = backoffMillis(errorStreak);
        LOG.debug("Backing off for {}ms after {} consecutive error(s)", delayMs, errorStreak);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

    /**
     * Returns the backoff delay after the given number of consecutive errors.
     * Delay: 1s → 2s → 4s → 8s → 16s → 30s (capped).
     *
     * @param errorStreak number of consecutive errors so far; at least 1
     * @return the delay in milliseconds
     */
    static long backoffMillis(int errorStreak) {
        // Shift by at most 5: 32s already exceeds the cap, and a larger shift would overflow.
        return Math.min(1000L << Math.min(errorStreak - 1, 5), MAX_BACKOFF_MS);
    }

    /**
     * Builder for {@link MaxLongPollingConsumer}.
     */
    public static final class Builder {

        private MaxBotAPI api;
        private UpdateHandler handler;
        private Consumer<Exception> errorHandler;
        private Set<UpdateType> updateTypes;
        private Integer pollTimeout;
        private @Nullable Executor dispatchExecutor;

        private Builder() {
        }

        /**
         * Sets the executor the handler runs on, so a slow handler does not hold up polling.
         *
         * <p>By default ({@code null}) updates are handled on the polling thread one after
         * another. With an executor each update is handed to it and polling continues at once,
         * so updates may be handled concurrently and out of order, and an update still being
         * handled when the application stops is lost: the marker has already moved past it.
         * Handler exceptions still go to the {@link #onError(Consumer) error handler}. The
         * caller owns the executor and shuts it down; {@code Executors.newVirtualThreadPerTaskExecutor()}
         * suits most handlers.</p>
         *
         * @param dispatchExecutor the executor, or {@code null} to handle updates on the
         *                         polling thread
         * @return this builder
         */
        public Builder dispatchExecutor(@Nullable Executor dispatchExecutor) {
            this.dispatchExecutor = dispatchExecutor;
            return this;
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
         * Sets the {@link UpdateHandler} that receives updates. Required.
         *
         * @param handler the update handler; must not be {@code null}
         * @return this builder
         */
        public Builder handler(UpdateHandler handler) {
            this.handler = Objects.requireNonNull(handler, "handler must not be null");
            return this;
        }

        /**
         * Sets an optional error handler invoked when polling or update handling throws
         * an exception. If not set, errors are logged at WARN level.
         *
         * @param errorHandler the error callback; must not be {@code null}
         * @return this builder
         */
        public Builder onError(Consumer<Exception> errorHandler) {
            this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler must not be null");
            return this;
        }

        /**
         * Filters polling to the specified update types.
         * When {@code null} or empty, all update types are received.
         *
         * @param updateTypes the set of update type constants to filter, may be {@code null}
         * @return this builder
         */
        public Builder types(Set<UpdateType> updateTypes) {
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
