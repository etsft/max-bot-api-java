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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.core.MaxSerializerProvider;
import ru.max.botapi.core.TypeReference;
import ru.max.botapi.gson.GsonMaxSerializer;
import ru.max.botapi.jackson.JacksonMaxSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link DefaultSerializers}.
 */
class DefaultSerializersTest {

    private static final MaxSerializer STUB = new StubSerializer();

    @Test
    void discoversTheBundledAdapters() {
        assertThat(DefaultSerializers.discoverProviders())
                .extracting(MaxSerializerProvider::name)
                .contains("jackson", "gson");
    }

    @Test
    void prefersJacksonOverGson() {
        assertThat(DefaultSerializers.load()).isInstanceOf(JacksonMaxSerializer.class);
        List<MaxSerializerProvider> withoutJackson = DefaultSerializers.discoverProviders().stream()
                .filter(provider -> !provider.name().equals("jackson"))
                .toList();
        assertThat(DefaultSerializers.load(withoutJackson)).isInstanceOf(GsonMaxSerializer.class);
    }

    @Test
    void usesTheHighestPriorityRegardlessOfOrder() {
        MaxSerializer preferred = new StubSerializer();

        MaxSerializer chosen = DefaultSerializers.load(List.of(
                provider("low", 1, () -> STUB),
                provider("high", 200, () -> preferred),
                provider("mid", 100, () -> STUB)));

        assertThat(chosen).isSameAs(preferred);
    }

    @Test
    void breaksPriorityTiesByName() {
        MaxSerializer alpha = new StubSerializer();

        MaxSerializer chosen = DefaultSerializers.load(List.of(
                provider("beta", 5, () -> STUB),
                provider("alpha", 5, () -> alpha)));

        assertThat(chosen).isSameAs(alpha);
    }

    @Test
    void skipsProvidersWithBrokenDependencies() {
        MaxSerializer fallback = new StubSerializer();

        MaxSerializer chosen = DefaultSerializers.load(List.of(
                provider("missing-class", 3, () -> {
                    throw new NoClassDefFoundError("tools/jackson/Missing");
                }),
                provider("broken-init", 2, () -> {
                    throw new ExceptionInInitializerError("broken static init");
                }),
                provider("fallback", 1, () -> fallback)));

        assertThat(chosen).isSameAs(fallback);
    }

    @Test
    void failsWithAllCausesWhenEveryProviderIsBroken() {
        assertThatThrownBy(() -> DefaultSerializers.load(List.of(
                provider("a", 2, () -> {
                    throw new NoClassDefFoundError("A");
                }),
                provider("b", 1, () -> {
                    throw new NoClassDefFoundError("B");
                }))))
                .isInstanceOf(MaxClientException.class)
                .hasMessageContaining("max-bot-api-jackson")
                .hasMessageContaining("max-bot-api-gson")
                .satisfies(e -> assertThat(e.getCause())
                        .hasMessage("A")
                        .satisfies(cause -> assertThat(cause.getSuppressed())
                                .singleElement().satisfies(s -> assertThat(s).hasMessage("B"))));
    }

    @Test
    void failsWhenThereIsNoProvider() {
        assertThatThrownBy(() -> DefaultSerializers.load(List.of()))
                .isInstanceOf(MaxClientException.class)
                .hasMessageContaining("No JSON serializer found")
                .hasNoCause();
    }

    @Test
    void doesNotMaskOtherFailures() {
        assertThatThrownBy(() -> DefaultSerializers.load(List.of(
                provider("buggy", 2, () -> {
                    throw new IllegalStateException("bug");
                }),
                provider("fallback", 1, () -> STUB))))
                .isInstanceOf(MaxClientException.class)
                .hasMessage("Failed to create serializer 'buggy'")
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> DefaultSerializers.load(List.of(provider("null", 1, () -> null))))
                .isInstanceOf(MaxClientException.class)
                .hasCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void discoversProvidersOfTheContextClassLoaderAndSkipsBrokenEntries(@TempDir Path dir)
            throws IOException {
        Path services = dir.resolve("META-INF/services");
        Files.createDirectories(services);
        Files.writeString(services.resolve(MaxSerializerProvider.class.getName()),
                "ru.max.botapi.missing.MissingProvider\n" + ContextProvider.class.getName() + "\n");

        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        try (URLClassLoader context = new URLClassLoader(new URL[] {dir.toUri().toURL()},
                getClass().getClassLoader())) {
            thread.setContextClassLoader(context);

            assertThat(DefaultSerializers.discoverProviders())
                    .extracting(MaxSerializerProvider::name)
                    .contains("context", "jackson", "gson")
                    .doesNotHaveDuplicates();
            assertThat(DefaultSerializers.load()).isInstanceOf(StubSerializer.class);
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    private static MaxSerializerProvider provider(String name, int priority,
            Supplier<MaxSerializer> factory) {
        return new MaxSerializerProvider() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public int priority() {
                return priority;
            }

            @Override
            public MaxSerializer create() {
                return factory.get();
            }
        };
    }

    /** Provider registered only through the context class loader in a test. */
    public static final class ContextProvider implements MaxSerializerProvider {

        @Override
        public String name() {
            return "context";
        }

        @Override
        public int priority() {
            return 1000;
        }

        @Override
        public MaxSerializer create() {
            return new StubSerializer();
        }
    }

    /** Serializer whose behaviour is never invoked. */
    static final class StubSerializer implements MaxSerializer {

        @Override
        public <T> String serialize(T object) {
            return "";
        }

        @Override
        public <T> T deserialize(String json, Class<T> type) {
            return null;
        }

        @Override
        public <T> T deserialize(String json, TypeReference<T> type) {
            return null;
        }
    }
}
