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

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition behind {@link ConditionalOnMaxBotMode}.
 *
 * <p>A value that does not bind to {@link MaxBotMode} is a non-match here; binding
 * {@link MaxBotProperties} then fails startup with a message naming the bad value.</p>
 */
class OnMaxBotModeCondition extends SpringBootCondition {

    static final String PROPERTY = "max.bot.mode";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context,
            AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes =
                metadata.getAnnotationAttributes(ConditionalOnMaxBotMode.class.getName());
        MaxBotMode required = (MaxBotMode) attributes.get("value");
        MaxBotMode actual;
        try {
            actual = Binder.get(context.getEnvironment())
                    .bind(PROPERTY, MaxBotMode.class)
                    .orElse(null);
        } catch (BindException e) {
            return ConditionOutcome.noMatch(PROPERTY + " has an invalid value");
        }
        if (actual == required) {
            return ConditionOutcome.match(PROPERTY + " is " + required);
        }
        return ConditionOutcome.noMatch(PROPERTY + " is " + actual + ", not " + required);
    }
}
