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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import ru.max.botapi.model.UpdateType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaxWebhookPropertiesTest {

    @Test
    void defaultValues() {
        MaxWebhookProperties props = new MaxWebhookProperties();

        assertThat(props.getToken()).isNull();
        assertThat(props.getPath()).isEqualTo("/max-bot/webhook");
        assertThat(props.getSecret()).isNull();
        assertThat(props.getUrl()).isNull();
        assertThat(props.isAutoRegister()).isTrue();
        assertThat(props.isAutoUnregister()).isTrue();
        assertThat(props.getUpdateTypes()).isEmpty();
    }

    @Test
    void settersAndGetters() {
        MaxWebhookProperties props = new MaxWebhookProperties();

        props.setToken("test-token");
        props.setPath("/custom/path");
        props.setSecret("my-secret");
        props.setUrl("https://example.com/webhook");
        props.setAutoRegister(false);
        props.setAutoUnregister(false);
        props.setUpdateTypes(List.of(UpdateType.MESSAGE_CREATED, UpdateType.MESSAGE_CALLBACK));

        assertThat(props.getToken()).isEqualTo("test-token");
        assertThat(props.getPath()).isEqualTo("/custom/path");
        assertThat(props.getSecret()).isEqualTo("my-secret");
        assertThat(props.getUrl()).isEqualTo("https://example.com/webhook");
        assertThat(props.isAutoRegister()).isFalse();
        assertThat(props.isAutoUnregister()).isFalse();
        assertThat(props.getUpdateTypes())
                .containsExactly(UpdateType.MESSAGE_CREATED, UpdateType.MESSAGE_CALLBACK);
    }

    @Test
    void getUpdateTypes_returnsUnmodifiableList() {
        MaxWebhookProperties props = new MaxWebhookProperties();
        props.setUpdateTypes(List.of(UpdateType.MESSAGE_CREATED));

        List<UpdateType> returned = props.getUpdateTypes();
        assertThat(returned).containsExactly(UpdateType.MESSAGE_CREATED);
        assertThatThrownBy(() -> returned.add(UpdateType.MESSAGE_CALLBACK))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void propertiesBindFromEnvironment() {
        new ApplicationContextRunner()
                .withUserConfiguration(BindingConfig.class)
                .withPropertyValues(
                        "max.bot.webhook.token=yaml-test-token",
                        "max.bot.webhook.path=/yaml/webhook",
                        "max.bot.webhook.secret=yaml-secret",
                        "max.bot.webhook.url=https://yaml.example.com/webhook",
                        "max.bot.webhook.auto-register=false",
                        "max.bot.webhook.auto-unregister=false",
                        "max.bot.webhook.update-types=MESSAGE_CREATED")
                .run(context -> {
                    assertThat(context).hasSingleBean(
                            MaxWebhookProperties.class);
                    MaxWebhookProperties properties =
                            context.getBean(MaxWebhookProperties.class);

                    assertThat(properties.getToken())
                            .isEqualTo("yaml-test-token");
                    assertThat(properties.getPath())
                            .isEqualTo("/yaml/webhook");
                    assertThat(properties.getSecret())
                            .isEqualTo("yaml-secret");
                    assertThat(properties.getUrl())
                            .isEqualTo("https://yaml.example.com/webhook");
                    assertThat(properties.isAutoRegister()).isFalse();
                    assertThat(properties.isAutoUnregister()).isFalse();
                    assertThat(properties.getUpdateTypes())
                            .containsExactly(UpdateType.MESSAGE_CREATED);
                });
    }

    @Test
    void propertiesDefaultsWhenNoValuesSet() {
        new ApplicationContextRunner()
                .withUserConfiguration(BindingConfig.class)
                .run(context -> {
                    MaxWebhookProperties properties =
                            context.getBean(MaxWebhookProperties.class);

                    assertThat(properties.getToken()).isNull();
                    assertThat(properties.getPath())
                            .isEqualTo("/max-bot/webhook");
                    assertThat(properties.getSecret()).isNull();
                    assertThat(properties.getUrl()).isNull();
                    assertThat(properties.isAutoRegister()).isTrue();
                    assertThat(properties.isAutoUnregister()).isTrue();
                    assertThat(properties.getUpdateTypes()).isEmpty();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MaxWebhookProperties.class)
    static class BindingConfig {
    }
}
