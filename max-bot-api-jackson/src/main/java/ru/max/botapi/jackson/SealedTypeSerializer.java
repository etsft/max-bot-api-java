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

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Generic serializer for sealed type hierarchies that need a {@code type} discriminator
 * field in the output JSON.
 *
 * <p>Writes the {@code "type"} field first, then writes each record component as a
 * field, converting names to snake_case.</p>
 *
 * @param <T> the sealed interface type
 */
final class SealedTypeSerializer<T> extends StdSerializer<T> {

    private static final long serialVersionUID = 1L;

    private static final ConcurrentHashMap<Class<?>, RecordComponent[]> COMPONENT_CACHE
            = new ConcurrentHashMap<>();

    @SuppressWarnings("serial") // Function lambda is not serializable, but Jackson serializers don't need it
    private final Function<T, String> typeExtractor;

    SealedTypeSerializer(Class<T> clazz, Function<T, String> typeExtractor) {
        super(clazz);
        this.typeExtractor = typeExtractor;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("type", typeExtractor.apply(value));

        RecordComponent[] components = COMPONENT_CACHE.computeIfAbsent(
                value.getClass(), Class::getRecordComponents);
        if (components != null) {
            for (RecordComponent component : components) {
                String fieldName = toSnakeCase(component.getName());
                if ("type".equals(fieldName)) {
                    continue; // already written as discriminator
                }
                try {
                    Object fieldValue = component.getAccessor().invoke(value);
                    if (fieldValue != null) {
                        gen.writeFieldName(fieldName);
                        provider.defaultSerializeValue(fieldValue, gen);
                    }
                } catch (ReflectiveOperationException e) {
                    throw new IOException("Failed to access record component: "
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
