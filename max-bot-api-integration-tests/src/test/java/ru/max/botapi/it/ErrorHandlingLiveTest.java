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

package ru.max.botapi.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import ru.max.botapi.client.MaxApiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * How the live API reports failures, and whether the client maps them onto the exception model.
 *
 * <p>The error body is parsed by a hand-rolled scanner rather than the serializer, so this is
 * the only place that verifies the real wire shape reaches {@code errorMessage} and
 * {@code errorCode}.</p>
 */
@Order(7)
@DisplayName("Live: error handling")
class ErrorHandlingLiveTest extends LiveTestBase {

    private static final long MISSING_CHAT_ID = 1L;

    @Test
    @Order(1)
    @DisplayName("a missing chat yields a mapped MaxApiException")
    void missingChat() {
        assertThatThrownBy(() -> api().getChat(MISSING_CHAT_ID).execute())
                .isInstanceOf(MaxApiException.class)
                .satisfies(thrown -> {
                    MaxApiException e = (MaxApiException) thrown;
                    assertThat(e.statusCode()).isBetween(400, 499);
                    assertThat(e.errorMessage()).isNotBlank();
                    System.out.println("Missing chat -> " + e.statusCode()
                            + " code=" + e.errorCode() + " message=" + e.errorMessage());
                });
    }

    @Test
    @Order(2)
    @DisplayName("an invalid token yields 401")
    void invalidToken() {
        try (LiveApi rejected = LiveApi.create("definitely-not-a-valid-token")) {
            assertThatThrownBy(() -> rejected.api().getMyInfo().execute())
                    .isInstanceOf(MaxApiException.class)
                    .satisfies(thrown -> assertThat(((MaxApiException) thrown).statusCode())
                            .isEqualTo(401));
        }
    }
}
