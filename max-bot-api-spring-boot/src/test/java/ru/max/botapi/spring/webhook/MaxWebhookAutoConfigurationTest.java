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

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.core.UpdateHandler;
import ru.max.botapi.jackson.JacksonMaxSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MaxWebhookAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            MaxWebhookAutoConfiguration.class,
                            DispatcherServletAutoConfiguration.class));

    @Test
    void allBeansCreated_whenModeIsWebhook() {
        contextRunner
                .withUserConfiguration(
                        HandlerConfig.class, SerializerConfig.class)
                .withPropertyValues(
                        "max.bot.mode=webhook",
                        "max.bot.webhook.token=test-token")
                .run(context -> {
                    assertThat(context).hasSingleBean(
                            MaxWebhookController.class);
                    assertThat(context).hasSingleBean(MaxBotAPI.class);
                    assertThat(context).hasSingleBean(
                            MaxWebhookRegistrar.class);
                    assertThat(context).hasSingleBean(
                            MaxWebhookProperties.class);
                });
    }

    @Test
    void noBeansCreated_whenModeIsLongpolling() {
        contextRunner
                .withUserConfiguration(
                        HandlerConfig.class, SerializerConfig.class)
                .withPropertyValues(
                        "max.bot.mode=longpolling",
                        "max.bot.webhook.token=test-token")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(
                            MaxWebhookController.class);
                    assertThat(context).doesNotHaveBean(
                            MaxWebhookRegistrar.class);
                });
    }

    @Test
    void noBeansCreated_whenModeNotSet() {
        contextRunner
                .withUserConfiguration(
                        HandlerConfig.class, SerializerConfig.class)
                .withPropertyValues(
                        "max.bot.webhook.token=test-token")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(
                            MaxWebhookController.class);
                    assertThat(context).doesNotHaveBean(
                            MaxWebhookRegistrar.class);
                });
    }

    @Test
    void noBeansCreated_whenModeIsNone() {
        contextRunner
                .withUserConfiguration(
                        HandlerConfig.class, SerializerConfig.class)
                .withPropertyValues(
                        "max.bot.mode=none",
                        "max.bot.webhook.token=test-token")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(
                            MaxWebhookController.class);
                    assertThat(context).doesNotHaveBean(
                            MaxWebhookRegistrar.class);
                });
    }

    @Test
    void controllerNotCreated_withoutHandlerBean() {
        contextRunner
                .withUserConfiguration(SerializerConfig.class)
                .withPropertyValues(
                        "max.bot.mode=webhook",
                        "max.bot.webhook.token=test-token")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(
                            MaxWebhookController.class);
                });
    }

    @Test
    void controllerCreated_withoutExplicitSerializer() {
        contextRunner
                .withUserConfiguration(HandlerConfig.class)
                .withPropertyValues(
                        "max.bot.mode=webhook",
                        "max.bot.webhook.token=test-token")
                .run(context -> {
                    assertThat(context).hasSingleBean(
                            MaxWebhookController.class);
                    assertThat(context).hasSingleBean(
                            MaxSerializer.class);
                });
    }

    @Test
    void userDefinedMaxBotAPI_takesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomApiConfig.class)
                .withPropertyValues("max.bot.mode=webhook")
                .run(context -> {
                    assertThat(context).hasSingleBean(MaxBotAPI.class);
                    assertThat(context).getBean(MaxBotAPI.class)
                            .isSameAs(context.getBean("customMaxBotAPI"));
                });
    }

    @Test
    void registrarNotCreated_whenAutoRegisterDisabled() {
        contextRunner
                .withUserConfiguration(
                        HandlerConfig.class, SerializerConfig.class)
                .withPropertyValues(
                        "max.bot.mode=webhook",
                        "max.bot.webhook.token=test-token",
                        "max.bot.webhook.auto-register=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(
                            MaxWebhookRegistrar.class);
                    assertThat(context).hasSingleBean(
                            MaxWebhookController.class);
                });
    }

    @Test
    void maxBotApiNotCreated_whenTokenNotSet() {
        contextRunner
                .withUserConfiguration(
                        HandlerConfig.class, SerializerConfig.class)
                .withPropertyValues("max.bot.mode=webhook")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MaxBotAPI.class);
                    assertThat(context).doesNotHaveBean(
                            MaxWebhookRegistrar.class);
                });
    }

    @Test
    void propertiesBindCorrectly() {
        contextRunner
                .withUserConfiguration(
                        HandlerConfig.class, SerializerConfig.class)
                .withPropertyValues(
                        "max.bot.mode=webhook",
                        "max.bot.webhook.token=my-token",
                        "max.bot.webhook.path=/custom/path",
                        "max.bot.webhook.secret=my-secret",
                        "max.bot.webhook.url="
                                + "https://test.example.com/hook")
                .run(context -> {
                    MaxWebhookProperties props =
                            context.getBean(MaxWebhookProperties.class);
                    assertThat(props.getToken())
                            .isEqualTo("my-token");
                    assertThat(props.getPath())
                            .isEqualTo("/custom/path");
                    assertThat(props.getSecret())
                            .isEqualTo("my-secret");
                    assertThat(props.getUrl()).isEqualTo(
                            "https://test.example.com/hook");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class HandlerConfig {

        @Bean
        UpdateHandler webhookHandler() {
            return update -> { };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class SerializerConfig {

        @Bean
        MaxSerializer maxSerializer() {
            return new JacksonMaxSerializer();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomApiConfig {

        @Bean
        MaxBotAPI customMaxBotAPI() {
            return mock(MaxBotAPI.class);
        }

        @Bean
        UpdateHandler customWebhookHandler() {
            return update -> { };
        }

        @Bean
        MaxSerializer customMaxSerializer() {
            return new JacksonMaxSerializer();
        }
    }
}
