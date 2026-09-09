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

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.core.TypeReference;
import ru.max.botapi.jackson.JacksonMaxSerializer;

/**
 * Serializer decorator that remembers the last JSON it was asked to deserialize.
 *
 * <p>{@code MaxClient.execute} deserializes the response and never exposes the body, so a
 * failure like "the API returned an attachment this library cannot represent" would otherwise
 * be reported without the payload that caused it. Wrapping the serializer is the least
 * invasive way to keep that payload available for the failure message.</p>
 *
 * <p>The recorded value is per-thread: long polling deserializes on its own thread while the
 * test thread makes ordinary calls.</p>
 */
public final class RecordingSerializer implements MaxSerializer {

    private static final ThreadLocal<String> LAST_JSON = new ThreadLocal<>();

    private final MaxSerializer delegate = new JacksonMaxSerializer();

    /**
     * Returns the last JSON deserialized on the current thread.
     *
     * @return the raw JSON, or {@code "<none recorded>"} if nothing was deserialized yet
     */
    public static String lastJson() {
        String json = LAST_JSON.get();
        return json == null ? "<none recorded>" : json;
    }

    /** Forgets the JSON recorded for the current thread. */
    public static void reset() {
        LAST_JSON.remove();
    }

    /** {@inheritDoc} */
    @Override
    public <T> String serialize(T object) {
        return delegate.serialize(object);
    }

    /** {@inheritDoc} */
    @Override
    public <T> T deserialize(String json, Class<T> type) {
        LAST_JSON.set(json);
        try {
            return delegate.deserialize(json, type);
        } catch (RuntimeException e) {
            throw withPayload(e, type.getSimpleName(), json);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T> T deserialize(String json, TypeReference<T> type) {
        LAST_JSON.set(json);
        try {
            return delegate.deserialize(json, type);
        } catch (RuntimeException e) {
            throw withPayload(e, type.getType().getTypeName(), json);
        }
    }

    /**
     * Rewraps a deserialization failure with the payload that caused it.
     *
     * <p>The library deliberately keeps response bodies out of its exceptions. Here the body is
     * the whole point: a model that cannot be built from a live response is the drift this suite
     * exists to catch, and the JSON says what to change.</p>
     *
     * @param failure the exception thrown by the delegate
     * @param type    the target type, for the message
     * @param json    the payload that failed to map
     * @return an exception carrying both
     */
    private static RuntimeException withPayload(RuntimeException failure, String type,
                                                String json) {
        return new IllegalArgumentException(String.format(
                "The live API returned a %s payload this library cannot map: %s%n"
                        + "Fix the model, do not relax the test.%nPayload:%n%s",
                type, failure.getMessage(), json), failure);
    }
}
