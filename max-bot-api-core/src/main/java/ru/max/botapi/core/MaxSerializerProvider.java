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
 * Service provider of a {@link MaxSerializer}, discovered with {@link java.util.ServiceLoader}.
 *
 * <p>{@code MaxBotAPI.create(...)} and {@code new MaxUploadAPI()} use the available provider with
 * the highest {@link #priority()}. The bundled adapters register themselves this way:</p>
 * <ul>
 *   <li>{@code max-bot-api-jackson} — name {@code "jackson"}, priority {@value #JACKSON_PRIORITY}</li>
 *   <li>{@code max-bot-api-gson} — name {@code "gson"}, priority {@value #GSON_PRIORITY}</li>
 * </ul>
 *
 * <p>A custom adapter takes part by implementing this interface and listing the implementation in
 * {@code META-INF/services/ru.max.botapi.core.MaxSerializerProvider}; a priority above
 * {@value #JACKSON_PRIORITY} makes it the default even when a bundled adapter is present.</p>
 *
 * <p>Implementations must have a public no-arg constructor and should not reference the JSON
 * library until {@link #create()}: a provider whose library is missing or broken on the classpath
 * is then skipped in favour of the next one instead of failing the lookup.</p>
 */
public interface MaxSerializerProvider {

    /** Priority of the bundled Jackson adapter. */
    int JACKSON_PRIORITY = 100;

    /** Priority of the bundled Gson adapter. */
    int GSON_PRIORITY = 50;

    /**
     * Returns a short name identifying the adapter, such as {@code "jackson"}.
     *
     * @return the adapter name
     */
    String name();

    /**
     * Returns the priority of this provider; higher wins.
     *
     * @return the priority
     */
    int priority();

    /**
     * Creates a new serializer.
     *
     * @return a new serializer instance
     */
    MaxSerializer create();
}
