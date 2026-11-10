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
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Gson factory for a sealed model hierarchy dispatched on a discriminator field.
 *
 * <p>For the base interface it reads the discriminator and delegates to the concrete type;
 * an unknown discriminator produces the hierarchy's fallback type carrying the raw JSON.
 * When {@code lenient}, a <em>known</em> type that cannot be constructed — a required field
 * missing, for example — also degrades to the fallback, so one odd element does not fail the
 * whole enclosing document.</p>
 *
 * <p>When a discriminator writer is given, every type of the hierarchy — the interface and
 * each concrete record — is written with the discriminator as the first property. The
 * concrete types matter because {@link Gson#toJson(Object)} resolves the adapter from the
 * runtime class.</p>
 *
 * @param <B> the sealed interface type
 */
final class SealedTypeAdapterFactory<B> implements TypeAdapterFactory {

    private final Class<B> baseType;
    private final String discriminatorField;
    private final Map<String, Class<? extends B>> subtypes;
    private final BiFunction<String, JsonElement, B> fallback;
    private final boolean lenient;
    private final boolean objectRequired;
    private final Function<B, String> discriminatorWriter;

    /**
     * Creates the factory.
     *
     * @param baseType            the sealed interface
     * @param discriminatorField  the JSON property holding the discriminator
     * @param subtypes            the concrete type for each known discriminator value
     * @param fallback            builds the fallback value from the discriminator and the raw node
     * @param lenient             whether a known type that fails to deserialize degrades to the fallback
     * @param objectRequired      whether a non-object node is rejected instead of read as the fallback
     * @param discriminatorWriter extracts the discriminator to write, or {@code null} to write
     *                            the records as plain objects
     */
    SealedTypeAdapterFactory(Class<B> baseType, String discriminatorField,
            Map<String, Class<? extends B>> subtypes, BiFunction<String, JsonElement, B> fallback,
            boolean lenient, boolean objectRequired, Function<B, String> discriminatorWriter) {
        this.baseType = baseType;
        this.discriminatorField = discriminatorField;
        this.subtypes = Map.copyOf(subtypes);
        this.fallback = fallback;
        this.lenient = lenient;
        this.objectRequired = objectRequired;
        this.discriminatorWriter = discriminatorWriter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        Class<? super T> rawType = typeToken.getRawType();
        if (rawType == baseType) {
            return (TypeAdapter<T>) new BaseAdapter(gson);
        }
        if (discriminatorWriter != null && baseType.isAssignableFrom(rawType)) {
            return new DiscriminatedRecordAdapter<>(gson.getDelegateAdapter(this, typeToken),
                    gson.getAdapter(JsonElement.class));
        }
        return null;
    }

    /**
     * Reads the discriminator of a node the way the Jackson adapter does: a missing,
     * {@code null} or non-scalar value reads as {@code "unknown"}, a scalar as its text.
     */
    private String discriminatorOf(JsonElement node) {
        if (node.isJsonObject()) {
            JsonElement value = node.getAsJsonObject().get(discriminatorField);
            if (value != null && value.isJsonPrimitive()) {
                return value.getAsString();
            }
        }
        return "unknown";
    }

    private final class BaseAdapter extends TypeAdapter<B> {

        private final Gson gson;
        private final TypeAdapter<JsonElement> elementAdapter;

        BaseAdapter(Gson gson) {
            this.gson = gson;
            this.elementAdapter = gson.getAdapter(JsonElement.class);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void write(JsonWriter out, B value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            ((TypeAdapter<B>) gson.getAdapter(value.getClass())).write(out, value);
        }

        @Override
        public B read(JsonReader in) throws IOException {
            JsonElement node = elementAdapter.read(in);
            if (node.isJsonNull()) {
                return null;
            }
            if (objectRequired && !node.isJsonObject()) {
                throw new JsonParseException("Expected a JSON object for " + baseType.getSimpleName()
                        + ", got " + LenientReads.truncate(node));
            }
            String discriminator = discriminatorOf(node);
            Class<? extends B> subtype = subtypes.get(discriminator);
            if (subtype == null) {
                return fallback.apply(discriminator, node);
            }
            if (!lenient) {
                return gson.getAdapter(subtype).fromJsonTree(node);
            }
            return LenientReads.readOrFallback(gson, node, subtype, discriminator,
                    () -> fallback.apply(discriminator, node));
        }
    }

    private final class DiscriminatedRecordAdapter<T> extends TypeAdapter<T> {

        private final TypeAdapter<T> delegate;
        private final TypeAdapter<JsonElement> elementAdapter;

        DiscriminatedRecordAdapter(TypeAdapter<T> delegate, TypeAdapter<JsonElement> elementAdapter) {
            this.delegate = delegate;
            this.elementAdapter = elementAdapter;
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            JsonObject fields = delegate.toJsonTree(value).getAsJsonObject();
            out.beginObject();
            out.name(discriminatorField).value(discriminatorWriter.apply(baseType.cast(value)));
            for (Map.Entry<String, JsonElement> entry : fields.entrySet()) {
                if (!discriminatorField.equals(entry.getKey())) {
                    // Written through `out`, so null properties are dropped per the writer's settings
                    out.name(entry.getKey());
                    elementAdapter.write(out, entry.getValue());
                }
            }
            out.endObject();
        }

        @Override
        public T read(JsonReader in) throws IOException {
            return delegate.read(in);
        }
    }
}
