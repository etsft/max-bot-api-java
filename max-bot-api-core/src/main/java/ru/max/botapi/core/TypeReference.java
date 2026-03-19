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

package ru.max.botapi.core;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Custom generic type token for preserving generic type information at runtime.
 *
 * <p>This is a zero-dependency alternative to Jackson's {@code TypeReference}
 * or Gson's {@code TypeToken}. It is used by {@link MaxSerializer} to support
 * deserialization of generic types (e.g., {@code List<Update>}).</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * TypeReference<List<Update>> ref = new TypeReference<>() {};
 * List<Update> updates = serializer.deserialize(json, ref);
 * }</pre>
 *
 * @param <T> the type to capture
 */
public abstract class TypeReference<T> {

    private final Type type;

    /**
     * Creates a TypeReference capturing the generic type argument.
     *
     * @throws IllegalStateException if not parameterized
     */
    protected TypeReference() {
        Type superclass = getClass().getGenericSuperclass();
        if (!(superclass instanceof ParameterizedType pt)) {
            throw new IllegalStateException("TypeReference must be parameterized");
        }
        this.type = pt.getActualTypeArguments()[0];
    }

    /**
     * Returns the captured type.
     *
     * @return the generic type
     */
    public Type getType() {
        return type;
    }
}
