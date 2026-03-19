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

package ru.max.botapi.client;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MaxClientConfig}.
 */
class MaxClientConfigTest {

    @Test
    void defaultsHaveExpectedValues() {
        MaxClientConfig config = MaxClientConfig.defaults();
        assertThat(config.baseUrl()).isEqualTo("https://platform-api.max.ru");
        assertThat(config.connectTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.longPollTimeout()).isEqualTo(Duration.ofSeconds(90));
        assertThat(config.maxRetries()).isEqualTo(3);
        assertThat(config.enableRateLimiting()).isTrue();
        assertThat(config.maxRequestsPerSecond()).isEqualTo(30);
    }

    @Test
    void builderProducesCorrectConfig() {
        MaxClientConfig config = MaxClientConfig.builder()
                .baseUrl("https://custom.api.ru")
                .connectTimeout(Duration.ofSeconds(5))
                .requestTimeout(Duration.ofSeconds(15))
                .longPollTimeout(Duration.ofSeconds(60))
                .maxRetries(5)
                .enableRateLimiting(false)
                .maxRequestsPerSecond(50)
                .build();
        assertThat(config.baseUrl()).isEqualTo("https://custom.api.ru");
        assertThat(config.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(15));
        assertThat(config.longPollTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(config.maxRetries()).isEqualTo(5);
        assertThat(config.enableRateLimiting()).isFalse();
        assertThat(config.maxRequestsPerSecond()).isEqualTo(50);
    }

    @Test
    void builderDefaultsMatchStaticDefaults() {
        MaxClientConfig fromBuilder = MaxClientConfig.builder().build();
        MaxClientConfig defaults = MaxClientConfig.defaults();
        assertThat(fromBuilder).isEqualTo(defaults);
    }

    @Test
    void nullBaseUrlThrows() {
        assertThatThrownBy(() -> MaxClientConfig.builder().baseUrl(null).build())
                .isInstanceOf(NullPointerException.class);
    }
}
