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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.model.Subscription;

/**
 * Shared lifecycle for every live test class.
 *
 * <p>The {@code liveTest} task is the only thing that runs this package, but the
 * {@link EnabledIfEnvironmentVariable} guard is kept as a second lock: it makes an accidental
 * run — a misconfigured IDE, someone re-enabling {@code test} — a skip rather than a burst of
 * real API calls.</p>
 *
 * <p>Steps within a class genuinely depend on each other (a message must exist before it can be
 * pinned), so ordering is explicit and state is shared across methods via
 * {@link TestInstance.Lifecycle#PER_CLASS}.</p>
 */
@Tag("live")
@EnabledIfEnvironmentVariable(named = IntegrationConfig.TOKEN, matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
abstract class LiveTestBase {

    /** The live API under test. */
    protected LiveApi live;

    /**
     * Opens the client and announces the configuration in effect.
     */
    @BeforeAll
    void openClient() {
        live = LiveApi.create();
        System.out.println("Live suite: token=" + IntegrationConfig.maskedToken()
                + ", interactive=" + IntegrationConfig.interactive()
                + ", destructive=" + IntegrationConfig.destructive());
    }

    /**
     * Closes the transport. {@code MaxBotAPI.close()} would not, see {@link LiveApi}.
     */
    @AfterAll
    void closeClient() {
        if (live != null) {
            live.close();
        }
        RecordingSerializer.reset();
    }

    /**
     * Returns the API facade.
     *
     * @return the facade
     */
    protected MaxBotAPI api() {
        return live.api();
    }

    /**
     * Makes sure updates will reach long polling, clearing this suite's own leftovers.
     *
     * <p>While a webhook subscription is registered, MAX delivers to it and {@code getUpdates}
     * comes back empty — which shows up as "no update arrived" and reads like a broken
     * consumer. A subscription this suite created (its URL ends with
     * {@link IntegrationConfig#WEBHOOK_PATH}) can outlive an interrupted run, so it is removed
     * here. Anything else belongs to someone and is only reported.</p>
     */
    protected void clearOwnWebhookSubscriptions() {
        List<Subscription> subscriptions = api().getSubscriptions().execute().subscriptions();
        if (subscriptions == null || subscriptions.isEmpty()) {
            return;
        }
        for (Subscription subscription : subscriptions) {
            String url = subscription.url();
            if (url.endsWith(IntegrationConfig.WEBHOOK_PATH)) {
                System.out.println("Removing a leftover subscription from an earlier run: " + url
                        + " (it would divert updates away from long polling)");
                cleanUp("unsubscribe " + url, () -> api().unsubscribe(url).execute());
            } else {
                System.out.println("WARNING: " + url + " is subscribed to this bot's updates. "
                        + "MAX may deliver there instead of to long polling, in which case the "
                        + "steps below will time out waiting for an update that never comes.");
            }
        }
    }

    /**
     * Runs a cleanup step, reporting but swallowing its failure.
     *
     * <p>Teardown must never replace the exception that actually failed the test, and one
     * broken step must not stop the rest of the cleanup. Steps are skipped outright when the
     * client was never opened — JUnit still calls {@code @AfterAll} after an aborted
     * {@code @BeforeAll}, and there is nothing to clean up in that case.</p>
     *
     * @param what human-readable description, used if the step fails
     * @param step the cleanup action
     */
    protected void cleanUp(String what, Runnable step) {
        if (live == null) {
            return;
        }
        try {
            step.run();
        } catch (RuntimeException e) {
            System.out.println("Cleanup failed (" + what + "): " + e);
        }
    }
}
