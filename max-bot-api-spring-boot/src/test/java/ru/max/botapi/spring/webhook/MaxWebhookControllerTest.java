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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.unit.DataSize;

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.core.UpdateHandler;
import ru.max.botapi.jackson.JacksonMaxSerializer;
import ru.max.botapi.model.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        UpdateHandler handler = captured::set;
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
        UpdateHandler handler = captured::set;
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
        UpdateHandler handler = captured::set;
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
        UpdateHandler handler = captured::set;
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
        UpdateHandler handler = captured::set;
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
        UpdateHandler handler = captured::set;
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
        UpdateHandler handler = update -> {
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
        UpdateHandler handler = captured::set;
        MaxWebhookProperties props = new MaxWebhookProperties();

        MaxWebhookController controller =
                new MaxWebhookController(handler, serializer, props);

        ResponseEntity<Void> response =
                controller.handleWebhook("{invalid json!!!", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(captured.get()).isNull();
    }

    @Test
    void dispatchExecutor_runsTheHandlerAfterResponding() throws IOException {
        List<Runnable> queued = new ArrayList<>();
        AtomicReference<Update> captured = new AtomicReference<>();
        MaxWebhookController controller = new MaxWebhookController(
                captured::set, serializer, new MaxWebhookProperties(), queued::add);

        ResponseEntity<Void> response =
                controller.handleWebhook(loadFixture("message-created.json"), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(captured.get()).isNull();
        assertThat(queued).hasSize(1);

        queued.getFirst().run();
        assertThat(captured.get()).isNotNull();
    }

    @Test
    void dispatchExecutor_containsAHandlerException() throws IOException {
        List<Runnable> queued = new ArrayList<>();
        UpdateHandler handler = update -> {
            throw new RuntimeException("handler exploded");
        };
        MaxWebhookController controller = new MaxWebhookController(
                handler, serializer, new MaxWebhookProperties(), queued::add);

        controller.handleWebhook(loadFixture("message-created.json"), null);

        assertThatCode(() -> queued.getFirst().run()).doesNotThrowAnyException();
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

    @Test
    void receiveWebhook_bodyOverTheLimit_returns413WithoutCallingHandler() throws IOException {
        List<Update> received = new ArrayList<>();
        byte[] json = loadFixture("message-created.json").getBytes(StandardCharsets.UTF_8);
        MaxWebhookProperties props = new MaxWebhookProperties();
        props.setMaxBodySize(DataSize.ofBytes(json.length - 1));
        MaxWebhookController controller = new MaxWebhookController(received::add, serializer, props);

        ResponseEntity<Void> response = controller.receiveWebhook(new ByteArrayInputStream(json), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
        assertThat(received).isEmpty();
    }

    @Test
    void receiveWebhook_bodyExactlyAtTheLimit_isHandled() throws IOException {
        List<Update> received = new ArrayList<>();
        byte[] json = loadFixture("message-created.json").getBytes(StandardCharsets.UTF_8);
        MaxWebhookProperties props = new MaxWebhookProperties();
        props.setMaxBodySize(DataSize.ofBytes(json.length));
        MaxWebhookController controller = new MaxWebhookController(received::add, serializer, props);

        ResponseEntity<Void> response = controller.receiveWebhook(new ByteArrayInputStream(json), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(received).hasSize(1);
    }

    @Test
    void receiveWebhook_wrongSecret_returns401WithoutReadingTheBody() {
        MaxWebhookProperties props = new MaxWebhookProperties();
        props.setSecret("my-secret");
        MaxWebhookController controller = new MaxWebhookController(update -> { }, serializer, props);
        InputStream unreadable = new InputStream() {
            @Override
            public int read() {
                throw new AssertionError("body must not be read before the secret is checked");
            }
        };

        ResponseEntity<Void> response = controller.receiveWebhook(unreadable, "wrong");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void maxBodySizeMustBePositive() {
        MaxWebhookProperties props = new MaxWebhookProperties();
        assertThatThrownBy(() -> props.setMaxBodySize(DataSize.ofBytes(0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void springMvcRoutesThePostAndEnforcesTheLimit() throws Exception {
        List<Update> received = new ArrayList<>();
        String json = loadFixture("message-created.json");
        MaxWebhookProperties props = new MaxWebhookProperties();
        props.setMaxBodySize(DataSize.ofBytes(json.getBytes(StandardCharsets.UTF_8).length));
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new MaxWebhookController(received::add, serializer, props))
                .build();

        mvc.perform(post("/max-bot/webhook").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
        mvc.perform(post("/max-bot/webhook").contentType(MediaType.APPLICATION_JSON)
                        .content(json + " "))
                .andExpect(status().isContentTooLarge());

        assertThat(received).hasSize(1);
    }
}
