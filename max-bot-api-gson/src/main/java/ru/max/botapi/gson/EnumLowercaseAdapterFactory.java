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
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Gson factory that serializes all enums as lowercase snake_case
 * and deserializes case-insensitively.
 */
final class EnumLowercaseAdapterFactory implements TypeAdapterFactory {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        Class<? super T> rawType = typeToken.getRawType();
        if (!Enum.class.isAssignableFrom(rawType) || rawType == Enum.class) {
            return null;
        }
        // A constant with a body is an anonymous subclass of its enum
        Class<?> enumType = rawType.isEnum() ? rawType : rawType.getSuperclass();
        return (TypeAdapter<T>) new LowercaseEnumAdapter(enumType);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final class LowercaseEnumAdapter extends TypeAdapter<Enum<?>> {

        private final Class<? extends Enum> enumType;

        LowercaseEnumAdapter(Class<?> enumType) {
            this.enumType = (Class<? extends Enum>) enumType;
        }

        @Override
        public void write(JsonWriter out, Enum<?> value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.name().toLowerCase(Locale.ROOT));
            }
        }

        @Override
        public Enum<?> read(JsonReader in) throws IOException {
            JsonToken token = in.peek();
            if (token == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            if (token != JsonToken.STRING && token != JsonToken.NUMBER && token != JsonToken.BOOLEAN) {
                throw new JsonParseException("Expected a string for enum " + enumType.getSimpleName()
                        + " but was " + token + " at path " + in.getPath());
            }
            String text = token == JsonToken.BOOLEAN ? String.valueOf(in.nextBoolean()) : in.nextString();
            if (text.isBlank()) {
                throw new JsonParseException("Blank value for enum " + enumType.getSimpleName()
                        + " at path " + in.getPath());
            }
            try {
                return Enum.valueOf(enumType, text.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                // Forward-compatible: unknown enum values return null instead of throwing,
                // so new API values never crash running bots.
                return null;
            }
        }
    }
}
