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

import java.time.Duration;

import ru.max.botapi.client.JdkHttpMaxTransportClient;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.client.MaxClient;
import ru.max.botapi.client.MaxClientConfig;
import ru.max.botapi.core.MaxSerializer;

/**
 * A live {@link MaxBotAPI} plus the transport that has to be closed with it.
 *
 * <p>Assembled by hand rather than through {@code MaxBotAPI.create(token)} because the suite
 * needs its own {@link RecordingSerializer}. That has a consequence worth stating: the public
 * {@code MaxBotAPI(MaxClient)} constructor leaves the transport unowned, so {@code api.close()}
 * does nothing and the transport must be closed through {@link #close()}.</p>
 *
 * @param api       the assembled facade
 * @param transport the transport backing it, owned by this record
 */
public record LiveApi(MaxBotAPI api, JdkHttpMaxTransportClient transport) implements AutoCloseable {

    /** Retries cost 1s + 2s + 4s each; a hand-run suite should surface the error instead. */
    private static final int MAX_RETRIES = 1;

    /** Uploaded video can take a while to process; the client resends the message until then. */
    private static final Duration ATTACHMENT_READY_TIMEOUT = Duration.ofSeconds(60);

    /**
     * Builds a client for the configured token.
     *
     * @return the live API handle
     */
    public static LiveApi create() {
        return create(IntegrationConfig.token());
    }

    /**
     * Builds a client for an explicit token, used to exercise authentication failures.
     *
     * @param token the access token to authenticate with
     * @return the live API handle
     */
    public static LiveApi create(String token) {
        MaxClientConfig.Builder builder = MaxClientConfig.builder()
                .maxRetries(MAX_RETRIES)
                .attachmentReadyTimeout(ATTACHMENT_READY_TIMEOUT);
        String baseUrl = IntegrationConfig.baseUrlOrNull();
        if (baseUrl != null) {
            builder.baseUrl(baseUrl);
        }
        MaxClientConfig config = builder.build();

        MaxSerializer serializer = new RecordingSerializer();
        JdkHttpMaxTransportClient transport = new JdkHttpMaxTransportClient(token, config);
        return new LiveApi(new MaxBotAPI(new MaxClient(transport, serializer, config)), transport);
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        transport.close();
    }
}
