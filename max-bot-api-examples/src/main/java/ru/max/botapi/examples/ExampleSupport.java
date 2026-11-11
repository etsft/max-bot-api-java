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

package ru.max.botapi.examples;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Helpers shared by the examples.
 */
final class ExampleSupport {

    private static final long CLOSE_GRACE_SECONDS = 10;

    private ExampleSupport() {
    }

    /**
     * Blocks until the JVM is asked to stop (Ctrl+C, SIGTERM), then returns so the caller's
     * try-with-resources block closes the bot.
     *
     * <p>On Ctrl+C the JVM runs its shutdown hooks and then halts without returning from
     * {@code main}, so resources closed only at the end of a try block would never be closed.
     * The hook registered here releases the caller and waits (up to 10 seconds) for its thread
     * to finish closing them.</p>
     *
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    static void awaitShutdown() throws InterruptedException {
        CountDownLatch stopRequested = new CountDownLatch(1);
        Thread caller = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopRequested.countDown();
            try {
                caller.join(TimeUnit.SECONDS.toMillis(CLOSE_GRACE_SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "example-shutdown"));
        stopRequested.await();
        System.out.println("Stopping...");
    }
}
