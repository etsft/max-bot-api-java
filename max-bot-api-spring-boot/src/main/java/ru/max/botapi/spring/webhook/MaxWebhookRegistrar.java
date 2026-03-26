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
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.model.SubscriptionRequestBody;

/**
 * Manages webhook lifecycle: registers the webhook URL with the MAX platform
 * on application startup and unregisters it on shutdown.
 *
 * <p>Listens for {@link ApplicationReadyEvent} (fired after the web server
 * is listening) to ensure the webhook endpoint is reachable before MAX starts
 * sending updates. Implements {@link DisposableBean} for graceful cleanup.</p>
 *
 * <p>Registration and unregistration failures are logged but never propagate —
 * they must not prevent application startup or shutdown.</p>
 */
public class MaxWebhookRegistrar
        implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(MaxWebhookRegistrar.class);

    private final MaxBotAPI api;
    private final MaxWebhookProperties properties;

    /**
     * Creates a new webhook registrar.
     *
     * @param api        the MAX Bot API instance; must not be {@code null}
     * @param properties the webhook configuration; must not be {@code null}
     */
    public MaxWebhookRegistrar(MaxBotAPI api, MaxWebhookProperties properties) {
        this.api = Objects.requireNonNull(api, "api must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * Registers the webhook URL with the MAX platform when the application is ready.
     *
     * @param event the application ready event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        registerWebhook();
    }

    /**
     * Unregisters the webhook URL from the MAX platform on bean destruction
     * if auto-unregistration is enabled.
     */
    @Override
    public void destroy() {
        if (properties.isAutoUnregister()) {
            unregisterWebhook();
        }
    }

    private void registerWebhook() {
        String webhookUrl = properties.getUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            LOG.info("Webhook URL not configured (max.bot.webhook.url), "
                    + "skipping auto-registration");
            return;
        }

        List<String> updateTypes = properties.getUpdateTypes().isEmpty()
                ? null
                : List.copyOf(properties.getUpdateTypes());

        SubscriptionRequestBody body = new SubscriptionRequestBody(
                webhookUrl, updateTypes, properties.getSecret());

        try {
            api.subscribe(body).execute();
            LOG.info("Webhook registered with MAX API: url={}", webhookUrl);
        } catch (Exception e) {
            LOG.error("Failed to register webhook with MAX API: url={}",
                    webhookUrl, e);
        }
    }

    private void unregisterWebhook() {
        String webhookUrl = properties.getUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        try {
            api.unsubscribe(webhookUrl).execute();
            LOG.info("Webhook unregistered from MAX API: url={}", webhookUrl);
        } catch (Exception e) {
            LOG.warn("Failed to unregister webhook from MAX API: url={}",
                    webhookUrl, e);
        }
    }
}
