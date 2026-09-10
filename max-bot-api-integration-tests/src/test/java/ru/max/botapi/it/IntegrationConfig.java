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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Assumptions;

/**
 * Environment-driven configuration for the live suite.
 *
 * <p>Every value comes from an environment variable so that no secret ever reaches the
 * repository. Optional values are exposed as {@link Optional}; the {@code require*} helpers
 * abort the calling test with a readable reason when a value is missing, which JUnit reports
 * as <em>skipped</em> rather than failed.</p>
 */
public final class IntegrationConfig {

    /** Bot access token. Without it the whole suite is disabled. */
    public static final String TOKEN = "MAX_BOT_TOKEN";

    /** Main test chat: messages, pins, chat properties. */
    public static final String CHAT_ID = "MAX_IT_CHAT_ID";

    /** A real user id, used for direct messages and membership operations. */
    public static final String USER_ID = "MAX_IT_USER_ID";

    /** Public chat link for {@code getChatByLink}. */
    public static final String CHAT_LINK = "MAX_IT_CHAT_LINK";

    /** Overrides the API base URL (staging environments). */
    public static final String BASE_URL = "MAX_IT_BASE_URL";

    /** Set to {@code true} to enable steps that need a human at the console. */
    public static final String INTERACTIVE = "MAX_IT_INTERACTIVE";

    /**
     * How long an interactive prompt waits for input, in seconds. Also settable on the command
     * line as {@code -Pit.promptTimeout=<seconds>}, which the {@code liveTest} task passes in
     * as this variable.
     */
    public static final String PROMPT_TIMEOUT = "MAX_IT_PROMPT_TIMEOUT_SECONDS";

    /** Set to {@code true} to enable irreversible operations on the disposable chat. */
    public static final String DESTRUCTIVE = "MAX_IT_DESTRUCTIVE";

    /** Throwaway chat for membership, admin and leave operations. */
    public static final String DISPOSABLE_CHAT_ID = "MAX_IT_DISPOSABLE_CHAT_ID";

    /** A second throwaway chat, deleted outright by {@code deleteChat}. */
    public static final String DELETABLE_CHAT_ID = "MAX_IT_DELETABLE_CHAT_ID";

    /** Public HTTPS URL proxied to the local webhook server. */
    public static final String WEBHOOK_URL = "MAX_IT_WEBHOOK_URL";

    /** Local port the webhook server binds to. */
    public static final String WEBHOOK_PORT = "MAX_IT_WEBHOOK_PORT";

    /** Secret expected in the {@code X-Max-Bot-Api-Secret} header. */
    public static final String WEBHOOK_SECRET = "MAX_IT_WEBHOOK_SECRET";

    /** Channel the bot administers, used by the comment tests. */
    public static final String CHANNEL_ID = "MAX_IT_CHANNEL_ID";

    /** Identifier (mid) of a post in {@link #CHANNEL_ID} that accepts comments. */
    public static final String POST_ID = "MAX_IT_POST_ID";

    /** Path to a real video file for upload tests. */
    public static final String VIDEO_PATH = "MAX_IT_VIDEO_PATH";

    /** Path to a real audio file for upload tests. */
    public static final String AUDIO_PATH = "MAX_IT_AUDIO_PATH";

    /**
     * Path this suite serves its webhook on, appended to {@link #WEBHOOK_URL} when that has no
     * path of its own. It also identifies the suite's own subscriptions, so a leftover from an
     * interrupted run can be recognised and removed.
     */
    public static final String WEBHOOK_PATH = "/max-bot/live-test";

    private static final int DEFAULT_WEBHOOK_PORT = 18080;

    private static final long DEFAULT_PROMPT_TIMEOUT_SECONDS = 30L;

    private IntegrationConfig() {
    }

    /**
     * Returns the bot access token, aborting the test if it is not configured.
     *
     * @return the token
     */
    public static String token() {
        return requireString(TOKEN);
    }

    /**
     * Returns the token with all but the first and last three characters replaced, safe to log.
     *
     * @return the masked token, or {@code "<unset>"}
     */
    public static String maskedToken() {
        return optional(TOKEN).map(IntegrationConfig::mask).orElse("<unset>");
    }

    /**
     * Returns the API base URL override, or {@code null} to use the client default.
     *
     * @return the base URL or {@code null}
     */
    public static String baseUrlOrNull() {
        return optional(BASE_URL).orElse(null);
    }

