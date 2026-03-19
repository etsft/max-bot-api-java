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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe token bucket rate limiter.
 *
 * <p>Limits the number of permits (requests) per second using an atomic
 * compare-and-swap approach. Tokens are refilled continuously based on
 * elapsed time.</p>
 */
public class RateLimiter {

    private final int permitsPerSecond;
    private final AtomicLong nextAvailableTime;

    /**
     * Creates a RateLimiter with the given permits per second.
     *
     * @param permitsPerSecond maximum permits per second
     */
    public RateLimiter(int permitsPerSecond) {
        if (permitsPerSecond <= 0) {
            throw new IllegalArgumentException("permitsPerSecond must be positive");
        }
        this.permitsPerSecond = permitsPerSecond;
        this.nextAvailableTime = new AtomicLong(System.nanoTime());
    }

    /**
     * Acquires a permit, blocking if necessary until one is available.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void acquire() throws InterruptedException {
        long intervalNanos = 1_000_000_000L / permitsPerSecond;
        while (true) {
            long now = System.nanoTime();
            long expected = nextAvailableTime.get();
            long next = Math.max(expected, now) + intervalNanos;
            if (nextAvailableTime.compareAndSet(expected, next)) {
                long waitNanos = Math.max(expected, now) - now;
                if (waitNanos > 0) {
                    long millis = waitNanos / 1_000_000;
                    int nanos = (int) (waitNanos % 1_000_000);
                    Thread.sleep(millis, nanos);
                }
                return;
            }
        }
    }

    /**
     * Returns the configured permits per second.
     *
     * @return permits per second
     */
    public int permitsPerSecond() {
        return permitsPerSecond;
    }
}
