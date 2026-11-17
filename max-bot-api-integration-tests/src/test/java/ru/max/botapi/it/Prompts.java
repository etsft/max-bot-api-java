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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assumptions;

import ru.max.botapi.client.MaxApiException;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.client.MaxClientException;

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

    /** The variables naming chats, in the order they are announced. */
    private static final String[] CHAT_VARIABLES = {
        IntegrationConfig.CHAT_ID,
        IntegrationConfig.CHANNEL_ID,
        IntegrationConfig.DISPOSABLE_CHAT_ID,
        IntegrationConfig.DELETABLE_CHAT_ID,
    };

    /** Chat titles by id; empty when the lookup failed or the chat has no title. */
    private static final Map<Long, Optional<String>> TITLES = new ConcurrentHashMap<>();

    /** Set once the titles have been looked up and announced. */
    private static final AtomicBoolean TITLES_LOADED = new AtomicBoolean();

    private Prompts() {
    }

    /**
     * Looks up the titles of every configured chat and announces them, once per run.
     *
     * <p>Called before any test runs, so that a prompt never makes a request of its own: a
     * {@code getChat} in the middle of a test would overwrite what {@link RecordingSerializer}
     * and {@link RecordingTransport} hold for the call the test is about. A failed lookup is
     * remembered as "no title" and never fails anything. Only the first call does anything;
     * every test class calls it, and the line {@code Live suite chats: ...} is printed once.</p>
     *
     * @param api the API to look the chats up with
     */
    public static void loadChatTitles(MaxBotAPI api) {
        if (!TITLES_LOADED.compareAndSet(false, true)) {
            return;
        }
        Map<String, Long> configured = new LinkedHashMap<>();
        for (String variable : CHAT_VARIABLES) {
            IntegrationConfig.optional(variable).ifPresent(raw -> {
                try {
                    configured.put(variable, Long.parseLong(raw));
                } catch (NumberFormatException e) {
                    // reported by IntegrationConfig when a test actually needs it
                }
            });
        }
        StringBuilder out = new StringBuilder();
        configured.forEach((variable, chatId) -> {
            TITLES.computeIfAbsent(chatId, id -> fetchTitle(api, id));
            out.append(out.isEmpty() ? "" : ", ").append(describe(variable, chatId));
        });
        if (!out.isEmpty()) {
            System.out.println("Live suite chats: " + out);
        }
    }

    private static Optional<String> fetchTitle(MaxBotAPI api, long chatId) {
        try {
            return Optional.ofNullable(api.getChat(chatId).execute().title())
                    .filter(title -> !title.isBlank());
        } catch (MaxApiException | MaxClientException e) {
            // A response the model cannot read is not caught: that is a model bug to fix.
            System.out.println("Could not look up the title of chat " + chatId + ": " + e);
            return Optional.empty();
        }
    }

    /**
     * Describes a configured chat by its variable, id and, when known, title.
     *
     * @param variable the variable that configures the chat
     * @param chatId   its value
     * @return e.g. {@code MAX_IT_CHAT_ID = -1 "Tests"}
     */
    private static String describe(String variable, long chatId) {
        return variable + " = " + chatId + titleSuffix(chatId);
    }

    private static String titleSuffix(long chatId) {
        return TITLES.getOrDefault(chatId, Optional.empty())
                .map(title -> " \"" + title + "\"")
                .orElse("");
    }

    /**
     * Names the group chat the suite works in, for the {@code WHERE} line of a prompt.
     *
     * @return a description of the group chat, naming its title when known and the variable
     *         that configures it
     */
    public static String groupChat() {
        long chatId = IntegrationConfig.chatId();
        return "the test GROUP CHAT" + titleSuffix(chatId) + " - " + IntegrationConfig.CHAT_ID
                + " = " + chatId;
    }

    /**
     * Names the channel the comment tests work in, for the {@code WHERE} line of a prompt.
     *
     * @return a description of the channel, naming its title when known and the variable
     *         that configures it
     */
    public static String channel() {
        long channelId = IntegrationConfig.channelId();
        return "the test CHANNEL" + titleSuffix(channelId) + " - " + IntegrationConfig.CHANNEL_ID
                + " = " + channelId;
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
        discardPendingInput();
        System.out.println();
        System.out.println("=== ACTION REQUIRED (" + timeout.toSeconds() + "s) ===");
        System.out.println("WHERE: " + where);
        System.out.println("WHAT:  " + question);
        System.out.flush();

        return readLine(timeout);
    }

    /**
     * Drops whatever was typed before this prompt was shown.
     *
     * <p>An Enter pressed after an earlier prompt had already timed out would otherwise sit in
     * the buffer and confirm this one before the operator has even read it.</p>
     */
    private static void discardPendingInput() {
        try {
            while (IN.ready()) {
                String stale = IN.readLine();
                if (stale == null) {
                    return;
                }
                System.out.println("(ignoring input typed before this prompt: \"" + stale + "\")");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read from the console", e);
        }
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
