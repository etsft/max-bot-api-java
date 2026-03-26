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
 * Spring Boot auto-configuration for receiving MAX Bot API updates via
 * long polling.
 *
 * <p>When on the classpath, the
 * {@link ru.max.botapi.spring.longpolling.MaxLongPollingAutoConfiguration}
 * creates a {@link ru.max.botapi.spring.longpolling.MaxLongPollingLifecycle}
 * that starts and stops the
 * {@link ru.max.botapi.longpolling.MaxLongPollingConsumer} with the
 * Spring application lifecycle.</p>
 *
 * <p>Configuration is externalized under the {@code max.bot.longpolling}
 * prefix in {@code application.yml}. The user must provide a
 * {@link ru.max.botapi.longpolling.MaxLongPollingConsumer.UpdateHandler}
 * bean.</p>
 *
 * @see ru.max.botapi.spring.longpolling.MaxLongPollingAutoConfiguration
 * @see ru.max.botapi.spring.longpolling.MaxLongPollingProperties
 */
package ru.max.botapi.spring.longpolling;
