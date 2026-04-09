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
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.core.UpdateHandler;
import ru.max.botapi.model.Nullable;
import ru.max.botapi.model.SubscriptionRequestBody;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateType;

/**
 * Embedded HTTP/HTTPS webhook server for receiving MAX Bot API updates.
 *
 * <p>Backed by {@code com.sun.net.httpserver.HttpServer} (part of the JDK) and uses a
 * virtual-thread executor for request handling. Validates the optional
 * {@code X-Max-Bot-Api-Secret} request header if a secret was configured.</p>
 *
 * <p>Always responds with HTTP 200, even on handler exceptions, to prevent the MAX platform
 * from retrying event delivery.</p>
 *
 * <p>Example — plain HTTP (development/testing):</p>
 * <pre>{@code
 * MaxWebhookServer server = MaxWebhookServer.builder()
 *     .handler(update -> System.out.println(update.updateType()))
 *     .serializer(new JacksonMaxSerializer())
 *     .port(8080)
 *     .build();
 * server.start();
 * server.register(api, "https://example.com/webhook", null);
 * }</pre>
 *
 * <p>Example — HTTPS with SSL:</p>
 * <pre>{@code
 * SSLContext ssl = ...; // load your keystore
 * server.start(ssl);
 * }</pre>
 *
 * <p>This class implements {@link AutoCloseable}; closing it stops the server immediately.</p>
 */
