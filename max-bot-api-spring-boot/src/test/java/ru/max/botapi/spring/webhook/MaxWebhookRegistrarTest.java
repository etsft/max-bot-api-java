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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.client.queries.SubscribeQuery;
import ru.max.botapi.client.queries.UnsubscribeQuery;
import ru.max.botapi.model.SubscriptionRequestBody;
import ru.max.botapi.model.UpdateType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaxWebhookRegistrarTest {

    @Test
    void registersWebhookOnApplicationReady() {
        AtomicReference<SubscriptionRequestBody> captured = new AtomicReference<>();
        MaxBotAPI api = mock(MaxBotAPI.class);
        SubscribeQuery subscribeQuery = mock(SubscribeQuery.class);
        when(api.subscribe(any(SubscriptionRequestBody.class)))
                .thenAnswer(invocation -> {
                    captured.set(invocation.getArgument(0));
                    return subscribeQuery;
                });

        MaxWebhookProperties props = new MaxWebhookProperties();
        props.setUrl("https://example.com/webhook");
        props.setSecret("test-secret");
        props.setUpdateTypes(List.of(UpdateType.MESSAGE_CREATED));

        MaxWebhookRegistrar registrar = new MaxWebhookRegistrar(api, props);
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);
        registrar.onApplicationEvent(event);

        verify(subscribeQuery).execute();
        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().url()).isEqualTo("https://example.com/webhook");
        assertThat(captured.get().secret()).isEqualTo("test-secret");
        assertThat(captured.get().updateTypes()).containsExactly(UpdateType.MESSAGE_CREATED);
    }

    @Test
    void skipsRegistrationWhenUrlIsNull() {
        MaxBotAPI api = mock(MaxBotAPI.class);
        MaxWebhookProperties props = new MaxWebhookProperties();
        // url is null

        MaxWebhookRegistrar registrar = new MaxWebhookRegistrar(api, props);
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);
        registrar.onApplicationEvent(event);

        verify(api, never()).subscribe(any());
    }

    @Test
    void skipsRegistrationWhenUrlIsBlank() {
        MaxBotAPI api = mock(MaxBotAPI.class);
        MaxWebhookProperties props = new MaxWebhookProperties();
        props.setUrl("   ");

        MaxWebhookRegistrar registrar = new MaxWebhookRegistrar(api, props);
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);
        registrar.onApplicationEvent(event);

        verify(api, never()).subscribe(any());
    }

    @Test
    void registrationWithEmptyUpdateTypes_passesNull() {
        AtomicReference<SubscriptionRequestBody> captured = new AtomicReference<>();
        MaxBotAPI api = mock(MaxBotAPI.class);
        SubscribeQuery subscribeQuery = mock(SubscribeQuery.class);
        when(api.subscribe(any(SubscriptionRequestBody.class)))
                .thenAnswer(invocation -> {
                    captured.set(invocation.getArgument(0));
                    return subscribeQuery;
                });

        MaxWebhookProperties props = new MaxWebhookProperties();
        props.setUrl("https://example.com/webhook");
        // updateTypes is empty list → should pass null to API

        MaxWebhookRegistrar registrar = new MaxWebhookRegistrar(api, props);
        registrar.onApplicationEvent(mock(ApplicationReadyEvent.class));

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().url()).isEqualTo("https://example.com/webhook");
        assertThat(captured.get().updateTypes()).isNull();
    }

    @Test
    void registrationFailure_doesNotThrow() {
        MaxBotAPI api = mock(MaxBotAPI.class);
        when(api.subscribe(any(SubscriptionRequestBody.class)))
                .thenThrow(new RuntimeException("connection refused"));

        MaxWebhookProperties props = new MaxWebhookProperties();
        props.setUrl("https://example.com/webhook");

        MaxWebhookRegistrar registrar = new MaxWebhookRegistrar(api, props);

        assertThatCode(() -> registrar.onApplicationEvent(
                mock(ApplicationReadyEvent.class)))
                .doesNotThrowAnyException();
    }

    @Test
    void unregistersOnDestroy() {
        MaxBotAPI api = mock(MaxBotAPI.class);
        UnsubscribeQuery unsubscribeQuery = mock(UnsubscribeQuery.class);
        when(api.unsubscribe(anyString())).thenReturn(unsubscribeQuery);

        MaxWebhookProperties props = new MaxWebhookProperties();
        props.setUrl("https://example.com/webhook");
        props.setAutoUnregister(true);

        MaxWebhookRegistrar registrar = new MaxWebhookRegistrar(api, props);
        registrar.destroy();

        verify(api).unsubscribe("https://example.com/webhook");
        verify(unsubscribeQuery).execute();
    }

    @Test
    void skipsUnregistrationWhenAutoUnregisterDisabled() {
        MaxBotAPI api = mock(MaxBotAPI.class);
        MaxWebhookProperties props = new MaxWebhookProperties();
        props.setUrl("https://example.com/webhook");
        props.setAutoUnregister(false);

        MaxWebhookRegistrar registrar = new MaxWebhookRegistrar(api, props);
        registrar.destroy();

        verify(api, never()).unsubscribe(anyString());
    }

    @Test
    void skipsUnregistrationWhenUrlIsNull() {
        MaxBotAPI api = mock(MaxBotAPI.class);
        MaxWebhookProperties props = new MaxWebhookProperties();
        props.setAutoUnregister(true);
        // url is null

        MaxWebhookRegistrar registrar = new MaxWebhookRegistrar(api, props);
        registrar.destroy();

        verify(api, never()).unsubscribe(anyString());
    }

    @Test
    void unregistrationFailure_doesNotThrow() {
        MaxBotAPI api = mock(MaxBotAPI.class);
        when(api.unsubscribe(anyString()))
                .thenThrow(new RuntimeException("network error"));

        MaxWebhookProperties props = new MaxWebhookProperties();
        props.setUrl("https://example.com/webhook");
        props.setAutoUnregister(true);

        MaxWebhookRegistrar registrar = new MaxWebhookRegistrar(api, props);

        assertThatCode(() -> registrar.destroy())
                .doesNotThrowAnyException();
    }
}
