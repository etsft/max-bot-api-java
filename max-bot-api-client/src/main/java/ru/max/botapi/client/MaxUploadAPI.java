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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.model.FileUploadedInfo;
import ru.max.botapi.model.ImageUploadedInfo;
import ru.max.botapi.model.MediaUploadedInfo;
import ru.max.botapi.model.UploadEndpoint;

/**
 * Provides file upload functionality to MAX platform upload endpoints.
 *
 * <p>The MAX upload flow is two-step: first call
 * {@link MaxBotAPI#getUploadUrl(ru.max.botapi.model.UploadType)} to obtain an
 * {@link UploadEndpoint}, then transfer the bytes to {@code endpoint.url()}. The shape of
 * the upload response and the moment when the attachment token becomes available depend
 * on the upload type:</p>
 *
 * <ul>
 *   <li>{@code FILE} — JSON response carries {@code fileId} and {@code token}. Use
 *       {@link #uploadFile(UploadEndpoint, Path, String)} or one of its overloads.</li>
 *   <li>{@code IMAGE} — JSON response carries a {@code photos} map of token references.
 *       Use {@link #uploadImage(UploadEndpoint, Path, String)} or one of its overloads.</li>
 *   <li>{@code VIDEO} / {@code AUDIO} — XML response ({@code <retval>1</retval>}) carries
 *       no token; the attachment token must come from {@link UploadEndpoint#token()}.
 *       Use {@link #uploadMedia(UploadEndpoint, Path, String)} or one of its overloads.</li>
 * </ul>
 *
 * <p>File-based uploads stream the file content directly from disk using
 * {@link HttpRequest.BodyPublishers#ofFile(Path)} without loading the entire file into
 * heap memory. This avoids {@code OutOfMemoryError} for large files such as videos.</p>
 *
 * <p>This class implements {@link AutoCloseable}; calling {@link #close()} shuts down the
 * internal executor service and releases all resources.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MaxBotAPI api = MaxBotAPI.create("my-token");
 * try (MaxUploadAPI uploadApi = new MaxUploadAPI()) {
 *     UploadEndpoint endpoint = api.getUploadUrl(UploadType.IMAGE).execute();
 *     ImageUploadedInfo info = uploadApi.uploadImage(endpoint, Path.of("photo.jpg"), "photo.jpg");
 *
 *     ImageAttachmentRequest att = new ImageAttachmentRequest(
 *         new PhotoAttachmentRequestPayload(null, null, info.photos()));
 *     api.sendMessage(new NewMessageBody("look", List.of(att), null, null, null))
 *         .chatId(chatId).execute();
 * }
 * }</pre>
 */
public class MaxUploadAPI implements AutoCloseable {

    private static final String MULTIPART_BOUNDARY_PREFIX = "----MaxBotApi";
    private static final String JACKSON_SERIALIZER_CLASS =
            "ru.max.botapi.jackson.JacksonMaxSerializer";
    private static final String CRLF = "\r\n";

