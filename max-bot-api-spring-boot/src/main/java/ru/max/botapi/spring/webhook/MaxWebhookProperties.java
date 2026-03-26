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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the MAX Bot API webhook integration.
 *
 * <p>All properties are bound under the {@code max.bot.webhook} prefix in
 * {@code application.yml} or {@code application.properties}.</p>
 *
 * <p>Example configuration:</p>
 * <pre>{@code
 * max:
 *   bot:
 *     mode: webhook
 *     webhook:
 *       token: "your-bot-token"
 *       path: "/max-bot/webhook"
 *       secret: "shared-secret"
 *       url: "https://myapp.example.com/max-bot/webhook"
 *       auto-register: true
 *       auto-unregister: true
 *       update-types:
 *         - message_created
 *         - message_callback
 * }</pre>
 */
@ConfigurationProperties(prefix = "max.bot.webhook")
public class MaxWebhookProperties {

    /**
     * Bot access token for the MAX Bot API.
     * Used to create a {@code MaxBotAPI} bean if one is not already provided.
     */
    private String token;

    /**
     * URL path where the webhook endpoint is exposed within the Spring MVC context.
     * Must start with {@code /}.
     */
    private String path = "/max-bot/webhook";

    /**
     * Shared secret for {@code X-Max-Bot-Api-Secret} header validation.
     * When set, every incoming request must contain this header value.
     * When {@code null}, secret validation is disabled.
     */
    private String secret;

    /**
     * Publicly reachable webhook URL to register with the MAX platform.
     * When set and {@code autoRegister} is {@code true}, the URL is registered
     * on application startup. When {@code null}, auto-registration is skipped.
     */
    private String url;

    /**
     * Whether to automatically register the webhook URL with the MAX API on startup.
     */
    private boolean autoRegister = true;

    /**
     * Whether to automatically unregister the webhook URL on graceful shutdown.
     * Only effective when {@code autoRegister} is {@code true}.
     */
    private boolean autoUnregister = true;

    /**
     * List of update types to subscribe to.
     * When empty, all update types are received.
     */
    private List<String> updateTypes = new ArrayList<>();

    /**
     * Returns the bot access token.
     *
     * @return the token, or {@code null} if not configured
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the bot access token.
     *
     * @param token the bot access token
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Returns the webhook endpoint path.
     *
     * @return the path (defaults to {@code /max-bot/webhook})
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the webhook endpoint path.
     *
     * @param path the URL path; must start with {@code /}
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Returns the shared secret for header validation.
     *
     * @return the secret, or {@code null} if not configured
     */
    public String getSecret() {
        return secret;
    }

    /**
     * Sets the shared secret for header validation.
     *
     * @param secret the secret string, or {@code null} to disable validation
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * Returns the publicly reachable webhook URL.
     *
     * @return the URL, or {@code null} if not configured
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the publicly reachable webhook URL for registration with MAX.
     *
     * @param url the public URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Returns whether auto-registration is enabled.
     *
     * @return {@code true} if auto-registration is enabled
     */
    public boolean isAutoRegister() {
        return autoRegister;
    }

    /**
     * Sets whether to auto-register the webhook on startup.
     *
     * @param autoRegister {@code true} to enable
     */
    public void setAutoRegister(boolean autoRegister) {
        this.autoRegister = autoRegister;
    }

    /**
     * Returns whether auto-unregistration is enabled.
     *
     * @return {@code true} if auto-unregistration is enabled
     */
    public boolean isAutoUnregister() {
        return autoUnregister;
    }

    /**
     * Sets whether to auto-unregister the webhook on shutdown.
     *
     * @param autoUnregister {@code true} to enable
     */
    public void setAutoUnregister(boolean autoUnregister) {
        this.autoUnregister = autoUnregister;
    }

    /**
     * Returns an unmodifiable view of the update types to subscribe to.
     *
     * @return unmodifiable list of update types; empty list means all types
     */
    public List<String> getUpdateTypes() {
        return Collections.unmodifiableList(updateTypes);
    }

    /**
     * Sets the list of update types to subscribe to.
     *
     * @param updateTypes the update type strings
     */
    public void setUpdateTypes(List<String> updateTypes) {
        this.updateTypes = updateTypes;
    }
}
