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

import ru.max.botapi.model.Update;

/**
 * Functional interface for handling incoming {@link Update} events from a webhook endpoint.
 *
 * <p>Implement this interface to process updates received via the MAX Bot API webhook
 * mechanism. Exceptions thrown by the handler are caught by {@link MaxWebhookServer} and
 * logged; the server always responds with HTTP 200 to prevent MAX from retrying the delivery.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * WebhookHandler handler = update -> System.out.println("Received: " + update.updateType());
 * MaxWebhookServer server = MaxWebhookServer.builder()
 *     .handler(handler)
 *     .serializer(new JacksonMaxSerializer())
 *     .build();
 * server.start();
 * }</pre>
 */
@FunctionalInterface
public interface WebhookHandler {

    /**
     * Called for each incoming {@link Update} delivered to the webhook endpoint.
     *
     * @param update the incoming update event; never {@code null}
     */
    void onUpdate(Update update);
}
