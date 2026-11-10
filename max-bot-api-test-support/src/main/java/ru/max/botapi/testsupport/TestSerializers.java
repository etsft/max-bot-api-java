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

package ru.max.botapi.testsupport;

import java.util.ServiceLoader;

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.core.MaxSerializerProvider;

/**
 * Creates the {@link MaxSerializer} a test suite runs against, so one suite can be run on every
 * adapter.
 *
 * <p>The adapter is chosen by its {@link MaxSerializerProvider#name()} from the system property
 * {@value #PROPERTY}, {@code "jackson"} by default; the build sets it to {@code "gson"} for the
 * {@code testGson} task. The chosen adapter must be on the test classpath.</p>
 */
public final class TestSerializers {

    /** System property naming the adapter, such as {@code "jackson"} or {@code "gson"}. */
    public static final String PROPERTY = "max.test.serializer";

    private static final String DEFAULT_NAME = "jackson";

    private TestSerializers() {
    }

    /**
     * Returns the name of the adapter selected for this run.
     *
     * @return the value of {@value #PROPERTY}, or {@code "jackson"}
     */
    public static String name() {
        return System.getProperty(PROPERTY, DEFAULT_NAME);
    }

    /**
     * Creates a new serializer of the selected adapter.
     *
     * @return a new serializer
     * @throws IllegalStateException if no adapter of that name is on the classpath; an unknown
     *                               name fails rather than silently testing the default adapter
     */
    public static MaxSerializer create() {
        String name = name();
        for (MaxSerializerProvider provider : ServiceLoader.load(MaxSerializerProvider.class)) {
            if (provider.name().equals(name)) {
                return provider.create();
            }
        }
        throw new IllegalStateException("No MaxSerializerProvider named '" + name
                + "' on the test classpath (system property " + PROPERTY + ")");
    }
}
