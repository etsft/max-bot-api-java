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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.core.UpdateHandler;
import ru.max.botapi.longpolling.MaxLongPollingConsumer;
import ru.max.botapi.spring.MaxBotProperties;

/**
 * Spring Boot auto-configuration for the MAX Bot API long-polling integration.
 *
 * <p>Activates when {@link MaxLongPollingConsumer} is on the classpath and the
 * property {@code max.bot.mode} is set to {@code longpolling}.</p>
 *
 * <p>This configuration creates a {@link MaxLongPollingLifecycle} bean that
 * manages the consumer's lifecycle (start/stop) within the Spring context.
 * The user must provide an {@link UpdateHandler} bean containing the
 * application-specific update processing logic.</p>
 *
 * @see MaxLongPollingProperties
 * @see MaxLongPollingLifecycle
 * @see ru.max.botapi.spring.MaxBotMode
 */
@AutoConfiguration
@ConditionalOnClass(MaxLongPollingConsumer.class)
@ConditionalOnProperty(prefix = "max.bot", name = "mode",
        havingValue = "longpolling")
@EnableConfigurationProperties({MaxBotProperties.class,
        MaxLongPollingProperties.class})
public class MaxLongPollingAutoConfiguration {

    /**
     * Creates a {@link MaxBotAPI} from the configured bot token.
     *
     * <p>Only created when no {@code MaxBotAPI} bean is already present and
     * the {@code max.bot.longpolling.token} property is set.</p>
     *
     * @param properties the long-polling configuration
     * @return a new {@code MaxBotAPI} instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "max.bot.longpolling", name = "token")
    public MaxBotAPI maxBotAPI(MaxLongPollingProperties properties) {
        return MaxBotAPI.create(properties.getToken());
    }

    /**
     * Creates the long-polling lifecycle manager.
     *
     * <p>Only created when both a {@link MaxBotAPI} bean and an
     * {@link UpdateHandler} bean are present.</p>
     *
     * @param api        the MAX Bot API instance
     * @param handler    the user-provided update handler
     * @param properties the long-polling configuration
     * @return a new lifecycle manager
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({MaxBotAPI.class, UpdateHandler.class})
    public MaxLongPollingLifecycle maxLongPollingLifecycle(
            MaxBotAPI api,
            UpdateHandler handler,
            MaxLongPollingProperties properties) {
        return new MaxLongPollingLifecycle(api, handler, properties);
    }
}
