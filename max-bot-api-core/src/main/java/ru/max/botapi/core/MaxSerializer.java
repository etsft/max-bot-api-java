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

/**
 * Abstraction for JSON serialization/deserialization.
 *
 * <p>Implementations of this interface (e.g., Jackson adapter, Gson adapter)
 * live in separate modules. The core module defines only this interface,
 * preserving the zero-dependency guarantee.</p>
 */
public interface MaxSerializer {

    /**
     * Serializes an object to a JSON string.
     *
     * @param object the object to serialize
     * @param <T>    the type of the object
     * @return JSON string representation
     */
    <T> String serialize(T object);

    /**
     * Deserializes a JSON string to an object of the given class.
     *
     * @param json the JSON string
     * @param type the target class
     * @param <T>  the target type
     * @return the deserialized object
     */
    <T> T deserialize(String json, Class<T> type);

    /**
     * Deserializes a JSON string to an object of the given generic type.
     *
     * @param json the JSON string
     * @param type the target type reference (for generics)
     * @param <T>  the target type
     * @return the deserialized object
     */
    <T> T deserialize(String json, TypeReference<T> type);
}
