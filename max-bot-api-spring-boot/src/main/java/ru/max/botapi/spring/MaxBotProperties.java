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

import org.springframework.boot.context.properties.ConfigurationProperties;

import ru.max.botapi.model.Nullable;

/**
 * Root configuration properties for the MAX Bot API Spring Boot integration.
 *
 * <p>Bound under the {@code max.bot} prefix. The {@code mode} property
 * selects the update delivery mechanism and must be set explicitly —
 * there is no default value.</p>
 *
 * <p>Example configuration:</p>
 * <pre>{@code
 * max:
 *   bot:
 *     mode: longpolling   # or "webhook"
 * }</pre>
 *
 * @see MaxBotMode
 */
@ConfigurationProperties(prefix = "max.bot")
public class MaxBotProperties {

    /**
     * The update delivery mode. Must be set to either {@code webhook} or
     * {@code longpolling}. When not set, neither auto-configuration activates.
     */
    @Nullable
    private MaxBotMode mode;

    /**
     * Returns the configured update delivery mode.
     *
     * @return the mode, or {@code null} if not configured
     */
    @Nullable
    public MaxBotMode getMode() {
        return mode;
    }

    /**
     * Sets the update delivery mode.
     *
     * @param mode the delivery mode
     */
    public void setMode(MaxBotMode mode) {
        this.mode = mode;
    }
}
