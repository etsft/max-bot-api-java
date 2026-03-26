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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.model.Update;
import ru.max.botapi.webhook.WebhookHandler;

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
 */
@RestController
public class MaxWebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(MaxWebhookController.class);

    static final String SECRET_HEADER = "X-Max-Bot-Api-Secret";

    private final WebhookHandler handler;
    private final MaxSerializer serializer;
    private final String secret;

    /**
     * Creates a new webhook controller.
     *
     * @param handler    the update handler; must not be {@code null}
     * @param serializer the JSON serializer; must not be {@code null}
     * @param properties the webhook configuration properties; must not be {@code null}
     */
    public MaxWebhookController(WebhookHandler handler,
                                MaxSerializer serializer,
                                MaxWebhookProperties properties) {
        this.handler = Objects.requireNonNull(handler, "handler must not be null");
        this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
        Objects.requireNonNull(properties, "properties must not be null");
        this.secret = properties.getSecret();
    }

    /**
     * Handles incoming webhook POST requests from the MAX platform.
     *
     * <p>Validates the secret header (if configured), deserializes the JSON body
     * into an {@link Update}, and dispatches it to the {@link WebhookHandler}.
     * Returns HTTP 200 in all cases except invalid secret (HTTP 401).</p>
     *
     * @param body         the raw JSON request body
     * @param headerSecret the value of the {@code X-Max-Bot-Api-Secret} header, or {@code null}
     * @return HTTP 200 on success or handler error; HTTP 401 on invalid secret
     */
    @PostMapping("${max.bot.webhook.path:/max-bot/webhook}")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String body,
            @RequestHeader(value = SECRET_HEADER, required = false) String headerSecret) {

        if (secret != null && !constantTimeEquals(secret, headerSecret)) {
            LOG.warn("Rejected webhook request: invalid or missing {} header",
                    SECRET_HEADER);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Update update = serializer.deserialize(body, Update.class);
            handler.onUpdate(update);
        } catch (Exception e) {
            LOG.error("Error handling webhook update", e);
        }

        return ResponseEntity.ok().build();
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
