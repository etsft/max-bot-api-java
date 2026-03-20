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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.model.UploadedInfo;

/**
 * Provides file upload functionality to MAX platform upload endpoints.
 *
 * <p>The MAX upload flow is two-step:</p>
 * <ol>
 *   <li>Call {@link MaxBotAPI#getUploadUrl(ru.max.botapi.model.UploadType)} to obtain
 *       an upload URL.</li>
 *   <li>Call one of the {@code upload(...)} methods on this class with the obtained URL
 *       and file data.</li>
 * </ol>
 *
 * <p>File-based uploads ({@link #upload(String, Path, String)} and
 * {@link #upload(String, File)}) stream the file content directly from disk using
 * {@link HttpRequest.BodyPublishers#ofFile(Path)} without loading the entire file into
 * heap memory. This avoids {@code OutOfMemoryError} for large files such as videos.</p>
 *
 * <p>This class implements {@link AutoCloseable} so it can be used in try-with-resources blocks.
 * Calling {@link #close()} shuts down the internal executor service and releases all resources.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MaxBotAPI api = MaxBotAPI.create("my-token");
 * try (MaxUploadAPI uploadApi = new MaxUploadAPI()) {
 *     UploadEndpoint endpoint = api.getUploadUrl(UploadType.IMAGE).execute();
 *     UploadedInfo info = uploadApi.upload(endpoint.url(), Path.of("photo.jpg"), "photo.jpg");
 * }
 * }</pre>
 */
public class MaxUploadAPI implements AutoCloseable {

    private static final String MULTIPART_BOUNDARY_PREFIX = "----MaxBotApi";
    private static final String JACKSON_SERIALIZER_CLASS =
            "ru.max.botapi.jackson.JacksonMaxSerializer";
    private static final String CRLF = "\r\n";

    private final ExecutorService executorService;
    private final HttpClient httpClient;
    private final MaxSerializer serializer;

    /**
     * Creates a {@code MaxUploadAPI} using the Jackson serializer loaded via reflection.
     *
     * <p>Requires {@code max-bot-api-jackson} on the classpath.</p>
     *
     * @throws MaxClientException if the Jackson serializer cannot be instantiated
     */
    public MaxUploadAPI() {
        this(loadJacksonSerializer());
    }

    /**
     * Creates a {@code MaxUploadAPI} with an explicit serializer.
     *
     * @param serializer the JSON serializer for deserializing the upload response;
     *                   must not be {@code null}
     */
    public MaxUploadAPI(MaxSerializer serializer) {
        this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.httpClient = HttpClient.newBuilder()
                .executor(executorService)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Uploads a {@link File} to the given upload URL using multipart/form-data streaming.
     *
     * <p>The file is streamed from disk; its contents are not loaded into heap memory.
     * This is safe for arbitrarily large files.</p>
     *
     * @param uploadUrl the URL obtained from {@code POST /uploads}; must not be {@code null}
     * @param file      the file to upload; must not be {@code null}
     * @return the {@link UploadedInfo} containing the upload token
     * @throws MaxClientException if an I/O error occurs or the server returns an error response
     */
    public UploadedInfo upload(String uploadUrl, File file) {
        Objects.requireNonNull(uploadUrl, "uploadUrl must not be null");
        Objects.requireNonNull(file, "file must not be null");
        return upload(uploadUrl, file.toPath(), file.getName());
    }

    /**
     * Uploads a file at the given {@link Path} to the upload URL using multipart/form-data
     * streaming.
     *
     * <p>Uses {@link HttpRequest.BodyPublishers#concat(HttpRequest.BodyPublisher...)} to
     * concatenate the multipart header bytes, the file content (streamed via
     * {@link HttpRequest.BodyPublishers#ofFile(Path)}), and the multipart footer bytes without
     * buffering the file in memory. This avoids {@code OutOfMemoryError} on large video files.</p>
     *
     * @param uploadUrl the URL obtained from {@code POST /uploads}; must not be {@code null}
     * @param filePath  the path of the file to upload; must not be {@code null}
     * @param filename  the filename to use in the multipart Content-Disposition header;
     *                  must not be {@code null}
     * @return the {@link UploadedInfo} containing the upload token
     * @throws MaxClientException if an I/O error occurs or the server returns an error response
     */
    public UploadedInfo upload(String uploadUrl, Path filePath, String filename) {
        Objects.requireNonNull(uploadUrl, "uploadUrl must not be null");
        Objects.requireNonNull(filePath, "filePath must not be null");
        Objects.requireNonNull(filename, "filename must not be null");

        String boundary = MULTIPART_BOUNDARY_PREFIX
                + UUID.randomUUID().toString().replace("-", "");

        byte[] partHeader = buildPartHeader(boundary, filename);
        byte[] footer = (CRLF + "--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8);

        // Stream the file content without loading it into heap memory.
        // BodyPublishers.concat() is available in Java 21 and sequences publishers
        // without buffering intermediate data. Each constituent publisher knows its length,
        // so concat() can report the correct total content length to the HTTP layer.
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.concat(
                HttpRequest.BodyPublishers.ofByteArray(partHeader),
                ofFileSafe(filePath),
                HttpRequest.BodyPublishers.ofByteArray(footer));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(bodyPublisher)
                .build();

        return executeRequest(request);
    }

    /**
     * Uploads raw byte data to the given upload URL using multipart/form-data.
     *
     * <p>Use this overload when the data is already in memory (e.g., generated content or
     * small thumbnail images). For file-based uploads prefer
     * {@link #upload(String, Path, String)} to avoid heap exhaustion.</p>
     *
     * @param uploadUrl the URL obtained from {@code POST /uploads}; must not be {@code null}
     * @param data      the file content as a byte array; must not be {@code null}
     * @param filename  the filename to use in the multipart form; must not be {@code null}
     * @return the {@link UploadedInfo} containing the upload token
     * @throws MaxClientException if an I/O error occurs or the server returns an error response
     */
    public UploadedInfo upload(String uploadUrl, byte[] data, String filename) {
        Objects.requireNonNull(uploadUrl, "uploadUrl must not be null");
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(filename, "filename must not be null");

        String boundary = MULTIPART_BOUNDARY_PREFIX
                + UUID.randomUUID().toString().replace("-", "");

        byte[] multipartBody = buildMultipartBody(boundary, data, filename);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();

        return executeRequest(request);
    }

    /**
     * Shuts down the internal executor service, releasing resources held by the
     * underlying {@link HttpClient}.
     *
     * <p>Initiates an orderly shutdown and waits up to 5 seconds for any in-flight
     * uploads to complete. If the timeout expires, remaining tasks are cancelled.
     * After calling this method the upload API instance must not be used.</p>
     */
    @Override
    public void close() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Sends the given HTTP request and deserializes the response body as {@link UploadedInfo}.
     *
     * @param request the pre-built HTTP request
     * @return the deserialized upload result
     * @throws MaxClientException if the server returns a non-2xx status or an I/O error occurs
     */
    private UploadedInfo executeRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new MaxClientException(
                        "Upload failed with HTTP status " + status + ": " + response.body(),
                        new IOException("HTTP " + status));
            }
            return serializer.deserialize(response.body(), UploadedInfo.class);
        } catch (IOException e) {
            throw new MaxClientException("Upload request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MaxClientException("Upload request interrupted", e);
        }
    }