    /** Matches the integer body of {@code <retval>...</retval>} responses (video/audio). */
    private static final Pattern RETVAL_PATTERN =
            Pattern.compile("<retval>\\s*(-?\\d+)\\s*</retval>");

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
     * @param serializer the JSON serializer for deserializing JSON upload responses
     *                   (file/image); must not be {@code null}
     */
    public MaxUploadAPI(MaxSerializer serializer) {
        this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.httpClient = HttpClient.newBuilder()
                .executor(executorService)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    // ===== FILE =====

    /**
     * Uploads a file at the given {@link Path} to the {@code FILE} upload endpoint.
     *
     * @param endpoint the endpoint returned by {@code getUploadUrl(UploadType.FILE)};
     *                 must not be {@code null}
     * @param filePath the path of the file to upload; must not be {@code null}
     * @param filename the filename to advertise in the multipart Content-Disposition;
     *                 must not be {@code null}
     * @return the parsed JSON response containing {@code fileId} and {@code token}
     * @throws MaxClientException if an I/O error occurs or the server returns a non-2xx status
     */
    public FileUploadedInfo uploadFile(UploadEndpoint endpoint, Path filePath, String filename) {
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        String body = transferFromFile(endpoint.url(), filePath, filename);
        return serializer.deserialize(body, FileUploadedInfo.class);
    }

    /**
     * Convenience overload accepting a {@link File} (delegates to the {@link Path} form).
     *
     * @param endpoint the upload endpoint; must not be {@code null}
     * @param file     the file to upload; must not be {@code null}
     * @return the parsed JSON response
     * @throws MaxClientException if an I/O error occurs
     */
    public FileUploadedInfo uploadFile(UploadEndpoint endpoint, File file) {
        Objects.requireNonNull(file, "file must not be null");
        return uploadFile(endpoint, file.toPath(), file.getName());
    }

    /**
     * Uploads in-memory data to the {@code FILE} upload endpoint.
     *
     * <p>Prefer the {@link Path} overload for large files to avoid heap exhaustion.</p>
     *
     * @param endpoint the upload endpoint; must not be {@code null}
     * @param data     the file bytes; must not be {@code null}
     * @param filename the filename to advertise in the multipart form; must not be {@code null}
     * @return the parsed JSON response
     * @throws MaxClientException if an I/O error occurs
     */
    public FileUploadedInfo uploadFile(UploadEndpoint endpoint, byte[] data, String filename) {
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        String body = transferFromBytes(endpoint.url(), data, filename);
        return serializer.deserialize(body, FileUploadedInfo.class);
    }

    // ===== IMAGE =====

    /**
     * Uploads a file at the given {@link Path} to the {@code IMAGE} upload endpoint.
     *
     * @param endpoint the endpoint returned by {@code getUploadUrl(UploadType.IMAGE)};
     *                 must not be {@code null}
     * @param filePath the path of the file to upload; must not be {@code null}
     * @param filename the filename to advertise; must not be {@code null}
     * @return the parsed JSON response containing the {@code photos} map
     * @throws MaxClientException if an I/O error occurs or the server returns a non-2xx status
     */
    public ImageUploadedInfo uploadImage(UploadEndpoint endpoint, Path filePath, String filename) {
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        String body = transferFromFile(endpoint.url(), filePath, filename);
        return serializer.deserialize(body, ImageUploadedInfo.class);
    }

    /**
     * Convenience overload accepting a {@link File} (delegates to the {@link Path} form).
     *
     * @param endpoint the upload endpoint; must not be {@code null}
     * @param file     the file to upload; must not be {@code null}
     * @return the parsed JSON response
     * @throws MaxClientException if an I/O error occurs
     */
    public ImageUploadedInfo uploadImage(UploadEndpoint endpoint, File file) {
        Objects.requireNonNull(file, "file must not be null");
        return uploadImage(endpoint, file.toPath(), file.getName());
    }

    /**
     * Uploads in-memory data to the {@code IMAGE} upload endpoint.
     *
     * @param endpoint the upload endpoint; must not be {@code null}
     * @param data     the image bytes; must not be {@code null}
     * @param filename the filename to advertise; must not be {@code null}
     * @return the parsed JSON response
     * @throws MaxClientException if an I/O error occurs
     */
    public ImageUploadedInfo uploadImage(UploadEndpoint endpoint, byte[] data, String filename) {
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        String body = transferFromBytes(endpoint.url(), data, filename);
        return serializer.deserialize(body, ImageUploadedInfo.class);
    }

    // ===== VIDEO / AUDIO =====

    /**
     * Uploads a file at the given {@link Path} to a {@code VIDEO} or {@code AUDIO} upload endpoint.
     *
     * <p>Unlike file/image uploads, the response carries no token; this method parses the
     * {@code <retval>...</retval>} XML body and forwards
     * {@link UploadEndpoint#token() endpoint.token()} as the attachment token. The endpoint
     * therefore <strong>must</strong> have a non-null token.</p>
     *
     * @param endpoint the endpoint returned by {@code getUploadUrl(UploadType.VIDEO)} or
     *                 {@code getUploadUrl(UploadType.AUDIO)}; must not be {@code null} and
     *                 must have a non-null {@code token}
     * @param filePath the path of the file to upload; must not be {@code null}
     * @param filename the filename to advertise; must not be {@code null}
     * @return a {@link MediaUploadedInfo} carrying the endpoint token and the parsed retval
     * @throws MaxClientException       if an I/O error occurs or the server returns a non-2xx
     *                                  status, or the XML body cannot be parsed
     * @throws IllegalArgumentException if {@code endpoint.token()} is {@code null}
     */
    public MediaUploadedInfo uploadMedia(UploadEndpoint endpoint, Path filePath, String filename) {
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        if (endpoint.token() == null) {
            throw new IllegalArgumentException(
                    "endpoint.token() must not be null for video/audio uploads — "
                    + "the upload response carries no token, the attachment token comes "
                    + "from the UploadEndpoint returned by POST /uploads.");
        }
        String body = transferFromFile(endpoint.url(), filePath, filename);
        return new MediaUploadedInfo(endpoint.token(), parseRetval(body));
    }

    /**
     * Convenience overload accepting a {@link File} (delegates to the {@link Path} form).
     *
     * @param endpoint the upload endpoint; must not be {@code null}
     * @param file     the file to upload; must not be {@code null}
     * @return the upload result
     * @throws MaxClientException if an I/O error occurs
     */
    public MediaUploadedInfo uploadMedia(UploadEndpoint endpoint, File file) {
        Objects.requireNonNull(file, "file must not be null");
        return uploadMedia(endpoint, file.toPath(), file.getName());
    }

    /**
     * Uploads in-memory data to a {@code VIDEO} or {@code AUDIO} upload endpoint.
     *
     * @param endpoint the upload endpoint; must not be {@code null}
     * @param data     the media bytes; must not be {@code null}
     * @param filename the filename to advertise; must not be {@code null}
     * @return the upload result
     * @throws MaxClientException if an I/O error occurs
     */
    public MediaUploadedInfo uploadMedia(UploadEndpoint endpoint, byte[] data, String filename) {
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        if (endpoint.token() == null) {
            throw new IllegalArgumentException(
                    "endpoint.token() must not be null for video/audio uploads.");
        }
        String body = transferFromBytes(endpoint.url(), data, filename);
        return new MediaUploadedInfo(endpoint.token(), parseRetval(body));
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

    // ===== Internal transport helpers =====

    /**
     * Streams the file content to {@code uploadUrl} via multipart/form-data.
     *
     * @param uploadUrl target URL
     * @param filePath  source file
     * @param filename  filename advertised in Content-Disposition
     * @return the response body as a string (may be JSON or XML depending on endpoint type)
     * @throws MaxClientException on transport, I/O, or non-2xx HTTP errors
     */
    private String transferFromFile(String uploadUrl, Path filePath, String filename) {
        Objects.requireNonNull(uploadUrl, "uploadUrl must not be null");
        Objects.requireNonNull(filePath, "filePath must not be null");
        Objects.requireNonNull(filename, "filename must not be null");

        String boundary = newBoundary();
        byte[] partHeader = buildPartHeader(boundary, filename);
        byte[] footer = (CRLF + "--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8);

        // Stream the file content without loading it into heap memory.
        // BodyPublishers.concat() (Java 21) sequences publishers without buffering.
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.concat(
                HttpRequest.BodyPublishers.ofByteArray(partHeader),
                ofFileSafe(filePath),
                HttpRequest.BodyPublishers.ofByteArray(footer));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(bodyPublisher)
                .build();

        return executeAndReadBody(request);
    }

    /**
     * Sends in-memory bytes to {@code uploadUrl} via multipart/form-data.
     *
     * @param uploadUrl target URL
     * @param data      payload bytes
     * @param filename  filename advertised in Content-Disposition
     * @return the response body as a string
     * @throws MaxClientException on transport, I/O, or non-2xx HTTP errors
     */
    private String transferFromBytes(String uploadUrl, byte[] data, String filename) {
        Objects.requireNonNull(uploadUrl, "uploadUrl must not be null");
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(filename, "filename must not be null");

        String boundary = newBoundary();
        byte[] multipartBody = buildMultipartBody(boundary, data, filename);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();

        return executeAndReadBody(request);
    }

    /**
     * Sends the request and returns the response body. Throws on non-2xx status.
     *
     * @param request the prepared request
     * @return the response body as a string
     * @throws MaxClientException on non-2xx status, I/O failure, or interruption
     */
    private String executeAndReadBody(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new MaxClientException(
                        "Upload failed with HTTP status " + status + ": " + response.body(),
                        new IOException("HTTP " + status));
            }
            return response.body();
        } catch (IOException e) {
            throw new MaxClientException("Upload request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MaxClientException("Upload request interrupted", e);
        }
    }

    /**
     * Parses the {@code <retval>...</retval>} integer from a video/audio upload response body.
     *
     * @param body the response body
     * @return the parsed integer
     * @throws MaxClientException if the body does not contain a parseable retval element
     */
    private static int parseRetval(String body) {
        if (body == null) {
            throw new MaxClientException(
                    "Empty upload response body; expected <retval>N</retval>",
                    new IOException("null body"));
        }
        Matcher m = RETVAL_PATTERN.matcher(body);
        if (!m.find()) {
            throw new MaxClientException(
                    "Unexpected upload response body, no <retval> element found: " + body,
                    new IOException("invalid response"));
        }
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            throw new MaxClientException(
                    "Unparseable retval value in upload response: " + m.group(1), e);
        }
    }

    private static String newBoundary() {
        return MULTIPART_BOUNDARY_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Wraps {@link HttpRequest.BodyPublishers#ofFile(Path)} and re-throws {@link IOException}
     * as {@link MaxClientException}.
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

    private static byte[] buildPartHeader(String boundary, String filename) {
        String header = "--" + boundary + CRLF
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\""
                + CRLF
                + "Content-Type: application/octet-stream" + CRLF
                + CRLF;
        return header.getBytes(StandardCharsets.UTF_8);
    }

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
