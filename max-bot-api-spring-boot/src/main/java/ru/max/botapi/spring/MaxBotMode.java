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

package ru.max.botapi.spring;

/**
 * Defines the update delivery mode for the MAX Bot API Spring Boot integration.
 *
 * <p>Only one mode can be active at a time. Set via the {@code max.bot.mode}
 * property in {@code application.yml} or {@code application.properties}.</p>
 *
 * @see MaxBotProperties
 */
public enum MaxBotMode {

    /**
     * Receive updates via HTTP webhook callbacks.
     *
     * <p>Requires a publicly reachable HTTPS endpoint. The MAX platform
     * pushes updates to the configured URL.</p>
     */
    WEBHOOK,

    /**
     * Receive updates via long polling.
     *
     * <p>The application periodically calls {@code getUpdates} on the
     * MAX API. No public URL is required.</p>
     */
    LONGPOLLING,

    /**
     * Explicitly disables all bot update delivery.
     *
     * <p>Neither webhook nor long-polling auto-configuration will activate.
     * Use this when the bot starter is on the classpath but you do not want
     * any update delivery in a particular profile (e.g. testing).</p>
     */
    NONE
}