    /**
     * Wraps {@link HttpRequest.BodyPublishers#ofFile(Path)} and re-throws {@link IOException}
     * as {@link MaxClientException}.
     *
     * @param filePath the file to stream
     * @return a body publisher that streams the file content
     * @throws MaxClientException if the file cannot be read
     */
    private static HttpRequest.BodyPublisher ofFileSafe(Path filePath) {
        try {
            return HttpRequest.BodyPublishers.ofFile(filePath);
        } catch (IOException e) {
            throw new MaxClientException("Cannot open file for upload: " + filePath, e);
        }
    }

    /**
     * Loads the Jackson serializer via reflection to avoid a compile-time dependency on Jackson.
     *
     * @return the instantiated serializer
     * @throws MaxClientException if the class cannot be found or instantiated
     */
    private static MaxSerializer loadJacksonSerializer() {
        try {
            Class<?> clazz = Class.forName(JACKSON_SERIALIZER_CLASS);
            return (MaxSerializer) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new MaxClientException(
                    "Jackson serializer not found on classpath. "
                    + "Add 'max-bot-api-jackson' dependency or supply a custom MaxSerializer.", e);
        } catch (InvocationTargetException | InstantiationException
                | IllegalAccessException | NoSuchMethodException e) {
            throw new MaxClientException(
                    "Failed to instantiate Jackson serializer: " + JACKSON_SERIALIZER_CLASS, e);
        }
    }

    /**
     * Builds the multipart part header (boundary line + Content-Disposition + Content-Type).
     *
     * @param boundary the multipart boundary string (without leading {@code --})
     * @param filename the filename to include in the Content-Disposition header
     * @return the header bytes in UTF-8
     */
    private static byte[] buildPartHeader(String boundary, String filename) {
        String header = "--" + boundary + CRLF
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\""
                + CRLF
                + "Content-Type: application/octet-stream" + CRLF
                + CRLF;
        return header.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Builds a complete multipart/form-data body for the given binary data and filename.
     *
     * <p>Used only by the {@link #upload(String, byte[], String)} overload where the caller
     * already has the data in memory.</p>
     *
     * @param boundary the multipart boundary string
     * @param data     the file bytes
     * @param filename the filename to include in the Content-Disposition header
     * @return the encoded multipart body as a byte array
     */
    private static byte[] buildMultipartBody(String boundary, byte[] data, String filename) {
        byte[] header = buildPartHeader(boundary, filename);
        byte[] footer = (CRLF + "--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8);

        byte[] result = new byte[header.length + data.length + footer.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(data, 0, result, header.length, data.length);
        System.arraycopy(footer, 0, result, header.length + data.length, footer.length);
        return result;
    }
}
