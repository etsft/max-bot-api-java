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

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.core.MaxSerializerProvider;

/**
 * Registers {@link JacksonMaxSerializer} for discovery by {@link java.util.ServiceLoader}.
 *
 * <p>Refers to the Jackson classes only inside {@link #create()}, so loading this provider succeeds
 * even when the Jackson library on the classpath is missing or broken.</p>
 */
public final class JacksonMaxSerializerProvider implements MaxSerializerProvider {

    /** Creates the provider; called by {@link java.util.ServiceLoader}. */
    public JacksonMaxSerializerProvider() {
    }

    @Override
    public String name() {
        return "jackson";
    }

    @Override
    public int priority() {
        return JACKSON_PRIORITY;
    }

    @Override
    public MaxSerializer create() {
        return new JacksonMaxSerializer();
    }
}
