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

package ru.max.botapi.it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;

/**
 * Console interaction for the steps a bot cannot perform on its own — pressing an inline
 * button, writing to the bot, adding it to a chat.
 *
 * <p>When {@code MAX_IT_INTERACTIVE} is not enabled, every prompt aborts its test through
 * {@link Assumptions}, so the step is reported as skipped with the instruction it would have
 * shown rather than failing the run.</p>
 *
 * <p>Requires the {@code liveTest} task to hand the process a real stdin; see the module
 * README for how to run it.</p>
 */
public final class Prompts {

    private static final BufferedReader IN =
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    private static final long POLL_INTERVAL_MS = 200L;

    private Prompts() {
    }

    /**
     * Names the group chat the suite works in, for the {@code WHERE} line of a prompt.
     *
     * @return a description of the group chat, naming the variable that configures it
     */
    public static String groupChat() {
        return "the test GROUP CHAT - " + IntegrationConfig.CHAT_ID + " = "
                + IntegrationConfig.chatId();
    }

    /**
     * Asks the operator to perform something and to press Enter when they are done.
     *
     * @param where       where to do it, e.g. {@link #groupChat()}
     * @param instruction what to do there
     */
    public static void step(String where, String instruction) {
        ask(where, instruction + "\n  Press Enter when done");
    }

    /**
     * Asks the operator a question and returns what they typed.
     *
     * <p>Skips the step when the suite is not running interactively.</p>
     *
     * @param where    where the operator has to act, e.g. {@link #groupChat()}
     * @param question what to ask
     * @return the line the operator typed, trimmed
     */
    public static String ask(String where, String question) {
        Assumptions.assumeTrue(IntegrationConfig.interactive(),
                () -> "interactive step skipped (set " + IntegrationConfig.INTERACTIVE
                        + "=true to run it): " + question);

        Duration timeout = IntegrationConfig.promptTimeout();
        System.out.println();
        System.out.println("=== ACTION REQUIRED (" + timeout.toSeconds() + "s) ===");
        System.out.println("WHERE: " + where);
        System.out.println("WHAT:  " + question);
        System.out.flush();

        return readLine(timeout);
    }

    private static String readLine(Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        try {
            while (Instant.now().isBefore(deadline)) {
                if (IN.ready()) {
                    String line = IN.readLine();
                    return line == null ? "" : line.trim();
                }
                TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL_MS);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read from the console", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for console input", e);
        }
        throw new AssertionError("No console input within " + timeout.toSeconds()
                + "s. Run the task with --console=plain, or allow more time with "
                + "-Pit.promptTimeout=<seconds> (or " + IntegrationConfig.PROMPT_TIMEOUT + ").");
    }
}
