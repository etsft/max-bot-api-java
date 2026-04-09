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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import ru.max.botapi.model.UpdateType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaxLongPollingPropertiesTest {

    @Test
    void defaultValues() {
        MaxLongPollingProperties props = new MaxLongPollingProperties();

        assertThat(props.getToken()).isNull();
        assertThat(props.getPollTimeout()).isNull();
        assertThat(props.getUpdateTypes()).isEmpty();
    }

    @Test
    void settersAndGetters() {
        MaxLongPollingProperties props = new MaxLongPollingProperties();

        props.setToken("test-token");
        props.setPollTimeout(60);
        props.setUpdateTypes(
                List.of(UpdateType.MESSAGE_CREATED, UpdateType.MESSAGE_CALLBACK));

        assertThat(props.getToken()).isEqualTo("test-token");
        assertThat(props.getPollTimeout()).isEqualTo(60);
        assertThat(props.getUpdateTypes())
                .containsExactly(UpdateType.MESSAGE_CREATED, UpdateType.MESSAGE_CALLBACK);
    }

    @Test
    void getUpdateTypes_returnsUnmodifiableList() {
        MaxLongPollingProperties props = new MaxLongPollingProperties();
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
                        "max.bot.longpolling.token=yaml-test-token",
                        "max.bot.longpolling.poll-timeout=60",
                        "max.bot.longpolling.update-types=MESSAGE_CREATED")
                .run(context -> {
                    assertThat(context).hasSingleBean(
                            MaxLongPollingProperties.class);
                    MaxLongPollingProperties properties =
                            context.getBean(
                                    MaxLongPollingProperties.class);

                    assertThat(properties.getToken())
                            .isEqualTo("yaml-test-token");
                    assertThat(properties.getPollTimeout())
                            .isEqualTo(60);
                    assertThat(properties.getUpdateTypes())
                            .containsExactly(UpdateType.MESSAGE_CREATED);
                });
    }

    @Test
    void propertiesDefaultsWhenNoValuesSet() {
        new ApplicationContextRunner()
                .withUserConfiguration(BindingConfig.class)
                .run(context -> {
                    MaxLongPollingProperties properties =
                            context.getBean(
                                    MaxLongPollingProperties.class);

                    assertThat(properties.getToken()).isNull();
                    assertThat(properties.getPollTimeout()).isNull();
                    assertThat(properties.getUpdateTypes()).isEmpty();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MaxLongPollingProperties.class)
    static class BindingConfig {
    }
}
