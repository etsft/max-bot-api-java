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

package ru.max.botapi.jackson;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;

/**
 * Helpers for lenient deserialization of the sealed model hierarchies.
 *
 * <p>The MAX API may send a payload that does not satisfy the invariants of the
 * corresponding model record (a missing required field, an unexpected shape).
 * Letting such a payload propagate an exception would abort the whole enclosing
 * document; instead the offending node is logged and represented by the
 * forward-compatibility fallback type of its hierarchy.</p>
 */
final class LenientReads {

    private static final Logger LOG = LoggerFactory.getLogger(LenientReads.class);

    /** Maximum number of raw JSON characters written to the log for a rejected node. */
    private static final int MAX_LOGGED_RAW_LENGTH = 2048;

    private LenientReads() {
    }

    /**
     * Reads {@code node} as {@code type}, falling back to {@code fallback} if construction fails.
     *
     * @param <T>           the hierarchy type
     * @param ctxt          the active deserialization context
     * @param node          the JSON node to read
     * @param type          the concrete type resolved from the discriminator
     * @param discriminator the discriminator value, used for logging
     * @param fallback      supplier of the fallback value, invoked only on failure
     * @return the deserialized value, or the fallback if deserialization failed
     */
    static <T> T readOrFallback(DeserializationContext ctxt, JsonNode node, Class<? extends T> type,
            String discriminator, Supplier<? extends T> fallback) {
        try {
            return ctxt.readTreeAsValue(node, type);
        } catch (RuntimeException e) {
            LOG.warn("Failed to deserialize '{}' as {}, falling back to the unknown-type "
                            + "representation. Raw JSON: {}",
                    discriminator, type.getSimpleName(), truncate(node), e);
            return fallback.get();
        }
    }

    /**
     * Renders a node as a JSON string, truncated to a length that is safe to log.
     *
     * @param node the node to render
     * @return the raw JSON, truncated with an ellipsis if too long
     */
    static String truncate(JsonNode node) {
        String raw = node.toString();
        return raw.length() <= MAX_LOGGED_RAW_LENGTH
                ? raw
                : raw.substring(0, MAX_LOGGED_RAW_LENGTH) + "...(truncated)";
    }
}
