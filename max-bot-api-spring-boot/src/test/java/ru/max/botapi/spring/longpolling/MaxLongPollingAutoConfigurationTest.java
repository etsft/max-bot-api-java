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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.client.MaxClientConfig;
import ru.max.botapi.client.queries.GetUpdatesQuery;
import ru.max.botapi.longpolling.MaxLongPollingConsumer;
import ru.max.botapi.model.UpdateList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaxLongPollingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            MaxLongPollingAutoConfiguration.class));

    @Test
    void allBeansCreated_whenModeIsLongpolling() {
        contextRunner
                .withUserConfiguration(HandlerConfig.class)
                .withPropertyValues(
                        "max.bot.mode=longpolling",
                        "max.bot.longpolling.token=test-token")
                .run(context -> {
                    assertThat(context).hasSingleBean(
                            MaxBotAPI.class);
                    assertThat(context).hasSingleBean(
                            MaxLongPollingLifecycle.class);
                    assertThat(context).hasSingleBean(
                            MaxLongPollingProperties.class);
                });
    }

    @Test
    void noBeansCreated_whenModeIsWebhook() {
        contextRunner
                .withUserConfiguration(HandlerConfig.class)
                .withPropertyValues(
                        "max.bot.mode=webhook",
                        "max.bot.longpolling.token=test-token")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(
                            MaxLongPollingLifecycle.class);
                });
    }

    @Test
    void noBeansCreated_whenModeNotSet() {
        contextRunner
                .withUserConfiguration(HandlerConfig.class)
                .withPropertyValues(
                        "max.bot.longpolling.token=test-token")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(
                            MaxLongPollingLifecycle.class);
                });
    }

    @Test
    void lifecycleNotCreated_withoutHandlerBean() {
        contextRunner
                .withPropertyValues(
                        "max.bot.mode=longpolling",
                        "max.bot.longpolling.token=test-token")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(
                            MaxLongPollingLifecycle.class);
                });
    }

    @Test
    void userDefinedMaxBotAPI_takesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomApiConfig.class)
                .withPropertyValues("max.bot.mode=longpolling")
                .run(context -> {
                    assertThat(context).hasSingleBean(
                            MaxBotAPI.class);
                    assertThat(context).getBean(MaxBotAPI.class)
                            .isSameAs(context.getBean(
                                    "customMaxBotAPI"));
                });
    }

    @Test
    void maxBotApiNotCreated_whenTokenNotSet() {
        contextRunner
                .withUserConfiguration(HandlerConfig.class)
                .withPropertyValues("max.bot.mode=longpolling")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(
                            MaxBotAPI.class);
                    assertThat(context).doesNotHaveBean(
                            MaxLongPollingLifecycle.class);
                });
    }

    @Test
    void propertiesBindCorrectly() {
        contextRunner
                .withUserConfiguration(HandlerConfig.class)
                .withPropertyValues(
                        "max.bot.mode=longpolling",
                        "max.bot.longpolling.token=my-token",
                        "max.bot.longpolling.poll-timeout=60",
                        "max.bot.longpolling.update-types="
                                + "message_created")
                .run(context -> {
                    MaxLongPollingProperties props =
                            context.getBean(
                                    MaxLongPollingProperties.class);
                    assertThat(props.getToken())
                            .isEqualTo("my-token");
                    assertThat(props.getPollTimeout())
                            .isEqualTo(60);
                    assertThat(props.getUpdateTypes())
                            .containsExactly("message_created");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class HandlerConfig {

        @Bean
        MaxLongPollingConsumer.UpdateHandler updateHandler() {
            return update -> { };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomApiConfig {

        @Bean
        MaxBotAPI customMaxBotAPI() {
            MaxBotAPI api = mock(MaxBotAPI.class);
            when(api.config()).thenReturn(
                    MaxClientConfig.defaults());
            GetUpdatesQuery query = mock(
                    GetUpdatesQuery.class);
            when(api.getUpdates()).thenReturn(query);
            when(query.timeout(
                    org.mockito.ArgumentMatchers.anyInt()))
                    .thenReturn(query);
            when(query.execute()).thenReturn(
                    new UpdateList(List.of(), null));
            return api;
        }

        @Bean
        MaxLongPollingConsumer.UpdateHandler customHandler() {
            return update -> { };
        }
    }
}
