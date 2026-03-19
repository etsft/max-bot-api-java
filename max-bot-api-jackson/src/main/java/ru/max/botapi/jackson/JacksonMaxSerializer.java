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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.core.TypeReference;

/**
 * Jackson-based implementation of {@link MaxSerializer}.
 *
 * <p>Configured with:</p>
 * <ul>
 *   <li>{@code SNAKE_CASE} property naming</li>
 *   <li>{@code NON_NULL} serialization inclusion</li>
 *   <li>{@code FAIL_ON_UNKNOWN_PROPERTIES = false}</li>
 *   <li>Custom {@link MaxBotApiModule} for sealed type hierarchies</li>
 * </ul>
 */
public class JacksonMaxSerializer implements MaxSerializer {

    private final ObjectMapper mapper;

    /**
     * Creates a new JacksonMaxSerializer with default configuration.
     */
    public JacksonMaxSerializer() {
        this.mapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addModule(new MaxBotApiModule())
                .addModule(new EnumLowercaseModule())
                .build();
    }

    /**
     * Returns the underlying ObjectMapper.
     *
     * <p><strong>Warning:</strong> Callers must NOT modify the returned instance.
     * The ObjectMapper is shared and mutable; registering modules or changing features
     * will affect all users of this serializer. For advanced use cases only.</p>
     *
     * @return the configured ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return mapper;
    }

    @Override
    public <T> String serialize(T object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to serialize object", e);
        }
    }

    @Override
    public <T> T deserialize(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON", e);
        }
    }

    @Override
    public <T> T deserialize(String json, TypeReference<T> type) {
        try {
            JavaType javaType = mapper.getTypeFactory().constructType(type.getType());
            return mapper.readValue(json, javaType);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON", e);
        }
    }
}
