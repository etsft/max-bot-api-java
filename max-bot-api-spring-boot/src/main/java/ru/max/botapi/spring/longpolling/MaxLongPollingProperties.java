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

package ru.max.botapi.spring.longpolling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the MAX Bot API long-polling integration.
 *
 * <p>All properties are bound under the {@code max.bot.longpolling} prefix in
 * {@code application.yml} or {@code application.properties}.</p>
 *
 * <p>Example configuration:</p>
 * <pre>{@code
 * max:
 *   bot:
 *     mode: longpolling
 *     longpolling:
 *       token: "your-bot-token"
 *       poll-timeout: 30
 *       update-types:
 *         - message_created
 *         - message_callback
 * }</pre>
 */
@ConfigurationProperties(prefix = "max.bot.longpolling")
public class MaxLongPollingProperties {

    /**
     * Bot access token for the MAX Bot API.
     * Used to create a {@code MaxBotAPI} bean if one is not already provided.
     */
    private String token;

    /**
     * Long-poll timeout in seconds.
     * If not set, defaults to the value from {@code MaxClientConfig} (30 seconds).
     */
    private Integer pollTimeout;

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
     * Returns the long-poll timeout in seconds.
     *
     * @return the timeout, or {@code null} to use the default
     */
    public Integer getPollTimeout() {
        return pollTimeout;
    }

    /**
     * Sets the long-poll timeout in seconds.
     *
     * @param pollTimeout the timeout in seconds; must be positive
     */
    public void setPollTimeout(Integer pollTimeout) {
        this.pollTimeout = pollTimeout;
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