public class MaxWebhookServer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MaxWebhookServer.class);

    private static final String DEFAULT_PATH = "/webhook";
    private static final int DEFAULT_PORT = 8443;
    private static final String SECRET_HEADER = "X-Max-Bot-Api-Secret";

    private final UpdateHandler handler;
    private final MaxSerializer serializer;
    private final @Nullable String secret;
    private final int port;
    private final String path;

    private volatile HttpServer server;

    private MaxWebhookServer(Builder builder) {
        this.handler = Objects.requireNonNull(builder.handler, "handler must not be null");
        this.serializer = Objects.requireNonNull(builder.serializer, "serializer must not be null");
        this.secret = builder.secret;
        this.port = builder.port;
        this.path = builder.path;
    }

    /**
     * Creates a new {@link Builder} for {@code MaxWebhookServer}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Starts a plain HTTP server on the configured port.
     *
     * <p>Intended for local development and testing. For production use,
     * prefer {@link #start(SSLContext)} with a valid TLS certificate.</p>
     *
     * @throws IOException if the server cannot bind to the port
     */
    public void start() throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext(path, this::handleRequest);
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        this.server = httpServer;
        httpServer.start();
        LOG.info("Webhook HTTP server started on port {} at path {}", port, path);
    }

    /**
     * Starts an HTTPS server with the given {@link SSLContext}.
     *
     * @param sslContext the SSL context loaded with the server's key material; must not be {@code null}
     * @throws IOException if the server cannot bind to the port
     */
    public void start(SSLContext sslContext) throws IOException {
        Objects.requireNonNull(sslContext, "sslContext must not be null");
        HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(port), 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsServer.createContext(path, this::handleRequest);
        httpsServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        this.server = httpsServer;
        httpsServer.start();
        LOG.info("Webhook HTTPS server started on port {} at path {}", port, path);
    }

    /**
     * Registers the webhook URL with the MAX Bot API.
     *
     * @param api         the {@link MaxBotAPI} instance to use for registration; must not be {@code null}
     * @param webhookUrl  the publicly reachable URL MAX should call; must not be {@code null}
     * @param updateTypes optional set of update types to subscribe to; {@code null} means all types
     */
    public void register(MaxBotAPI api, String webhookUrl, @Nullable Set<UpdateType> updateTypes) {
        Objects.requireNonNull(api, "api must not be null");
        Objects.requireNonNull(webhookUrl, "webhookUrl must not be null");
        List<UpdateType> typeList = updateTypes == null ? null : List.copyOf(updateTypes);
        SubscriptionRequestBody body = new SubscriptionRequestBody(webhookUrl, typeList, secret);
        api.subscribe(body).execute();
        LOG.info("Webhook registered: url={}", webhookUrl);
    }

    /**
     * Unregisters the webhook URL from the MAX Bot API.
     *
     * @param api        the {@link MaxBotAPI} instance to use; must not be {@code null}
     * @param webhookUrl the webhook URL to unregister; must not be {@code null}
     */
    public void unregister(MaxBotAPI api, String webhookUrl) {
        Objects.requireNonNull(api, "api must not be null");
        Objects.requireNonNull(webhookUrl, "webhookUrl must not be null");
        api.unsubscribe(webhookUrl).execute();
        LOG.info("Webhook unregistered: url={}", webhookUrl);
    }

    /**
     * Stops the server immediately. After this call the server cannot be restarted.
     */
    @Override
    public void close() {
        HttpServer s = this.server;
        if (s != null) {
            s.stop(0);
            LOG.info("Webhook server stopped");
        }
    }

    /**
     * Returns the port this server listens on.
     *
     * @return the configured port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the context path this server handles webhook requests on.
     *
     * @return the URL path (e.g., {@code "/webhook"})
     */
    public String getPath() {
        return path;
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            if (secret != null) {
                String headerSecret = exchange.getRequestHeaders().getFirst(SECRET_HEADER);
                if (!constantTimeEquals(secret, headerSecret)) {
                    LOG.warn("Rejected webhook request: invalid or missing {} header", SECRET_HEADER);
                    exchange.sendResponseHeaders(401, -1);
                    return;
                }
            }
            byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
            String body = new String(bodyBytes, StandardCharsets.UTF_8);
            Update update = serializer.deserialize(body, Update.class);
            handler.onUpdate(update);
            exchange.sendResponseHeaders(200, -1);
        } catch (Exception e) {
            LOG.error("Error handling webhook request", e);
            // Always return 200 so MAX does not retry delivery
            exchange.sendResponseHeaders(200, -1);
        } finally {
            exchange.close();
        }
    }

    /**
     * Compares two strings for equality in constant time to prevent timing-based side-channel
     * attacks on secret comparison.
     *
     * <p>Uses {@link MessageDigest#isEqual(byte[], byte[])} which is guaranteed not to
     * short-circuit on differing bytes.</p>
     *
     * @param a first string; {@code null} is treated as mismatch
     * @param b second string; {@code null} is treated as mismatch
     * @return {@code true} if both strings are non-null and byte-for-byte equal in UTF-8
     */
    private static boolean constantTimeEquals(@Nullable String a, @Nullable String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Builder for {@link MaxWebhookServer}.
     */
    public static final class Builder {

        private UpdateHandler handler;
        private MaxSerializer serializer;
        private @Nullable String secret;
        private int port = DEFAULT_PORT;
        private String path = DEFAULT_PATH;

        private Builder() {
        }

        /**
         * Sets the {@link UpdateHandler} that processes incoming updates. Required.
         *
         * @param handler the update handler; must not be {@code null}
         * @return this builder
         */
        public Builder handler(UpdateHandler handler) {
            this.handler = Objects.requireNonNull(handler, "handler must not be null");
            return this;
        }

        /**
         * Sets the {@link MaxSerializer} used to deserialize incoming JSON payloads. Required.
         *
         * @param serializer the JSON serializer; must not be {@code null}
         * @return this builder
         */
        public Builder serializer(MaxSerializer serializer) {
            this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
            return this;
        }

        /**
         * Sets the optional shared secret used for request authentication.
         *
         * <p>When set, the server validates the {@code X-Max-Bot-Api-Secret} header on every
         * incoming request and returns HTTP 401 if the value does not match.</p>
         *
         * @param secret the shared secret string; {@code null} disables header validation
         * @return this builder
         */
        public Builder secret(@Nullable String secret) {
            this.secret = secret;
            return this;
        }

        /**
         * Sets the port the server binds to. Defaults to {@value DEFAULT_PORT}.
         *
         * @param port the TCP port number
         * @return this builder
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the URL path the server handles. Defaults to {@value DEFAULT_PATH}.
         *
         * @param path the URL context path (must start with {@code /})
         * @return this builder
         * @throws IllegalArgumentException if {@code path} does not start with {@code /}
         */
        public Builder path(String path) {
            Objects.requireNonNull(path, "path must not be null");
            if (!path.startsWith("/")) {
                throw new IllegalArgumentException("path must start with '/', got: " + path);
            }
            this.path = path;
            return this;
        }

        /**
         * Builds and returns a new {@link MaxWebhookServer}.
         *
         * @return a new server instance
         * @throws NullPointerException if {@code handler} or {@code serializer} was not set
         */
        public MaxWebhookServer build() {
            return new MaxWebhookServer(this);
        }
    }
}
