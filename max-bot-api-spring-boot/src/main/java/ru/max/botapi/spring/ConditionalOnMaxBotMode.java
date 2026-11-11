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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Matches when {@code max.bot.mode} binds to the given {@link MaxBotMode}.
 *
 * <p>The property is bound the same way as {@link MaxBotProperties#getMode()}, so every
 * spelling Spring Boot accepts for the enum matches: {@code longpolling}, {@code long-polling},
 * {@code LONGPOLLING} and {@code longPolling} all select {@link MaxBotMode#LONGPOLLING}. A plain
 * {@code @ConditionalOnProperty(havingValue = "longpolling")} compares the raw string and
 * would silently leave the mode off for {@code long-polling}.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@Conditional(OnMaxBotModeCondition.class)
public @interface ConditionalOnMaxBotMode {

    /**
     * The mode that must be configured.
     *
     * @return the required mode
     */
    MaxBotMode value();
}
