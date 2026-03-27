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

/**
 * Spring Boot auto-configuration for receiving MAX Bot API webhook updates.
 *
 * <p>This module integrates with the host application's existing Spring MVC
 * web server — it does not create its own. When on the classpath, the
 * {@link ru.max.botapi.spring.webhook.MaxWebhookAutoConfiguration} registers
 * a {@link ru.max.botapi.spring.webhook.MaxWebhookController} endpoint and
 * optionally manages webhook registration/unregistration with the MAX platform
 * via {@link ru.max.botapi.spring.webhook.MaxWebhookRegistrar}.</p>
 *
 * <p>Configuration is externalized under the {@code max.bot.webhook} prefix
 * in {@code application.yml}. The user must provide a
 * {@link ru.max.botapi.core.UpdateHandler} bean. The {@code MaxSerializer}
 * is automatically extracted from the {@code MaxBotAPI} bean.</p>
 *
 * @see ru.max.botapi.spring.webhook.MaxWebhookAutoConfiguration
 * @see ru.max.botapi.spring.webhook.MaxWebhookProperties
 */
package ru.max.botapi.spring.webhook;
