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
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.max.botapi.jackson.JacksonMaxSerializer;
import ru.max.botapi.model.UploadedInfo;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MaxUploadAPI}.
 */
@WireMockTest
class MaxUploadAPITest {

    @Test
    void uploadsBytesToUploadEndpoint(WireMockRuntimeInfo wmInfo) {
        stubFor(post(urlPathEqualTo("/upload"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"token\": \"uploaded-token-abc\"}")));

        MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer());
        byte[] data = "hello world".getBytes();
        UploadedInfo info = uploadApi.upload(wmInfo.getHttpBaseUrl() + "/upload", data, "test.txt");

        assertThat(info).isNotNull();
        assertThat(info.token()).isEqualTo("uploaded-token-abc");
    }

    @Test
    void throwsOnNonSuccessHttpStatus(WireMockRuntimeInfo wmInfo) {
        stubFor(post(urlPathEqualTo("/upload"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer());
        byte[] data = "test".getBytes();

        assertThatThrownBy(() -> uploadApi.upload(wmInfo.getHttpBaseUrl() + "/upload", data,
                "test.txt"))
                .isInstanceOf(MaxClientException.class);
    }

    @Test
    void constructsWithExplicitSerializer() {
        MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer());
        assertThat(uploadApi).isNotNull();
    }

    @Test
    void uploadsFileViaStreamingPath(WireMockRuntimeInfo wmInfo, @TempDir Path tempDir)
            throws IOException {
        stubFor(post(urlPathEqualTo("/upload"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"token\": \"stream-token-xyz\"}")));

        // Write a small temp file — this tests the streaming (Path-based) overload
        Path testFile = tempDir.resolve("test-stream.txt");
        Files.writeString(testFile, "streaming file content");

        try (MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer())) {
            UploadedInfo info = uploadApi.upload(
                    wmInfo.getHttpBaseUrl() + "/upload", testFile, "test-stream.txt");

            assertThat(info).isNotNull();
            assertThat(info.token()).isEqualTo("stream-token-xyz");
        }
    }

    @Test
    void uploadsFileObjectViaStreamingDelegate(WireMockRuntimeInfo wmInfo, @TempDir Path tempDir)
            throws IOException {
        stubFor(post(urlPathEqualTo("/upload"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"token\": \"file-obj-token\"}")));

        Path testFile = tempDir.resolve("file-obj.txt");
        Files.writeString(testFile, "file object content");
        File file = testFile.toFile();

        try (MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer())) {
            UploadedInfo info = uploadApi.upload(wmInfo.getHttpBaseUrl() + "/upload", file);

            assertThat(info).isNotNull();
            assertThat(info.token()).isEqualTo("file-obj-token");
        }
    }

    @Test
    void closeIsIdempotentAndDoesNotThrow() {
        MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer());
        uploadApi.close();
        // Calling close() a second time should not throw
        uploadApi.close();
    }
}
