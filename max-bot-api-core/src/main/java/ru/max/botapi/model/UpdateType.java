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

package ru.max.botapi.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * All known update types delivered by the MAX Bot API.
 *
 * <p>Each constant corresponds to the {@code update_type} discriminator field
 * in the JSON payload and to the value returned by {@link Update#updateType()}.</p>
 *
 * <p>Use {@link #value()} to obtain the snake_case API string, or
 * {@link #of(String)} to look up a constant by that string:</p>
 *
 * <pre>{@code
 * // filtering a long-polling consumer or webhook
 * Set<UpdateType> wanted = Set.of(UpdateType.MESSAGE_CREATED, UpdateType.MESSAGE_CALLBACK);
 *
 * // converting to raw strings for the API
 * Set<String> rawTypes = UpdateType.toStrings(wanted);
 *
 * // looking up from a received update
 * UpdateType type = UpdateType.of(update.updateType()); // may be null for unknown
 * }</pre>
 */
public enum UpdateType {

    /** A new message was sent to a chat. */
    MESSAGE_CREATED("message_created"),

    /** A user tapped an inline keyboard button. */
    MESSAGE_CALLBACK("message_callback"),

    /** An existing message was edited. */
    MESSAGE_EDITED("message_edited"),

    /** A message was deleted. */
    MESSAGE_REMOVED("message_removed"),

    /** The bot was added to a chat. */
    BOT_ADDED("bot_added"),

    /** The bot was removed from a chat. */
    BOT_REMOVED("bot_removed"),

    /** A user was added to a chat. */
    USER_ADDED("user_added"),

    /** A user was removed from a chat. */
    USER_REMOVED("user_removed"),

    /** A user started a direct conversation with the bot. */
    BOT_STARTED("bot_started"),

    /** A user stopped (blocked) the bot in a direct conversation. */
    BOT_STOPPED("bot_stopped"),

    /** The title of a chat was changed. */
    CHAT_TITLE_CHANGED("chat_title_changed"),

    /** A message construction session was requested. */
    MESSAGE_CONSTRUCTION_REQUEST("message_construction_request"),

    /** A message construction session was completed. */
    MESSAGE_CONSTRUCTED("message_constructed"),

    /** A new chat was created via a message. */
    MESSAGE_CHAT_CREATED("message_chat_created");

    private final String value;

    UpdateType(String value) {
        this.value = value;
    }

    /**
     * Returns the snake_case API string for this update type
     * (e.g., {@code "message_created"}).
     *
     * @return the API value string
     */
    public String value() {
        return value;
    }

    /**
     * Looks up an {@code UpdateType} by its API value string.
     *
     * <p>Returns {@code null} for unrecognized values instead of throwing,
     * so callers can handle forward-compatible unknown types gracefully.</p>
     *
     * @param value the snake_case string (e.g., {@code "message_created"})
     * @return the matching constant, or {@code null} if not recognized
     */
    @Nullable
    public static UpdateType of(String value) {
        for (UpdateType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Converts a set of {@code UpdateType} constants to the raw string set
     * expected by the API.
     *
     * @param types the update type constants; must not be {@code null}
     * @return an unmodifiable set of API value strings
     */
    public static Set<String> toStrings(Set<UpdateType> types) {
        return types.stream()
                .map(UpdateType::value)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns the set of all known update type value strings.
     *
     * <p>Equivalent to {@code UpdateType.toStrings(EnumSet.allOf(UpdateType.class))}.</p>
     *
     * @return unmodifiable set of all 14 known API value strings
     */
    public static Set<String> allValues() {
        return Arrays.stream(values())
                .map(UpdateType::value)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return value;
    }
}
