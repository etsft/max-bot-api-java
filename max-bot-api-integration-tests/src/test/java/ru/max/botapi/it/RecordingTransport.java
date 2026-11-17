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

package ru.max.botapi.it;

import java.util.concurrent.CompletableFuture;

import ru.max.botapi.client.MaxClientException;
import ru.max.botapi.client.MaxRequest;
import ru.max.botapi.client.MaxResponse;
import ru.max.botapi.client.MaxTransportClient;

/**
 * Transport decorator that remembers the HTTP status and raw body of the last response.
 *
 * <p>{@link RecordingSerializer} only sees bodies that reach deserialization, which a 4xx
 * response never does: {@code MaxClient} maps it onto {@code MaxApiException} first, so the
 * serializer would still be holding the previous call's body. The transport sees every
 * response, whatever its status, which is what a probe of the wire format needs.</p>
 *
 * <p>The recorded value is per-thread, like {@link RecordingSerializer}: long polling runs on
 * its own thread while the test thread makes ordinary calls. With retries enabled the last
 * attempt wins, which is the response the client actually acted on.</p>
 */
public final class RecordingTransport implements MaxTransportClient {

    private static final ThreadLocal<MaxResponse> LAST_RESPONSE = new ThreadLocal<>();

    private final MaxTransportClient delegate;

    /**
     * Wraps a real transport.
     *
     * @param delegate the transport that performs the requests
     */
    public RecordingTransport(MaxTransportClient delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns the HTTP status of the last response received on the current thread.
     *
     * @return the status, or {@code -1} if nothing was received since the last {@link #reset()}
     */
    public static int lastStatus() {
        MaxResponse response = LAST_RESPONSE.get();
        return response == null ? -1 : response.statusCode();
    }

    /**
     * Returns the raw body of the last response received on the current thread.
     *
     * @return the body, or {@code "<none recorded>"} if nothing was received since the last
     *         {@link #reset()}
     */
    public static String lastBody() {
        MaxResponse response = LAST_RESPONSE.get();
        return response == null ? "<none recorded>" : response.body();
    }

    /** Forgets the response recorded for the current thread. */
    public static void reset() {
        LAST_RESPONSE.remove();
    }

    /** {@inheritDoc} */
    @Override
    public MaxResponse execute(MaxRequest request) throws MaxClientException {
        MaxResponse response = delegate.execute(request);
        LAST_RESPONSE.set(response);
        return response;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The response is recorded on the thread that completes the future, not on the caller's;
     * {@code MaxClient} does not use this method, it runs {@link #execute} on its own executor.</p>
     */
    @Override
    public CompletableFuture<MaxResponse> executeAsync(MaxRequest request) {
        return delegate.executeAsync(request).thenApply(response -> {
            LAST_RESPONSE.set(response);
            return response;
        });
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        delegate.close();
    }
}
