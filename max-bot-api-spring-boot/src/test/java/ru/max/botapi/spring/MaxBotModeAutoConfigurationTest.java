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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class MaxBotModeAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            MaxBotModeAutoConfiguration.class));

    @Test
    void logsWarning_whenModeNotSet(CapturedOutput output) {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(
                    MaxBotModeAutoConfiguration.class);
            assertThat(output).contains(
                    "No max.bot.mode configured");
        });
    }

    @Test
    void noWarning_whenModeIsWebhook(CapturedOutput output) {
        contextRunner
                .withPropertyValues("max.bot.mode=webhook")
                .run(context -> {
                    assertThat(context).hasSingleBean(
                            MaxBotModeAutoConfiguration.class);
                    assertThat(output).doesNotContain(
                            "No max.bot.mode configured");
                });
    }

    @Test
    void noWarning_whenModeIsLongpolling(CapturedOutput output) {
        contextRunner
                .withPropertyValues("max.bot.mode=longpolling")
                .run(context -> {
                    assertThat(context).hasSingleBean(
                            MaxBotModeAutoConfiguration.class);
                    assertThat(output).doesNotContain(
                            "No max.bot.mode configured");
                });
    }
}
