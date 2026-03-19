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

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MaxRequest}.
 */
class MaxRequestTest {

    @Test
    void constructionWithAllFields() {
        MaxRequest request = new MaxRequest(
                HttpMethod.POST, "/messages",
                Map.of("chat_id", "123"),
                "{\"text\":\"hello\"}",
                Map.of("X-Custom", "value")
        );
        assertThat(request.method()).isEqualTo(HttpMethod.POST);
        assertThat(request.path()).isEqualTo("/messages");
        assertThat(request.queryParams()).containsEntry("chat_id", "123");
        assertThat(request.body()).isEqualTo("{\"text\":\"hello\"}");
        assertThat(request.headers()).containsEntry("X-Custom", "value");
    }

    @Test
    void constructionWithNullBody() {
        MaxRequest request = new MaxRequest(
                HttpMethod.GET, "/me", Map.of(), null, Map.of());
        assertThat(request.body()).isNull();
    }

    @Test
    void queryParamsAreDefensivelyCopied() {
        var mutable = new java.util.HashMap<String, String>();
        mutable.put("key", "value");
        MaxRequest request = new MaxRequest(
                HttpMethod.GET, "/test", mutable, null, Map.of());
        mutable.put("key2", "value2");
        assertThat(request.queryParams()).doesNotContainKey("key2");
    }

    @Test
    void nullMethodThrows() {
        assertThatThrownBy(() -> new MaxRequest(
                null, "/test", Map.of(), null, Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullPathThrows() {
        assertThatThrownBy(() -> new MaxRequest(
                HttpMethod.GET, null, Map.of(), null, Map.of()))
                .isInstanceOf(NullPointerException.class);
    }
}
