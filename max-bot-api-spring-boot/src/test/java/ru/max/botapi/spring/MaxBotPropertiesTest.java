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

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class MaxBotPropertiesTest {

    @Test
    void defaultValues() {
        MaxBotProperties props = new MaxBotProperties();
        assertThat(props.getMode()).isNull();
    }

    @Test
    void setterAndGetter() {
        MaxBotProperties props = new MaxBotProperties();

        props.setMode(MaxBotMode.WEBHOOK);
        assertThat(props.getMode()).isEqualTo(MaxBotMode.WEBHOOK);

        props.setMode(MaxBotMode.LONGPOLLING);
        assertThat(props.getMode()).isEqualTo(MaxBotMode.LONGPOLLING);
    }

    @Test
    void propertiesBindFromEnvironment_webhook() {
        new ApplicationContextRunner()
                .withUserConfiguration(BindingConfig.class)
                .withPropertyValues("max.bot.mode=webhook")
                .run(context -> {
                    MaxBotProperties properties =
                            context.getBean(MaxBotProperties.class);
                    assertThat(properties.getMode())
                            .isEqualTo(MaxBotMode.WEBHOOK);
                });
    }

    @Test
    void propertiesBindFromEnvironment_longpolling() {
        new ApplicationContextRunner()
                .withUserConfiguration(BindingConfig.class)
                .withPropertyValues("max.bot.mode=longpolling")
                .run(context -> {
                    MaxBotProperties properties =
                            context.getBean(MaxBotProperties.class);
                    assertThat(properties.getMode())
                            .isEqualTo(MaxBotMode.LONGPOLLING);
                });
    }

    @Test
    void propertiesDefaultsWhenNoValuesSet() {
        new ApplicationContextRunner()
                .withUserConfiguration(BindingConfig.class)
                .run(context -> {
                    MaxBotProperties properties =
                            context.getBean(MaxBotProperties.class);
                    assertThat(properties.getMode()).isNull();
                });
    }

    @Test
    void modeEnum_hasExpectedValues() {
        assertThat(MaxBotMode.values())
                .containsExactly(MaxBotMode.WEBHOOK,
                        MaxBotMode.LONGPOLLING);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MaxBotProperties.class)
    static class BindingConfig {
    }
}
