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

import java.lang.reflect.RecordComponent;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import ru.max.botapi.model.ModelTypes;

/**
 * Generic serializer for sealed type hierarchies that need a discriminator field in the
 * output JSON.
 *
 * <p>Writes the discriminator field ({@code type} or {@code update_type}) first, then writes
 * each record component as a field, converting names to snake_case. An {@code Unknown*} value
 * read from JSON is written as that original JSON instead, see
 * {@link ModelTypes#rawJsonOf(Object)}.</p>
 *
 * @param <T> the sealed interface type
 */
final class SealedTypeSerializer<T> extends StdSerializer<T> {

    private static final ConcurrentHashMap<Class<?>, RecordComponent[]> COMPONENT_CACHE
            = new ConcurrentHashMap<>();

    private final String discriminatorField;
    private final Function<T, String> typeExtractor;

    SealedTypeSerializer(Class<T> clazz, Function<T, String> typeExtractor) {
        this(clazz, "type", typeExtractor);
    }

    SealedTypeSerializer(Class<T> clazz, String discriminatorField, Function<T, String> typeExtractor) {
        super(clazz);
        this.discriminatorField = discriminatorField;
        this.typeExtractor = typeExtractor;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializationContext ctxt) {
        String rawJson = ModelTypes.rawJsonOf(value).orElse(null);
        if (rawJson != null) {
            gen.writeRawValue(rawJson);
            return;
        }
        gen.writeStartObject();
        gen.writeStringProperty(discriminatorField, typeExtractor.apply(value));

        RecordComponent[] components = COMPONENT_CACHE.computeIfAbsent(
                value.getClass(), Class::getRecordComponents);
        if (components != null) {
            for (RecordComponent component : components) {
                String fieldName = toSnakeCase(component.getName());
                if (discriminatorField.equals(fieldName)) {
                    continue; // already written as discriminator
                }
                try {
                    Object fieldValue = component.getAccessor().invoke(value);
                    if (fieldValue != null) {
                        gen.writeName(fieldName);
                        ctxt.writeValue(gen, fieldValue);
                    }
                } catch (ReflectiveOperationException e) {
                    throw DatabindException.from(gen, "Failed to access record component: "
                            + component.getName(), e);
                }
            }
        }

        gen.writeEndObject();
    }

    private static String toSnakeCase(String camelCase) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char ch = camelCase.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