    /**
     * Returns the main test chat id, aborting the test if it is not configured.
     *
     * @return the chat id
     */
    public static long chatId() {
        return requireLong(CHAT_ID);
    }

    /**
     * Returns the configured user id, aborting the test if it is not configured.
     *
     * @return the user id
     */
    public static long userId() {
        return requireLong(USER_ID);
    }

    /**
     * Returns the channel id used by the comment tests, aborting the test if it is not
     * configured.
     *
     * @return the channel id
     */
    public static long channelId() {
        return requireLong(CHANNEL_ID);
    }

    /**
     * Returns the identifier of the post the comment tests operate on, aborting the test if it
     * is not configured.
     *
     * @return the post identifier (mid)
     */
    public static String postId() {
        return requireString(POST_ID);
    }

    /**
     * Returns the public chat link, aborting the test if it is not configured.
     *
     * @return the chat link
     */
    public static String chatLink() {
        return requireString(CHAT_LINK);
    }

    /**
     * Returns the disposable chat id, aborting the test if it is not configured.
     *
     * @return the disposable chat id
     */
    public static long disposableChatId() {
        return requireLong(DISPOSABLE_CHAT_ID);
    }

    /**
     * Returns the chat id that {@code deleteChat} is allowed to delete.
     *
     * @return the deletable chat id
     */
    public static long deletableChatId() {
        return requireLong(DELETABLE_CHAT_ID);
    }

    /**
     * Returns the public webhook URL, aborting the test if it is not configured.
     *
     * @return the webhook URL
     */
    public static String webhookUrl() {
        return requireString(WEBHOOK_URL);
    }

    /**
     * Returns the local webhook port.
     *
     * @return the port, {@value #DEFAULT_WEBHOOK_PORT} when unset
     */
    public static int webhookPort() {
        return optional(WEBHOOK_PORT).map(Integer::parseInt).orElse(DEFAULT_WEBHOOK_PORT);
    }

    /**
     * Returns the webhook secret, or {@code null} when the endpoint is unauthenticated.
     *
     * @return the secret or {@code null}
     */
    public static String webhookSecretOrNull() {
        return optional(WEBHOOK_SECRET).orElse(null);
    }

    /**
     * Returns an existing media file from the given variable, aborting the test if unusable.
     *
     * @param variable {@link #VIDEO_PATH} or {@link #AUDIO_PATH}
     * @return the file path
     */
    public static Path mediaPath(String variable) {
        String raw = requireString(variable);
        Path path = Path.of(raw);
        Assumptions.assumeTrue(Files.isReadable(path),
                () -> variable + " points at " + raw + ", which is not a readable file");
        return path;
    }

    /**
     * Returns whether interactive steps are enabled.
     *
     * @return {@code true} if {@value #INTERACTIVE} is {@code true}
     */
    public static boolean interactive() {
        return flag(INTERACTIVE);
    }

    /**
     * Returns whether irreversible operations are enabled.
     *
     * @return {@code true} if {@value #DESTRUCTIVE} is {@code true}
     */
    public static boolean destructive() {
        return flag(DESTRUCTIVE);
    }

    /**
     * Returns how long an interactive prompt waits for input.
     *
     * @return the timeout, {@value #DEFAULT_PROMPT_TIMEOUT_SECONDS} seconds when unset
     */
    public static Duration promptTimeout() {
        long seconds = optional(PROMPT_TIMEOUT)
                .map(Long::parseLong)
                .orElse(DEFAULT_PROMPT_TIMEOUT_SECONDS);
        return Duration.ofSeconds(seconds);
    }

    /**
     * Reads an environment variable, treating blank as unset.
     *
     * @param variable the variable name
     * @return the value, or empty
     */
    public static Optional<String> optional(String variable) {
        String value = System.getenv(variable);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    private static String requireString(String variable) {
        Optional<String> value = optional(variable);
        Assumptions.assumeTrue(value.isPresent(), () -> variable + " is not set");
        return value.orElseThrow();
    }

    private static long requireLong(String variable) {
        String raw = requireString(variable);
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(variable + " must be a number, got: " + raw, e);
        }
    }

    private static boolean flag(String variable) {
        return optional(variable).filter("true"::equalsIgnoreCase).isPresent();
    }

    private static String mask(String value) {
        int visible = 3;
        if (value.length() <= visible * 2) {
            return "*".repeat(value.length());
        }
        return value.substring(0, visible) + "..." + value.substring(value.length() - visible);
    }
}
