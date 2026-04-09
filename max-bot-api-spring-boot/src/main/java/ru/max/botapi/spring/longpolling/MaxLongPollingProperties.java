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

import ru.max.botapi.model.UpdateType;

/**
 * Configuration properties for the MAX Bot API long-polling integration.
 *
 * <p>All properties are bound under the {@code max.bot.longpolling} prefix in
 * {@code application.yml} or {@code application.properties}.</p>
 *
 * <p>Update types may be specified either as raw strings or as {@link UpdateType}
 * enum constants. Both forms are accepted in YAML:</p>
 *
 * <pre>{@code
 * max:
 *   bot:
 *     mode: longpolling
 *     longpolling:
 *       token: "your-bot-token"
 *       poll-timeout: 30
 *       update-types:
 *         - MESSAGE_CREATED
 *         - MESSAGE_CALLBACK
 * }</pre>
 *
 * <p>Or using the raw API strings (also supported):</p>
 *
 * <pre>{@code
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
     * List of update types to filter.
     * When empty, all update types are received.
     */
    private List<UpdateType> updateTypes = new ArrayList<>();

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
     * Returns an unmodifiable view of the update types to filter.
     *
     * @return unmodifiable list of update type strings; empty list means all types
     */
    public List<UpdateType> getUpdateTypes() {
        return Collections.unmodifiableList(updateTypes);
    }

    /**
     * Sets the list of update types to filter.
     * Accepts {@link UpdateType} enum constants.
     *
     * @param updateTypes the update type strings
     */
    public void setUpdateTypes(List<UpdateType> updateTypes) {
        this.updateTypes = updateTypes;
    }

}

