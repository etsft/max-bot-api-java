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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.core.TypeReference;
import ru.max.botapi.model.Nullable;

/**
 * Client orchestrator for the MAX Bot API.
 *
 * <p>Coordinates transport, serialization, rate limiting, retries,
 * and error mapping. This is the main entry point for executing API requests.</p>
 */
public class MaxClient {

    private static final Logger LOG = LoggerFactory.getLogger(MaxClient.class);

    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_METHOD_NOT_ALLOWED = 405;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_SERVER_ERROR = 500;

    /**
     * Error code MAX returns while an uploaded attachment is still being processed. The API
     * answers HTTP 400 with {@code {"code":"attachment.not.ready","message":"Key:
     * errors.process.attachment.file.not.processed"}}; the underscore spelling is accepted as
     * well because it appeared in earlier documentation.
     */
    private static final String ERROR_ATTACHMENT_NOT_READY = "attachment.not.ready";
    private static final String ERROR_ATTACHMENT_NOT_READY_LEGACY = "attachment_not_ready";

    /** Message key prefix and suffix of the same condition, used when the code is absent. */
    private static final String NOT_PROCESSED_PREFIX = "Key: errors.process.attachment.";
    private static final String NOT_PROCESSED_SUFFIX = ".not.processed";

    private static final Executor VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final MaxTransportClient transport;
    private final MaxSerializer serializer;
    private final RetryPolicy retryPolicy;
    private final Duration attachmentReadyTimeout;
    private final @Nullable RateLimiter rateLimiter;

    /**
     * Creates a MaxClient.
     *
     * @param transport  the HTTP transport client
     * @param serializer the JSON serializer
     * @param config     client configuration
     */
    public MaxClient(MaxTransportClient transport, MaxSerializer serializer, MaxClientConfig config) {
        this.transport = Objects.requireNonNull(transport, "transport must not be null");
        this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
        Objects.requireNonNull(config, "config must not be null");
        this.retryPolicy = new RetryPolicy(config.maxRetries());
        this.attachmentReadyTimeout = config.attachmentReadyTimeout();
        this.rateLimiter = config.enableRateLimiting()
                ? new RateLimiter(config.maxRequestsPerSecond())
                : null;
    }

    /**
     * Creates a MaxClient with a custom retry policy (for testing or advanced configuration).
     *
     * @param transport   the HTTP transport client
     * @param serializer  the JSON serializer
     * @param config      client configuration
     * @param retryPolicy custom retry policy
     */
    MaxClient(MaxTransportClient transport, MaxSerializer serializer, MaxClientConfig config,
            RetryPolicy retryPolicy) {
        this.transport = Objects.requireNonNull(transport, "transport must not be null");
        this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
        Objects.requireNonNull(config, "config must not be null");
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy must not be null");
        this.attachmentReadyTimeout = config.attachmentReadyTimeout();
        this.rateLimiter = config.enableRateLimiting()
                ? new RateLimiter(config.maxRequestsPerSecond())
                : null;
    }

    /**
     * Returns the JSON serializer used by this client.
     *
     * @return the serializer; never {@code null}
     */
    public MaxSerializer serializer() {
        return serializer;
    }

    /**
     * Executes a request and deserializes the response to the given class.
     *
     * @param request      the API request
     * @param responseType the response class
     * @param <T>          the response type
     * @return the deserialized response
     * @throws MaxApiException    if the API returns an error
     * @throws MaxClientException if a transport error occurs
     */
    public <T> T execute(MaxRequest request, Class<T> responseType) {
        MaxResponse response = executeWithRetry(request);
        return serializer.deserialize(response.body(), responseType);
    }

    /**
     * Executes a request and deserializes the response to the given generic type.
     *
     * @param request      the API request
     * @param responseType the response type reference
     * @param <T>          the response type
     * @return the deserialized response
     * @throws MaxApiException    if the API returns an error
     * @throws MaxClientException if a transport error occurs
     */
    public <T> T execute(MaxRequest request, TypeReference<T> responseType) {
        MaxResponse response = executeWithRetry(request);
        return serializer.deserialize(response.body(), responseType);
    }

