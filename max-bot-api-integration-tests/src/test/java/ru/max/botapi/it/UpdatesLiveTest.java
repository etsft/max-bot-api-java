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

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import ru.max.botapi.longpolling.MaxLongPollingConsumer;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateList;
import ru.max.botapi.model.UpdateType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Update delivery over long polling.
 *
 * <p>The strict check matters most here: a {@code message_created} the library cannot map used
 * to abort the whole batch and stall the marker, so every update this class receives is walked
 * by {@link ModelAssertions}.</p>
 */
@Order(5)
@DisplayName("Live: updates over long polling")
class UpdatesLiveTest extends LiveTestBase {

    private static final int POLL_TIMEOUT_SECONDS = 5;

    private static final int INCOMING_WAIT_SECONDS = 90;

    private Long marker;

    /**
     * Long polling only sees updates that are not being diverted to a webhook.
     */
    @BeforeAll
    void clearSubscriptions() {
        clearOwnWebhookSubscriptions();
    }

    @Test
    @Order(1)
    @DisplayName("getUpdates drains the queue and every update maps to a concrete type")
    void getUpdates() {
        UpdateList updates = api().getUpdates()
                .timeout(POLL_TIMEOUT_SECONDS)
                .limit(100)
                .execute();

        ModelAssertions.assertFullyMapped(updates);
        marker = updates.marker();
        System.out.println("Drained " + updates.updates().size()
                + " update(s), marker=" + marker);
    }

    @Test
    @Order(2)
    @DisplayName("getUpdates accepts a marker and a type filter")
    void getUpdatesFiltered() {
        var query = api().getUpdates()
                .timeout(POLL_TIMEOUT_SECONDS)
                .types(Set.of(UpdateType.MESSAGE_CREATED, UpdateType.MESSAGE_CALLBACK));
        if (marker != null) {
            query.marker(marker);
        }

        UpdateList updates = query.execute();

        ModelAssertions.assertFullyMapped(updates);
        assertThat(updates.updates())
                .allMatch(u -> u.updateType().equals("message_created")
                        || u.updateType().equals("message_callback"));
    }

    @Test
    @Order(3)
    @DisplayName("the consumer receives a real incoming message")
    void receivesIncomingMessage() {
        Prompts.step(Prompts.groupChat(),
                "Send any text message to the bot there. Not a direct message to the bot: "
                + "this step watches the chat above.");

        List<Update> received = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
                .api(api())
                .pollTimeout(POLL_TIMEOUT_SECONDS)
                .handler(update -> {
                    received.add(update);
                    if (update instanceof MessageCreatedUpdate) {
                        latch.countDown();
                    }
                })
                .onError(e -> System.out.println("Long polling error: " + e))
                .build();

        boolean arrived;
        consumer.start();
        try {
            arrived = await(latch);
        } finally {
            consumer.stop();
        }

        received.forEach(ModelAssertions::assertFullyMapped);
        assertThat(arrived)
                .withFailMessage("No message_created arrived within %ds; received: %s",
                        INCOMING_WAIT_SECONDS, received)
                .isTrue();
    }

    private static boolean await(CountDownLatch latch) {
        try {
            return latch.await(INCOMING_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for an update", e);
        }
    }
}
