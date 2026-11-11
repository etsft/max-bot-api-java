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

package ru.max.botapi.spring.webhook;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.core.UpdateHandler;
import ru.max.botapi.model.Nullable;
import ru.max.botapi.model.Update;

/**
 * Spring MVC controller that receives MAX Bot API webhook updates.
 *
 * <p>This controller is registered into the host application's
 * {@link org.springframework.web.servlet.DispatcherServlet} —
 * it does not create its own web server. The endpoint path is configurable
 * via {@code max.bot.webhook.path} (defaults to {@code /max-bot/webhook}).</p>
 *
 * <p>If a shared secret is configured, the controller validates the
 * {@code X-Max-Bot-Api-Secret} header using constant-time comparison
 * to prevent timing-based side-channel attacks.</p>
 *
 * <p>The controller always returns HTTP 200 for successfully received requests
 * (even when the handler throws an exception) to prevent the MAX platform
 * from retrying delivery.</p>
 *
 * <p>MAX expects that 200 within 30 seconds; a slower response counts as a failed delivery
 * and the update is sent again. By default the handler runs before the response is sent, so
 * a handler that can take that long should either hand its work off itself or run with a
 * dispatch executor ({@code max.bot.webhook.async-dispatch=true}), which answers first and
 * runs the handler afterwards.</p>
 *
 * <p>The body is read only after the secret has been checked, and at most
 * {@code max.bot.webhook.max-body-size} bytes of it (1 MB by default); a larger request is
 * answered with HTTP 413.</p>
 */
@RestController
public class MaxWebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(MaxWebhookController.class);

    static final String SECRET_HEADER = "X-Max-Bot-Api-Secret";

    private final UpdateHandler handler;
    private final MaxSerializer serializer;
    private final String secret;
    private final @Nullable Executor dispatchExecutor;
    private final int maxBodyBytes;

    /**
     * Creates a new webhook controller that runs the handler before responding.
     *
     * @param handler    the update handler; must not be {@code null}
     * @param serializer the JSON serializer; must not be {@code null}
     * @param properties the webhook configuration properties; must not be {@code null}
     */
    public MaxWebhookController(UpdateHandler handler,
                                MaxSerializer serializer,
                                MaxWebhookProperties properties) {
        this(handler, serializer, properties, null);
    }

    /**
     * Creates a new webhook controller.
     *
     * @param handler          the update handler; must not be {@code null}
     * @param serializer       the JSON serializer; must not be {@code null}
     * @param properties       the webhook configuration properties; must not be {@code null}
     * @param dispatchExecutor the executor the handler runs on after the response is sent;
     *                         {@code null} runs the handler before responding
     */
    public MaxWebhookController(UpdateHandler handler,
                                MaxSerializer serializer,
                                MaxWebhookProperties properties,
                                @Nullable Executor dispatchExecutor) {
        this.handler = Objects.requireNonNull(handler, "handler must not be null");
        this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
        Objects.requireNonNull(properties, "properties must not be null");
        this.secret = properties.getSecret();
        this.dispatchExecutor = dispatchExecutor;
        this.maxBodyBytes = (int) Math.min(properties.getMaxBodySize().toBytes(),
                Integer.MAX_VALUE - 8);
    }

    /**
     * Handles incoming webhook POST requests from the MAX platform.
     *
     * <p>Validates the secret header (if configured) before reading anything, then reads at
     * most {@code max.bot.webhook.max-body-size} bytes of the body and hands it to
     * {@link #handleWebhook(String, String)}.</p>
     *
     * @param body         the request body stream
     * @param headerSecret the value of the {@code X-Max-Bot-Api-Secret} header, or {@code null}
     * @return HTTP 200 on success or handler error; HTTP 401 on invalid secret; HTTP 413 if the
     *         body is larger than the limit; HTTP 400 if the body cannot be read
     */
    @PostMapping("${max.bot.webhook.path:/max-bot/webhook}")
    public ResponseEntity<Void> receiveWebhook(
            InputStream body,
            @RequestHeader(value = SECRET_HEADER, required = false) String headerSecret) {

        if (!secretMatches(headerSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        byte[] bytes;
        try {
            bytes = body.readNBytes(maxBodyBytes + 1);
        } catch (IOException e) {
            LOG.warn("Could not read webhook request body", e);
            return ResponseEntity.badRequest().build();
        }
        if (bytes.length > maxBodyBytes) {
            LOG.warn("Rejected webhook request: body exceeds {} bytes (max.bot.webhook.max-body-size)",
                    maxBodyBytes);
            return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).build();
        }
        return process(new String(bytes, StandardCharsets.UTF_8));
    }

    /**
     * Handles a webhook request whose body has already been read.
     *
     * <p>Validates the secret header (if configured), deserializes the JSON body
     * into an {@link Update}, and dispatches it to the {@link UpdateHandler}.
     * Returns HTTP 200 in all cases except invalid secret (HTTP 401). No size limit is applied
     * here: the caller has already read the body.</p>
     *
     * @param body         the raw JSON request body
     * @param headerSecret the value of the {@code X-Max-Bot-Api-Secret} header, or {@code null}
     * @return HTTP 200 on success or handler error; HTTP 401 on invalid secret
     */
    public ResponseEntity<Void> handleWebhook(String body, @Nullable String headerSecret) {
        if (!secretMatches(headerSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return process(body);
    }

    private boolean secretMatches(@Nullable String headerSecret) {
        if (secret != null && !constantTimeEquals(secret, headerSecret)) {
            LOG.warn("Rejected webhook request: invalid or missing {} header", SECRET_HEADER);
            return false;
        }
        return true;
    }

    private ResponseEntity<Void> process(String body) {
        try {
            Update update = serializer.deserialize(body, Update.class);
            if (dispatchExecutor == null) {
                handler.onUpdate(update);
            } else {
                dispatchExecutor.execute(() -> dispatch(update));
            }
        } catch (Exception e) {
            LOG.error("Error handling webhook update", e);
        }

        return ResponseEntity.ok().build();
    }

    private void dispatch(Update update) {
        try {
            handler.onUpdate(update);
        } catch (Exception e) {
            LOG.error("Error handling webhook update", e);
        }
    }

    /**
     * Compares two strings in constant time to prevent timing attacks.
     *
     * @param expected the expected value; may be {@code null}
     * @param actual   the actual value; may be {@code null}
     * @return {@code true} if both are non-null and byte-for-byte equal;
     *         {@code false} if either argument is {@code null}
     */
    static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
