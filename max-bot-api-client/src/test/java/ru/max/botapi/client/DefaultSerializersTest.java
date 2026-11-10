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

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.core.TypeReference;
import ru.max.botapi.jackson.JacksonMaxSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link DefaultSerializers}.
 */
class DefaultSerializersTest {

    private static final String MISSING = "ru.max.botapi.missing.MissingSerializer";

    @Test
    void prefersJacksonThenGson() {
        assertThat(DefaultSerializers.CANDIDATES).containsExactly(
                "ru.max.botapi.jackson.JacksonMaxSerializer",
                "ru.max.botapi.gson.GsonMaxSerializer");
        assertThat(DefaultSerializers.load()).isInstanceOf(JacksonMaxSerializer.class);
    }

    @Test
    void skipsCandidatesThatAreNotOnTheClasspath() {
        assertThat(DefaultSerializers.load(List.of(MISSING, StubSerializer.class.getName())))
                .isInstanceOf(StubSerializer.class);
    }

    @Test
    void failsWhenNoCandidateIsOnTheClasspath() {
        assertThatThrownBy(() -> DefaultSerializers.load(List.of(MISSING, MISSING + "2")))
                .isInstanceOf(MaxClientException.class)
                .hasMessageContaining("max-bot-api-jackson")
                .hasMessageContaining("max-bot-api-gson")
                .satisfies(e -> assertThat(e.getCause())
                        .isInstanceOf(ClassNotFoundException.class)
                        .satisfies(cause -> assertThat(cause.getSuppressed()).hasSize(1)));
    }

    @Test
    void failsWhenTheFoundClassCannotBeInstantiated() {
        assertThatThrownBy(() -> DefaultSerializers.load(List.of(String.class.getName())))
                .isInstanceOf(MaxClientException.class)
                .hasMessage("Failed to instantiate serializer: java.lang.String")
                .hasCauseInstanceOf(ClassCastException.class);
        assertThatThrownBy(() -> DefaultSerializers.load(List.of(NoDefaultConstructor.class.getName())))
                .isInstanceOf(MaxClientException.class)
                .hasCauseInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void skipsCandidatesWithBrokenDependencies() {
        assertThat(DefaultSerializers.load(List.of(
                BrokenStaticInit.class.getName(),
                BrokenConstructor.class.getName(),
                StubSerializer.class.getName())))
                .isInstanceOf(StubSerializer.class);
    }

    @Test
    void failsWithAllCausesWhenEveryCandidateIsBroken() {
        assertThatThrownBy(() -> DefaultSerializers.load(List.of(BrokenConstructor.class.getName(), MISSING)))
                .isInstanceOf(MaxClientException.class)
                .hasMessageContaining("No JSON serializer found")
                .satisfies(e -> assertThat(e.getCause())
                        .isInstanceOf(NoClassDefFoundError.class)
                        .satisfies(cause -> assertThat(cause.getSuppressed())
                                .singleElement().isInstanceOf(ClassNotFoundException.class)));
    }

    @Test
    void doesNotMaskOtherConstructorFailures() {
        assertThatThrownBy(() -> DefaultSerializers.load(List.of(
                FailingConstructor.class.getName(), StubSerializer.class.getName())))
                .isInstanceOf(MaxClientException.class)
                .hasMessageContaining("Failed to instantiate serializer")
                .rootCause().isInstanceOf(IllegalStateException.class);
    }

    /** Serializer whose class initialization fails, as with a missing dependency. */
    public static final class BrokenStaticInit extends StubBase {

        static {
            if (Boolean.parseBoolean("true")) {
                throw new IllegalStateException("broken static init");
            }
        }
    }

    /** Serializer whose constructor hits a missing class. */
    public static final class BrokenConstructor extends StubBase {

        /** Creates the instance. */
        public BrokenConstructor() {
            throw new NoClassDefFoundError("tools/jackson/Missing");
        }
    }

    /** Serializer whose constructor fails for a reason other than linkage. */
    public static final class FailingConstructor extends StubBase {

        /** Creates the instance. */
        public FailingConstructor() {
            throw new IllegalStateException("bug");
        }
    }

    /** Base for serializers whose behaviour is never invoked. */
    public abstract static class StubBase implements MaxSerializer {

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

    /** Minimal serializer with a public no-arg constructor. */
    public static final class StubSerializer implements MaxSerializer {

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

    /** Class without a no-arg constructor. */
    public static final class NoDefaultConstructor {

        /**
         * Creates the instance.
         *
         * @param ignored unused
         */
        public NoDefaultConstructor(String ignored) {
        }
    }
}
