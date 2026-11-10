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

package ru.max.botapi.gson;

import java.io.IOException;
import java.io.StringReader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.ReflectionAccessFilter;
import com.google.gson.Strictness;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.core.TypeReference;

/**
 * Gson-based implementation of {@link MaxSerializer}.
 *
 * <p>Produces the same wire format as the Jackson adapter:</p>
 * <ul>
 *   <li>{@code snake_case} property naming, with the few API-specific exceptions
 *       handled by {@link MaxFieldNamingStrategy}</li>
 *   <li>{@code null} properties are omitted from the output</li>
 *   <li>unknown properties are ignored</li>
 *   <li>enums are written in lowercase and read case-insensitively; unknown values read as {@code null}</li>
 *   <li>numbers and booleans accept the same coercions as Jackson's defaults — a fractional
 *       number into an integer, numeric text, blank text as the default value — and NaN and
 *       the infinities are written as strings (see {@link LenientPrimitiveAdapters})</li>
 *   <li>sealed hierarchies are dispatched on their discriminator, with lenient fallbacks
 *       to the {@code Unknown*} types (see {@link MaxTypeAdapters})</li>
 * </ul>
 */
public class GsonMaxSerializer implements MaxSerializer {

    private final Gson gson;

    /**
     * Creates a new GsonMaxSerializer with default configuration.
     */
    public GsonMaxSerializer() {
        GsonBuilder builder = new GsonBuilder()
                .setFieldNamingStrategy(new MaxFieldNamingStrategy())
                .setStrictness(Strictness.STRICT)
                .disableHtmlEscaping()
                .addReflectionAccessFilter(ReflectionAccessFilter.BLOCK_INACCESSIBLE_JAVA)
                .registerTypeAdapterFactory(new EnumLowercaseAdapterFactory());
        LenientPrimitiveAdapters.register(builder);
        MaxTypeAdapters.register(builder);
        this.gson = builder.create();
    }

    /**
     * Returns the underlying {@link Gson} instance.
     *
     * <p>{@code Gson} is immutable; derive a customized instance with {@link Gson#newBuilder()}.
     * For advanced use cases only.</p>
     *
     * @return the configured Gson
     */
    public Gson getGson() {
        return gson;
    }

    /** {@inheritDoc} */
    @Override
    public <T> String serialize(T object) {
        try {
            return gson.toJson(object);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Failed to serialize object", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T> T deserialize(String json, Class<T> type) {
        return read(json, TypeToken.get(type));
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(String json, TypeReference<T> type) {
        return read(json, (TypeToken<T>) TypeToken.get(type.getType()));
    }

    /**
     * Reads a complete JSON document as {@code type}.
     *
     * <p>Unlike {@link Gson#fromJson(String, Class)}, empty input is rejected rather than
     * read as {@code null}, matching the Jackson adapter.</p>
     */
    private <T> T read(String json, TypeToken<T> type) {
        try {
            TypeAdapter<T> adapter = gson.getAdapter(type);
            JsonReader reader = gson.newJsonReader(new StringReader(json));
            T value = adapter.read(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonIOException("JSON document was not fully consumed");
            }
            return value;
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON", e);
        }
    }
}
