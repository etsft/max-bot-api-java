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

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.core.MaxSerializerProvider;

/**
 * Locates the default {@link MaxSerializer} through {@link MaxSerializerProvider} services, so the
 * client has no compile-time dependency on any JSON library.
 *
 * <p>Providers are discovered with {@link ServiceLoader} in the client's own class loader and in
 * the thread context class loader, and tried from the highest {@link MaxSerializerProvider#priority()}
 * down: {@code max-bot-api-jackson}, then {@code max-bot-api-gson}, unless a custom adapter
 * outranks them. Nothing is cached, so no class loader is retained across application restarts.</p>
 */
final class DefaultSerializers {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSerializers.class);

    /** Bound on loading failures per class loader, in case a broken lookup never advances. */
    private static final int MAX_LOADING_FAILURES = 32;

    private static final Comparator<MaxSerializerProvider> BY_PREFERENCE =
            Comparator.comparingInt(MaxSerializerProvider::priority).reversed()
                    .thenComparing(MaxSerializerProvider::name);

    private DefaultSerializers() {
    }

    /**
     * Creates the serializer of the most preferred provider on the classpath.
     *
     * @return the created serializer
     * @throws MaxClientException if no provider is usable or the chosen one fails
     */
    static MaxSerializer load() {
        return load(discoverProviders());
    }

    /**
     * Discovers the providers visible to the client's class loader and to the thread context
     * class loader, one per provider class.
     *
     * <p>A provider that cannot be loaded is logged and skipped.</p>
     *
     * @return the discovered providers, in discovery order
     */
    static List<MaxSerializerProvider> discoverProviders() {
        Map<String, MaxSerializerProvider> byClassName = new LinkedHashMap<>();
        ClassLoader own = DefaultSerializers.class.getClassLoader();
        collect(own, byClassName);
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (context != null && context != own) {
            collect(context, byClassName);
        }
        return List.copyOf(byClassName.values());
    }

    private static void collect(ClassLoader loader, Map<String, MaxSerializerProvider> into) {
        // An explicit iterator rather than stream(): it keeps going past a provider that fails
        // to load, while a stream is done after the first error
        Iterator<MaxSerializerProvider> providers =
                ServiceLoader.load(MaxSerializerProvider.class, loader).iterator();
        int failures = 0;
        while (failures < MAX_LOADING_FAILURES) {
            try {
                if (!providers.hasNext()) {
                    return;
                }
                MaxSerializerProvider provider = providers.next();
                into.putIfAbsent(provider.getClass().getName(), provider);
            } catch (ServiceConfigurationError | LinkageError e) {
                failures++;
                LOG.warn("Skipping a MaxSerializerProvider that could not be loaded", e);
            }
        }
    }

    /**
     * Creates the serializer of the most preferred of {@code providers}.
     *
     * <p>A provider whose library is broken on the classpath — {@link MaxSerializerProvider#create()}
     * throws a {@link LinkageError}, such as for a conflicting Jackson version — is skipped in
     * favour of the next one. Any other failure is a configuration bug and is not masked.</p>
     *
     * @param providers the candidate providers, in any order
     * @return the created serializer
     * @throws MaxClientException if no provider is usable or the chosen one fails
     */
    static MaxSerializer load(List<MaxSerializerProvider> providers) {
        List<MaxSerializerProvider> ordered = providers.stream().sorted(BY_PREFERENCE).toList();
        if (ordered.size() > 1 && ordered.get(0).priority() == ordered.get(1).priority()) {
            LOG.warn("Serializers '{}' and '{}' share the top priority {}; using '{}'",
                    ordered.get(0).name(), ordered.get(1).name(), ordered.get(0).priority(),
                    ordered.get(0).name());
        }
        Throwable skipped = null;
        for (MaxSerializerProvider provider : ordered) {
            try {
                MaxSerializer serializer = Objects.requireNonNull(provider.create(),
                        "create() returned null");
                LOG.debug("Using the '{}' JSON serializer", provider.name());
                return serializer;
            } catch (LinkageError e) {
                LOG.warn("Serializer '{}' is on the classpath but its dependencies are broken; "
                        + "trying the next one", provider.name(), e);
                skipped = addSkipped(skipped, e);
            } catch (RuntimeException e) {
                throw new MaxClientException("Failed to create serializer '" + provider.name() + "'", e);
            }
        }
        throw new MaxClientException(
                "No JSON serializer found on classpath. Add the 'max-bot-api-jackson' or "
                + "'max-bot-api-gson' dependency, or supply a custom MaxSerializer.", skipped);
    }

    private static Throwable addSkipped(Throwable first, Throwable next) {
        if (first == null) {
            return next;
        }
        first.addSuppressed(next);
        return first;
    }
}
