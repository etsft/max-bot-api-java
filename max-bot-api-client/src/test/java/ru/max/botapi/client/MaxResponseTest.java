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
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MaxResponse}.
 */
class MaxResponseTest {

    @Test
    void constructionWithAllFields() {
        MaxResponse response = new MaxResponse(
                200, "{\"success\":true}",
                Map.of("Content-Type", List.of("application/json"))
        );
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"success\":true}");
        assertThat(response.headers()).containsKey("Content-Type");
    }

    @Test
    void nullBodyThrows() {
        assertThatThrownBy(() -> new MaxResponse(200, null, Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullHeadersThrows() {
        assertThatThrownBy(() -> new MaxResponse(200, "", null))
                .isInstanceOf(NullPointerException.class);
    }
}
