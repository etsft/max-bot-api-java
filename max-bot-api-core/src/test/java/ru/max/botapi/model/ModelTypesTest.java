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

package ru.max.botapi.model;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ModelTypes}.
 */
class ModelTypesTest {

    static Stream<Arguments> tables() {
        return Stream.of(
                Arguments.of(Update.class, ModelTypes.updates(), UnknownUpdate.class),
                Arguments.of(Attachment.class, ModelTypes.attachments(), UnknownAttachment.class),
                Arguments.of(AttachmentRequest.class, ModelTypes.attachmentRequests(),
                        UnknownAttachmentRequest.class),
                Arguments.of(Button.class, ModelTypes.buttons(), UnknownButton.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void everyPermittedSubtypeExceptTheFallbackIsRegisteredOnce(Class<?> sealedType,
            Map<String, Class<?>> table, Class<?> fallback) {
        Set<Class<?>> permitted = Stream.of(sealedType.getPermittedSubclasses())
                .filter(type -> type != fallback)
                .collect(Collectors.toSet());

        assertThat(table.values())
                .as("a record added to %s must be added to ModelTypes", sealedType.getSimpleName())
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrderElementsOf(permitted);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void tablesAreImmutable(Class<?> sealedType, Map<String, Class<?>> table, Class<?> fallback) {
        assertThatThrownBy(() -> table.put("x", fallback)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rawJsonOfReturnsTheObjectAnUnknownValueWasReadFrom() {
        assertThat(ModelTypes.rawJsonOf(new UnknownUpdate("x", 1L, " {\"a\":1} ")))
                .contains("{\"a\":1}");
        assertThat(ModelTypes.rawJsonOf(new UnknownAttachment("x", "{}"))).contains("{}");
        assertThat(ModelTypes.rawJsonOf(new UnknownAttachmentRequest("x", "{\"b\":2}")))
                .contains("{\"b\":2}");
        assertThat(ModelTypes.rawJsonOf(new UnknownButton("x", "t", "{}"))).contains("{}");
    }

    @Test
    void rawJsonOfIsEmptyForKnownValuesNullAndNonObjects() {
        assertThat(ModelTypes.rawJsonOf(null)).isEmpty();
        assertThat(ModelTypes.rawJsonOf("{}")).isEmpty();
        assertThat(ModelTypes.rawJsonOf(new CallbackButton("t", "p", null))).isEmpty();
        assertThat(ModelTypes.rawJsonOf(new UnknownButton("x", "t", ""))).isEmpty();
        assertThat(ModelTypes.rawJsonOf(new UnknownButton("x", "t", "[1]"))).isEmpty();
        assertThat(ModelTypes.rawJsonOf(new UnknownButton("x", "t", "{\"a\":1"))).isEmpty();
    }
}
