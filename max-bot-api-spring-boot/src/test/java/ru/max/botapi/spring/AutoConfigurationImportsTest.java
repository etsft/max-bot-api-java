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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;

import ru.max.botapi.spring.longpolling.MaxLongPollingAutoConfiguration;
import ru.max.botapi.spring.webhook.MaxWebhookAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the {@code META-INF/spring/...AutoConfiguration.imports} file that Spring Boot reads
 * to find the auto-configurations: a typo there silently disables the starter.
 */
class AutoConfigurationImportsTest {

    @Test
    void registersEveryAutoConfiguration() {
        assertThat(ourImports()).containsExactlyInAnyOrder(
                MaxBotModeAutoConfiguration.class.getName(),
                MaxWebhookAutoConfiguration.class.getName(),
                MaxLongPollingAutoConfiguration.class.getName());
    }

    @Test
    void everyImportIsALoadableAutoConfiguration() throws ClassNotFoundException {
        for (String name : ourImports()) {
            assertThat(Class.forName(name).isAnnotationPresent(AutoConfiguration.class))
                    .as(name).isTrue();
        }
    }

    /** The candidates from this module's file; the other jars on the classpath have their own. */
    private List<String> ourImports() {
        List<String> names = new ArrayList<>();
        ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).forEach(names::add);
        names.removeIf(name -> !name.startsWith("ru.max.botapi."));
        return names;
    }
}