    /**
     * Executes a request asynchronously and deserializes the response.
     *
     * @param request      the API request
     * @param responseType the response class
     * @param <T>          the response type
     * @return a future with the deserialized response
     */
    public <T> CompletableFuture<T> executeAsync(MaxRequest request, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> execute(request, responseType), VIRTUAL_EXECUTOR);
    }

    /**
     * Executes a request asynchronously and deserializes the response.
     *
     * @param request      the API request
     * @param responseType the response type reference
     * @param <T>          the response type
     * @return a future with the deserialized response
     */
    public <T> CompletableFuture<T> executeAsync(MaxRequest request, TypeReference<T> responseType) {
        return CompletableFuture.supplyAsync(() -> execute(request, responseType), VIRTUAL_EXECUTOR);
    }

    /**
     * Serializes the given body object to a JSON string.
     *
     * @param body the object to serialize, may be {@code null}
     * @return JSON string, or {@code null} if body is {@code null}
     */
    public @Nullable String serialize(@Nullable Object body) {
        return body == null ? null : serializer.serialize(body);
    }

    /**
     * Sends the request, resending it while MAX reports an attachment as not processed yet.
     *
     * <p>A rejected message was not created, so resending the identical request cannot post it
     * twice. Resends stop once the next one would start after {@code attachmentReadyTimeout};
     * the last {@link AttachmentNotReadyException} is then thrown.</p>
     */
    private MaxResponse executeWithRetry(MaxRequest request) {
        long deadline = System.nanoTime() + attachmentReadyTimeout.toNanos();
        int attempt = 0;
        while (true) {
            MaxResponse response = executeWithTransientRetry(request);
            if (response.statusCode() < HTTP_BAD_REQUEST) {
                return response;
            }
            MaxApiException exception = mapException(response);
            if (!(exception instanceof AttachmentNotReadyException)) {
                throw exception;
            }
            Duration delay = retryPolicy.attachmentRetryDelay(attempt);
            if (attachmentReadyTimeout.isZero() || System.nanoTime() + delay.toNanos() > deadline) {
                throw exception;
            }
            LOG.debug("Attachment not ready, resending request to {} after {}ms (attempt {})",
                    request.path(), delay.toMillis(), attempt + 1);
            sleep(delay);
            attempt++;
        }
    }

    private MaxResponse executeWithTransientRetry(MaxRequest request) {
        acquireRateLimit();
        MaxResponse response = transport.execute(request);
        int attempt = 0;
        while (response.statusCode() >= 400 && retryPolicy.shouldRetry(response.statusCode())
                && attempt < retryPolicy.maxRetries()) {
            Duration delay = getRetryDelay(response, attempt);
            LOG.debug("Retrying request to {} after {}ms (attempt {}/{})",
                    request.path(), delay.toMillis(), attempt + 1, retryPolicy.maxRetries());
            sleep(delay);
            acquireRateLimit();
            response = transport.execute(request);
            attempt++;
        }
        return response;
    }

    private Duration getRetryDelay(MaxResponse response, int attempt) {
        if (response.statusCode() == HTTP_TOO_MANY_REQUESTS) {
            List<String> retryAfterValues = response.headers().get("Retry-After");
            if (retryAfterValues != null && !retryAfterValues.isEmpty()) {
                try {
                    long seconds = Long.parseLong(retryAfterValues.getFirst());
                    return Duration.ofSeconds(seconds);
                } catch (NumberFormatException ignored) {
                    // fall through to exponential backoff
                }
            }
        }
        return retryPolicy.getDelay(attempt);
    }

    private void acquireRateLimit() {
        if (rateLimiter != null) {
            try {
                rateLimiter.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MaxClientException("Rate limit acquisition interrupted", e);
            }
        }
    }

    private MaxApiException mapException(MaxResponse response) {
        int statusCode = response.statusCode();
        String body = response.body();
        String errorMessage = "Unknown error";
        String errorCode = null;
        if (body != null && !body.isBlank()) {
            try {
                errorMessage = extractJsonField(body, "message", body);
                errorCode = extractJsonField(body, "code", null);
            } catch (Exception e) {
                errorMessage = body;
            }
        }
        if (isAttachmentNotReady(statusCode, errorCode, errorMessage)) {
            return new AttachmentNotReadyException(statusCode, errorMessage, errorCode);
        }
        return switch (statusCode) {
            case HTTP_TOO_MANY_REQUESTS -> {
                Duration retryAfter = parseRetryAfter(response.headers());
                yield new MaxRateLimitException(statusCode, errorMessage, errorCode, retryAfter);
            }
            case HTTP_METHOD_NOT_ALLOWED ->
                    new MaxMethodNotAllowedException(statusCode, errorMessage, errorCode);
            default -> new MaxApiException(statusCode, errorMessage, errorCode);
        };
    }

    /**
     * Tells whether the error response means the attachment is still being processed.
     *
     * <p>MAX reports this as HTTP 400 with the code {@code attachment.not.ready}, not as the
     * HTTP 409 the earlier documentation described, so the status is only narrowed to 4xx and
     * the decision is made on the code. When the body carries no code the message key is used
     * instead: it names the media kind ({@code file}, {@code video}, ...) between a fixed
     * prefix and suffix.</p>
     *
     * @param statusCode   the HTTP status code
     * @param errorCode    the {@code code} field of the body, or {@code null}
     * @param errorMessage the {@code message} field of the body
     * @return {@code true} if the caller should retry the request unchanged
     */
    private static boolean isAttachmentNotReady(int statusCode, @Nullable String errorCode,
                                                String errorMessage) {
        if (statusCode < HTTP_BAD_REQUEST || statusCode >= HTTP_SERVER_ERROR) {
            return false;
        }
        if (ERROR_ATTACHMENT_NOT_READY.equals(errorCode)
                || ERROR_ATTACHMENT_NOT_READY_LEGACY.equals(errorCode)) {
            return true;
        }
        return errorCode == null
                && errorMessage.startsWith(NOT_PROCESSED_PREFIX)
                && errorMessage.endsWith(NOT_PROCESSED_SUFFIX);
    }

    /**
     * Minimal JSON field extractor to avoid coupling to a full JSON parser in the client module.
     * Parses simple string values from a flat JSON object for error body extraction.
     *
     * @param json         the JSON body string
     * @param fieldName    the field to extract
     * @param defaultValue value returned when the field is absent or not a string
     * @return the string value of the field, or {@code defaultValue}
     */
    private static @Nullable String extractJsonField(String json, String fieldName,
            @Nullable String defaultValue) {
        String search = "\"" + fieldName + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) {
            return defaultValue;
        }
        int colonIdx = json.indexOf(':', keyIdx + search.length());
        if (colonIdx < 0) {
            return defaultValue;
        }
        int valueStart = colonIdx + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
            return defaultValue;
        }
        int contentStart = valueStart + 1;
        int endQuote = json.indexOf('"', contentStart);
        if (endQuote < 0) {
            return defaultValue;
        }
        return json.substring(contentStart, endQuote);
    }

    private @Nullable Duration parseRetryAfter(Map<String, List<String>> headers) {
        List<String> values = headers.get("Retry-After");
        if (values != null && !values.isEmpty()) {
            try {
                return Duration.ofSeconds(Long.parseLong(values.getFirst()));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MaxClientException("Sleep interrupted during retry", e);
        }
    }
}
