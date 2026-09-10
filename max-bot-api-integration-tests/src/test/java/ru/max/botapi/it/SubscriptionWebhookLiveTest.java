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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import ru.max.botapi.model.GetSubscriptionsResult;
import ru.max.botapi.model.SimpleQueryResult;
import ru.max.botapi.model.Subscription;
import ru.max.botapi.model.SubscriptionRequestBody;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateType;
import ru.max.botapi.webhook.MaxWebhookServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Webhook delivery end to end, using a public URL the operator proxies to this machine.
 *
 * <p>Runs last on purpose. A subscription changes how MAX delivers updates for the whole bot,
 * and it is not established whether {@code getUpdates} keeps working while one is active — so
 * every earlier class gets to finish first, and the subscription is removed in teardown even
 * when the test fails.</p>
 */
@Order(8)
@DisplayName("Live: webhook subscription")
class SubscriptionWebhookLiveTest extends LiveTestBase {

    private static final int DELIVERY_WAIT_SECONDS = 60;

    private MaxWebhookServer server;

    private String subscribedUrl;

    @Test
    @Order(1)
    @DisplayName("getSubscriptions lists current subscriptions")
    void getSubscriptions() {
        GetSubscriptionsResult result = api().getSubscriptions().execute();

        assertThat(result.subscriptions()).isNotNull();
        result.subscriptions().forEach(s -> System.out.println("Existing subscription: " + s.url()));
    }

    @Test
    @Order(2)
    @DisplayName("an update posted to the subscribed URL reaches the local server")
    void webhookRoundTrip() {
        // MAX posts to exactly the URL it was given, and the proxy forwards the path unchanged,
        // so the subscribed URL and the path the local server answers on must be one decision:
        // a URL with a path of its own is used verbatim, otherwise the default path is appended.
        String publicUrl = subscribableUrl(IntegrationConfig.webhookUrl());
        String path = URI.create(publicUrl).getPath();
        List<Update> delivered = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        server = MaxWebhookServer.builder()
                .handler(update -> {
                    delivered.add(update);
                    latch.countDown();
                })
                .serializer(new RecordingSerializer())
                .secret(IntegrationConfig.webhookSecretOrNull())
                .port(IntegrationConfig.webhookPort())
                .path(path)
                .build();

        try {
            server.start();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to bind the webhook server on port " + IntegrationConfig.webhookPort(), e);
        }
        System.out.println("Webhook server listening on port " + server.getPort() + " at "
                + server.getPath() + ", subscribing " + publicUrl
                + " — the proxy must forward that path unchanged to this port");

        SimpleQueryResult subscribed = api()
                .subscribe(new SubscriptionRequestBody(
                        publicUrl,
                        List.of(UpdateType.MESSAGE_CREATED),
                        IntegrationConfig.webhookSecretOrNull()))
                .execute();
        assertThat(subscribed.success()).isTrue();
        subscribedUrl = publicUrl;

        assertThat(api().getSubscriptions().execute().subscriptions())
                .extracting(Subscription::url)
                .contains(publicUrl);

        Prompts.step(Prompts.groupChat(),
                "Send any text message to the bot there, so MAX delivers it to the webhook.");

        boolean arrived = await(latch);

        delivered.forEach(ModelAssertions::assertFullyMapped);
        assertThat(arrived)
                .withFailMessage("Nothing reached %s within %ds. Check that the proxy forwards "
                        + "it to port %d with the path %s intact.",
                        publicUrl, DELIVERY_WAIT_SECONDS, server.getPort(), path)
                .isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("unsubscribe removes the subscription")
    void unsubscribe() {
        Assumptions.assumeTrue(subscribedUrl != null,
                "nothing was subscribed");

        SimpleQueryResult result = api().unsubscribe(subscribedUrl).execute();
        assertThat(result.success()).isTrue();

        assertThat(api().getSubscriptions().execute().subscriptions())
                .extracting(Subscription::url)
                .doesNotContain(subscribedUrl);
        subscribedUrl = null;
    }

    /**
     * Returns the URL to subscribe: the configured one when it already names a path, otherwise
     * the same host with {@link IntegrationConfig#WEBHOOK_PATH} appended.
     *
     * @param configured the value of {@code MAX_IT_WEBHOOK_URL}
     * @return a URL whose path the local server can bind to
     */
    private static String subscribableUrl(String configured) {
        String url = configured.endsWith("/")
                ? configured.substring(0, configured.length() - 1)
                : configured;
        String path = URI.create(url).getPath();
        return path == null || path.isEmpty() ? url + IntegrationConfig.WEBHOOK_PATH : url;
    }

    private static boolean await(CountDownLatch latch) {
        try {
            return latch.await(DELIVERY_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a webhook delivery", e);
        }
    }

    /**
     * Removes the subscription and stops the local server, whatever happened above.
     *
     * <p>Leaving a subscription behind would silently break long polling for this bot, so this
     * runs even when the round trip failed.</p>
     */
    @AfterAll
    void removeSubscription() {
        if (subscribedUrl != null) {
            cleanUp("unsubscribe " + subscribedUrl,
                    () -> api().unsubscribe(subscribedUrl).execute());
        }
        if (server != null) {
            cleanUp("stop webhook server", server::close);
        }
    }
}
