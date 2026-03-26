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
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.jackson.JacksonMaxSerializer;
import ru.max.botapi.model.Update;
import ru.max.botapi.webhook.WebhookHandler;

import static org.assertj.core.api.Assertions.assertThat;

class MaxWebhookControllerTest {

    private final MaxSerializer serializer = new JacksonMaxSerializer();

    private String loadFixture(String name) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(
                "/fixtures/updates/" + name)) {
            if (is == null) {
                throw new IOException("Fixture not found: " + name);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void postWithValidBody_returns200AndCallsHandler() throws IOException {
        AtomicReference<Update> captured = new AtomicReference<>();
        WebhookHandler handler = captured::set;
        MaxWebhookProperties props = new MaxWebhookProperties();

        MaxWebhookController controller =
                new MaxWebhookController(handler, serializer, props);

        String json = loadFixture("message-created.json");
        ResponseEntity<Void> response = controller.handleWebhook(json, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().updateType()).isEqualTo("message_created");
    }

    @Test
    void postWithMessageCallback_returns200AndCallsHandler() throws IOException {
        AtomicReference<Update> captured = new AtomicReference<>();
        WebhookHandler handler = captured::set;
        MaxWebhookProperties props = new MaxWebhookProperties();

        MaxWebhookController controller =
                new MaxWebhookController(handler, serializer, props);

        String json = loadFixture("message-callback.json");
        ResponseEntity<Void> response = controller.handleWebhook(json, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().updateType()).isEqualTo("message_callback");
    }

    @Test
    void postWithValidSecretHeader_returns200() throws IOException {
        AtomicReference<Update> captured = new AtomicReference<>();
        WebhookHandler handler = captured::set;
        MaxWebhookProperties props = new MaxWebhookProperties();
        props.setSecret("my-secret");

        MaxWebhookController controller =
                new MaxWebhookController(handler, serializer, props);

        String json = loadFixture("message-created.json");
        ResponseEntity<Void> response =
                controller.handleWebhook(json, "my-secret");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(captured.get()).isNotNull();
    }

    @Test
    void postWithInvalidSecret_returns401() throws IOException {
        AtomicReference<Update> captured = new AtomicReference<>();
        WebhookHandler handler = captured::set;
        MaxWebhookProperties props = new MaxWebhookProperties();
        props.setSecret("my-secret");

        MaxWebhookController controller =
                new MaxWebhookController(handler, serializer, props);

        String json = loadFixture("message-created.json");
        ResponseEntity<Void> response =
                controller.handleWebhook(json, "wrong-secret");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(captured.get()).isNull();
    }

    @Test
    void postWithMissingSecretHeader_returns401() throws IOException {
        AtomicReference<Update> captured = new AtomicReference<>();
        WebhookHandler handler = captured::set;
        MaxWebhookProperties props = new MaxWebhookProperties();
        props.setSecret("my-secret");

        MaxWebhookController controller =
                new MaxWebhookController(handler, serializer, props);

        String json = loadFixture("message-created.json");
        ResponseEntity<Void> response = controller.handleWebhook(json, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(captured.get()).isNull();
    }

    @Test
    void postWithNoSecretConfigured_skipsValidation() throws IOException {
        AtomicReference<Update> captured = new AtomicReference<>();
        WebhookHandler handler = captured::set;
        MaxWebhookProperties props = new MaxWebhookProperties();
        // secret is null — no validation

        MaxWebhookController controller =
                new MaxWebhookController(handler, serializer, props);

        String json = loadFixture("message-created.json");
        ResponseEntity<Void> response =
                controller.handleWebhook(json, "any-value");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(captured.get()).isNotNull();
    }

    @Test
    void handlerException_stillReturns200() throws IOException {
        WebhookHandler handler = update -> {
            throw new RuntimeException("handler exploded");
        };
        MaxWebhookProperties props = new MaxWebhookProperties();

        MaxWebhookController controller =
                new MaxWebhookController(handler, serializer, props);

        String json = loadFixture("message-created.json");
        ResponseEntity<Void> response = controller.handleWebhook(json, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deserializationError_returns200() {
        AtomicReference<Update> captured = new AtomicReference<>();
        WebhookHandler handler = captured::set;
        MaxWebhookProperties props = new MaxWebhookProperties();

        MaxWebhookController controller =
                new MaxWebhookController(handler, serializer, props);

        ResponseEntity<Void> response =
                controller.handleWebhook("{invalid json!!!", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(captured.get()).isNull();
    }

    @Test
    void constantTimeEquals_matchingStrings() {
        assertThat(MaxWebhookController.constantTimeEquals("abc", "abc"))
                .isTrue();
    }

    @Test
    void constantTimeEquals_differentStrings() {
        assertThat(MaxWebhookController.constantTimeEquals("abc", "def"))
                .isFalse();
    }

    @Test
    void constantTimeEquals_nullExpected() {
        assertThat(MaxWebhookController.constantTimeEquals(null, "abc"))
                .isFalse();
    }

    @Test
    void constantTimeEquals_nullActual() {
        assertThat(MaxWebhookController.constantTimeEquals("abc", null))
                .isFalse();
    }

    @Test
    void constantTimeEquals_bothNull() {
        assertThat(MaxWebhookController.constantTimeEquals(null, null))
                .isFalse();
    }
}
