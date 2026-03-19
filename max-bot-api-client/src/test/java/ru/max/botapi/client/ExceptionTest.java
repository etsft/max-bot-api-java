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
import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the exception hierarchy.
 */
class ExceptionTest {

    @Test
    void maxApiExceptionFields() {
        MaxApiException ex = new MaxApiException(400, "Bad Request", "bad_request");
        assertThat(ex.statusCode()).isEqualTo(400);
        assertThat(ex.errorMessage()).isEqualTo("Bad Request");
        assertThat(ex.errorCode()).isEqualTo("bad_request");
        assertThat(ex.getMessage()).isEqualTo("HTTP 400: Bad Request");
    }

    @Test
    void maxApiExceptionWithNullErrorCode() {
        MaxApiException ex = new MaxApiException(500, "Internal Error", null);
        assertThat(ex.errorCode()).isNull();
    }

    @Test
    void maxClientExceptionWrapsIOException() {
        IOException cause = new IOException("connection refused");
        MaxClientException ex = new MaxClientException("Failed to connect", cause);
        assertThat(ex.getMessage()).isEqualTo("Failed to connect");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void maxRateLimitExceptionWithRetryAfter() {
        Duration retryAfter = Duration.ofSeconds(5);
        MaxRateLimitException ex = new MaxRateLimitException(429, "Too Many Requests",
                "rate_limit", retryAfter);
        assertThat(ex.statusCode()).isEqualTo(429);
        assertThat(ex.retryAfter()).isEqualTo(Duration.ofSeconds(5));
        assertThat(ex).isInstanceOf(MaxApiException.class);
    }

    @Test
    void maxRateLimitExceptionWithNullRetryAfter() {
        MaxRateLimitException ex = new MaxRateLimitException(429, "Too Many Requests",
                null, null);
        assertThat(ex.retryAfter()).isNull();
    }

    @Test
    void attachmentNotReadyException() {
        AttachmentNotReadyException ex = new AttachmentNotReadyException(
                409, "Attachment not ready", "attachment_not_ready");
        assertThat(ex.statusCode()).isEqualTo(409);
        assertThat(ex).isInstanceOf(MaxApiException.class);
    }
}
