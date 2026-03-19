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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.core.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MaxQuery}.
 */
class MaxQueryTest {

    private final MaxSerializer stubSerializer = new MaxSerializer() {
        @Override
        public <T> String serialize(T object) {
            return "\"serialized\"";
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T deserialize(String json, Class<T> type) {
            return (T) json;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T deserialize(String json, TypeReference<T> type) {
            return (T) json;
        }
    };

    private MaxClient createClient() {
        MaxTransportClient transport = new MaxTransportClient() {
            @Override
            public MaxResponse execute(MaxRequest request) {
                return new MaxResponse(200, "{\"ok\":true}", Map.of());
            }

            @Override
            public CompletableFuture<MaxResponse> executeAsync(MaxRequest request) {
                return CompletableFuture.completedFuture(execute(request));
            }

            @Override
            public void close() {
            }
        };
        return new MaxClient(transport, stubSerializer,
                MaxClientConfig.builder().enableRateLimiting(false).build());
    }

    /** Concrete subclass for testing. */
    static class TestQuery extends MaxQuery<String> {
        TestQuery(MaxClient client) {
            super(client, "/test", HttpMethod.GET, String.class);
        }

        TestQuery withParam(String key, String value) {
            queryParams.put(key, value);
            return this;
        }

        TestQuery withBody(Object body) {
            this.body = body;
            return this;
        }
    }

    @Test
    void executeReturnsResult() {
        MaxClient client = createClient();
        TestQuery query = new TestQuery(client);
        String result = query.execute();
        assertThat(result).isEqualTo("{\"ok\":true}");
    }

    @Test
    void enqueueReturnsResultAsync() throws ExecutionException, InterruptedException {
        MaxClient client = createClient();
        TestQuery query = new TestQuery(client);
        String result = query.enqueue().get();
        assertThat(result).isEqualTo("{\"ok\":true}");
    }

    @Test
    void pathReturnsCorrectValue() {
        MaxClient client = createClient();
        TestQuery query = new TestQuery(client);
        assertThat(query.path()).isEqualTo("/test");
    }

    @Test
    void methodReturnsCorrectValue() {
        MaxClient client = createClient();
        TestQuery query = new TestQuery(client);
        assertThat(query.method()).isEqualTo(HttpMethod.GET);
    }

    @Test
    void queryParamsAreChainable() {
        MaxClient client = createClient();
        TestQuery query = new TestQuery(client)
                .withParam("key1", "val1")
                .withParam("key2", "val2");
        assertThat(query.queryParams()).containsEntry("key1", "val1");
        assertThat(query.queryParams()).containsEntry("key2", "val2");
    }

    @Test
    void bodyCanBeSet() {
        MaxClient client = createClient();
        TestQuery query = new TestQuery(client).withBody("body_object");
        assertThat(query.body()).isEqualTo("body_object");
    }

    @Test
    void nullBodyReturnsNull() {
        MaxClient client = createClient();
        TestQuery query = new TestQuery(client);
        assertThat(query.body()).isNull();
    }

    @Test
    void nullClientThrows() {
        assertThatThrownBy(() -> new TestQuery(null))
                .isInstanceOf(NullPointerException.class);
    }
}
