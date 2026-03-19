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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link RateLimiter}.
 */
class RateLimiterTest {

    @Test
    void permitsConfigured() {
        RateLimiter limiter = new RateLimiter(30);
        assertThat(limiter.permitsPerSecond()).isEqualTo(30);
    }

    @Test
    void invalidPermitsThrows() {
        assertThatThrownBy(() -> new RateLimiter(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RateLimiter(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acquirePermitSucceeds() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(100);
        limiter.acquire();
        // No exception means success
    }

    @Test
    void threadSafetyWithMultipleThreads() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(1000);
        AtomicInteger acquired = new AtomicInteger(0);
        int threads = 10;
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            Thread.startVirtualThread(() -> {
                try {
                    limiter.acquire();
                    acquired.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(acquired.get()).isEqualTo(threads);
    }
}
