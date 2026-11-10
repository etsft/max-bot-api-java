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

package ru.max.botapi.client;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.max.botapi.core.MaxSerializer;

/**
 * Locates a default {@link MaxSerializer} on the classpath via reflection, so the client has no
 * compile-time dependency on any JSON library.
 *
 * <p>Candidates are tried in order: {@code max-bot-api-jackson}, then {@code max-bot-api-gson}.</p>
 */
final class DefaultSerializers {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSerializers.class);

    /** Fully qualified names of the bundled serializer adapters, in order of preference. */
    static final List<String> CANDIDATES = List.of(
            "ru.max.botapi.jackson.JacksonMaxSerializer",
            "ru.max.botapi.gson.GsonMaxSerializer");

    private DefaultSerializers() {
    }

    /**
     * Instantiates the first bundled serializer adapter found on the classpath.
     *
     * @return the instantiated serializer
     * @throws MaxClientException if no adapter is on the classpath or it cannot be instantiated
     */
    static MaxSerializer load() {
        return load(CANDIDATES);
    }

    /**
     * Instantiates the first of {@code classNames} found on the classpath.
     *
     * <p>A class that is missing, or whose own dependencies are broken (a {@link LinkageError},
     * such as a conflicting Jackson version), is skipped in favour of the next one. Any other
     * failure to instantiate a found class is a configuration bug and is not masked.</p>
     *
     * @param classNames fully qualified names of {@link MaxSerializer} implementations with a
     *                   public no-arg constructor, in order of preference
     * @return the instantiated serializer
     * @throws MaxClientException if no class is usable or the found one cannot be instantiated
     */
    static MaxSerializer load(List<String> classNames) {
        Throwable skipped = null;
        for (String className : classNames) {
            try {
                return instantiate(className);
            } catch (ClassNotFoundException e) {
                skipped = addSkipped(skipped, e);
            } catch (LinkageError e) {
                LOG.warn("Serializer {} is on the classpath but its dependencies are broken; "
                        + "trying the next one", className, e);
                skipped = addSkipped(skipped, e);
            }
        }
        throw new MaxClientException(
                "No JSON serializer found on classpath. Add the 'max-bot-api-jackson' or "
                + "'max-bot-api-gson' dependency, or supply a custom MaxSerializer.", skipped);
    }

    private static MaxSerializer instantiate(String className) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        try {
            return (MaxSerializer) clazz.getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof LinkageError linkageError) {
                throw linkageError;
            }
            throw new MaxClientException("Failed to instantiate serializer: " + className, e);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                | ClassCastException e) {
            throw new MaxClientException("Failed to instantiate serializer: " + className, e);
        }
    }

    private static Throwable addSkipped(Throwable first, Throwable next) {
        if (first == null) {
            return next;
        }
        first.addSuppressed(next);
        return first;
    }
}
