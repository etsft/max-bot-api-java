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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.spring.MaxBotProperties;
import ru.max.botapi.webhook.WebhookHandler;

/**
 * Spring Boot auto-configuration for the MAX Bot API webhook integration.
 *
 * <p>Activates when {@link DispatcherServlet} (Spring MVC) and
 * {@link WebhookHandler} are on the classpath and the property
 * {@code max.bot.mode} is set to {@code webhook}.</p>
 *
 * <p>This configuration does not create its own web server — it registers
 * a {@link MaxWebhookController} into the host application's existing
 * {@link DispatcherServlet}. All beans are overridable via
 * {@code @ConditionalOnMissingBean}.</p>
 *
 * <p>The user must provide a {@link WebhookHandler} bean — this is intentional,
 * as the handler contains application-specific business logic.</p>
 *
 * @see MaxWebhookProperties
 * @see MaxWebhookController
 * @see MaxWebhookRegistrar
 * @see ru.max.botapi.spring.MaxBotMode
 */
@AutoConfiguration
@ConditionalOnClass({DispatcherServlet.class, WebhookHandler.class})
@ConditionalOnProperty(prefix = "max.bot", name = "mode", havingValue = "webhook")
@EnableConfigurationProperties({MaxBotProperties.class, MaxWebhookProperties.class})
public class MaxWebhookAutoConfiguration {

    /**
     * Creates a {@link MaxBotAPI} from the configured bot token.
     *
     * <p>Only created when no {@code MaxBotAPI} bean is already present and the
     * {@code max.bot.webhook.token} property is set.</p>
     *
     * @param properties the webhook configuration
     * @return a new {@code MaxBotAPI} instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "max.bot.webhook", name = "token")
    public MaxBotAPI maxBotAPI(MaxWebhookProperties properties) {
        return MaxBotAPI.create(properties.getToken());
    }

    /**
     * Creates the webhook controller that handles incoming POST requests.
     *
     * <p>Only created when a {@link WebhookHandler} bean is present and no
     * custom {@code MaxWebhookController} bean exists.</p>
     *
     * @param handler    the user-provided update handler
     * @param serializer the JSON serializer
     * @param properties the webhook configuration
     * @return a new controller instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({WebhookHandler.class, MaxSerializer.class})
    public MaxWebhookController maxWebhookController(
            WebhookHandler handler,
            MaxSerializer serializer,
            MaxWebhookProperties properties) {
        return new MaxWebhookController(handler, serializer, properties);
    }

    /**
     * Creates the lifecycle registrar that registers/unregisters the webhook
     * URL with the MAX platform.
     *
     * <p>Only created when a {@link MaxBotAPI} bean is present and
     * {@code max.bot.webhook.auto-register} is {@code true} (the default).</p>
     *
     * @param api        the MAX Bot API instance
     * @param properties the webhook configuration
     * @return a new registrar instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MaxBotAPI.class)
    @ConditionalOnProperty(
            prefix = "max.bot.webhook",
            name = "auto-register",
            havingValue = "true",
            matchIfMissing = true)
    public MaxWebhookRegistrar maxWebhookRegistrar(
            MaxBotAPI api,
            MaxWebhookProperties properties) {
        return new MaxWebhookRegistrar(api, properties);
    }
}
