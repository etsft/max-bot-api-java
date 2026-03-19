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
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TypeReference and MaxSerializer interface.
 */
class TypeReferenceTest {

    @Test
    void capturesSimpleType() {
        var ref = new TypeReference<String>() { };
        assertThat(ref.getType()).isEqualTo(String.class);
    }

    @Test
    void capturesParameterizedType() {
        var ref = new TypeReference<List<String>>() { };
        assertThat(ref.getType()).isInstanceOf(ParameterizedType.class);
        var pt = (ParameterizedType) ref.getType();
        assertThat(pt.getRawType()).isEqualTo(List.class);
        assertThat(pt.getActualTypeArguments()[0]).isEqualTo(String.class);
    }

    @Test
    void maxSerializer_interfaceExists() {
        // Verify the interface compiles and has expected method count
        assertThat(MaxSerializer.class.getMethods()).hasSizeGreaterThanOrEqualTo(3);
    }
}
