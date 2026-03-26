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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Auto-configuration that validates the {@code max.bot.mode} property.
 *
 * <p>Logs a warning at startup if no mode is configured, guiding the user
 * to set {@code max.bot.mode} to either {@code webhook} or
 * {@code longpolling}.</p>
 *
 * @see MaxBotMode
 * @see MaxBotProperties
 */
@AutoConfiguration
@EnableConfigurationProperties(MaxBotProperties.class)
public class MaxBotModeAutoConfiguration implements InitializingBean {

    private static final Logger LOG =
            LoggerFactory.getLogger(MaxBotModeAutoConfiguration.class);

    private final MaxBotProperties properties;

    /**
     * Creates the auto-configuration with the bound properties.
     *
     * @param properties the root MAX Bot properties
     */
    public MaxBotModeAutoConfiguration(MaxBotProperties properties) {
        this.properties = properties;
    }

    /**
     * Checks whether {@code max.bot.mode} is set and logs a warning
     * if it is not.
     */
    @Override
    public void afterPropertiesSet() {
        if (properties.getMode() == null) {
            LOG.warn("No max.bot.mode configured. Set max.bot.mode "
                    + "to 'webhook' or 'longpolling' to activate "
                    + "the corresponding auto-configuration.");
        }
    }
}
