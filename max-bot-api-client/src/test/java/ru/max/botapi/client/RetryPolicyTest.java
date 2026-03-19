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

/**
 * Tests for {@link RetryPolicy}.
 */
class RetryPolicyTest {

    private final RetryPolicy policy = new RetryPolicy(3);

    @Test
    void shouldRetryOn429() {
        assertThat(policy.shouldRetry(429)).isTrue();
    }

    @Test
    void shouldRetryOn503() {
        assertThat(policy.shouldRetry(503)).isTrue();
    }

    @Test
    void shouldNotRetryOn400() {
        assertThat(policy.shouldRetry(400)).isFalse();
    }

    @Test
    void shouldNotRetryOn401() {
        assertThat(policy.shouldRetry(401)).isFalse();
    }

    @Test
    void shouldNotRetryOn404() {
        assertThat(policy.shouldRetry(404)).isFalse();
    }

    @Test
    void shouldNotRetryOn200() {
        assertThat(policy.shouldRetry(200)).isFalse();
    }

    @Test
    void shouldNotRetryOn500() {
        assertThat(policy.shouldRetry(500)).isFalse();
    }

    @Test
    void exponentialBackoff() {
        assertThat(policy.getDelay(0)).isEqualTo(Duration.ofSeconds(1));
        assertThat(policy.getDelay(1)).isEqualTo(Duration.ofSeconds(2));
        assertThat(policy.getDelay(2)).isEqualTo(Duration.ofSeconds(4));
    }

    @Test
    void maxRetriesConfigured() {
        assertThat(policy.maxRetries()).isEqualTo(3);
    }

    @Test
    void zeroRetries() {
        RetryPolicy noRetry = new RetryPolicy(0);
        assertThat(noRetry.maxRetries()).isEqualTo(0);
    }
}
