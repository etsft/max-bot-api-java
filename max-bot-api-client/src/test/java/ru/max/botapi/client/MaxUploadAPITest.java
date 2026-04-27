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
import ru.max.botapi.model.FileUploadedInfo;
import ru.max.botapi.model.ImageUploadedInfo;
import ru.max.botapi.model.MediaUploadedInfo;
import ru.max.botapi.model.UploadEndpoint;

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

    // ===== FILE =====

    @Test
    void uploadFile_parsesFileIdAndToken(WireMockRuntimeInfo wmInfo) {
        stubFor(post(urlPathEqualTo("/upload"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"fileId\": 3343344796, \"token\": \"file-tok\"}")));

        try (MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer())) {
            UploadEndpoint endpoint = new UploadEndpoint(
                    wmInfo.getHttpBaseUrl() + "/upload", null);
            FileUploadedInfo info = uploadApi.uploadFile(
                    endpoint, "hello".getBytes(), "test.txt");

            assertThat(info.fileId()).isEqualTo(3343344796L);
            assertThat(info.token()).isEqualTo("file-tok");
        }
    }

    @Test
    void uploadFile_streamingPath(WireMockRuntimeInfo wmInfo, @TempDir Path tempDir)
            throws IOException {
        stubFor(post(urlPathEqualTo("/upload"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"fileId\": 1, \"token\": \"stream-tok\"}")));

        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "streaming");

        try (MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer())) {
            UploadEndpoint endpoint = new UploadEndpoint(
                    wmInfo.getHttpBaseUrl() + "/upload", null);
            FileUploadedInfo info = uploadApi.uploadFile(endpoint, testFile, "test.txt");
            assertThat(info.token()).isEqualTo("stream-tok");
            assertThat(info.fileId()).isEqualTo(1L);
        }
    }

    @Test
    void uploadFile_fileObject(WireMockRuntimeInfo wmInfo, @TempDir Path tempDir)
            throws IOException {
        stubFor(post(urlPathEqualTo("/upload"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"fileId\": 2, \"token\": \"file-obj\"}")));

        Path testFile = tempDir.resolve("file.txt");
        Files.writeString(testFile, "data");
        File file = testFile.toFile();

        try (MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer())) {
            UploadEndpoint endpoint = new UploadEndpoint(
                    wmInfo.getHttpBaseUrl() + "/upload", null);
            FileUploadedInfo info = uploadApi.uploadFile(endpoint, file);
            assertThat(info.token()).isEqualTo("file-obj");
        }
    }

    // ===== IMAGE =====

    @Test
    void uploadImage_parsesPhotosMap(WireMockRuntimeInfo wmInfo) {
        String body = "{\"photos\": {\"abc==\": {\"token\": \"photo-tok-1\"}}}";
        stubFor(post(urlPathEqualTo("/upload"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));

        try (MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer())) {
            UploadEndpoint endpoint = new UploadEndpoint(
                    wmInfo.getHttpBaseUrl() + "/upload", null);
            ImageUploadedInfo info = uploadApi.uploadImage(
                    endpoint, "img".getBytes(), "img.jpg");

            assertThat(info.photos()).hasSize(1);
            assertThat(info.photos().get("abc==").token()).isEqualTo("photo-tok-1");
        }
    }

    @Test
    void uploadImage_multiplePhotos(WireMockRuntimeInfo wmInfo) {
        String body = "{\"photos\": {"
                + "\"k1\": {\"token\": \"t1\"},"
                + "\"k2\": {\"token\": \"t2\"}}}";
        stubFor(post(urlPathEqualTo("/upload"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));

        try (MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer())) {
            UploadEndpoint endpoint = new UploadEndpoint(
                    wmInfo.getHttpBaseUrl() + "/upload", null);
            ImageUploadedInfo info = uploadApi.uploadImage(
                    endpoint, "img".getBytes(), "img.jpg");

            assertThat(info.photos()).hasSize(2);
            assertThat(info.photos().get("k1").token()).isEqualTo("t1");
            assertThat(info.photos().get("k2").token()).isEqualTo("t2");
        }
    }

    // ===== VIDEO / AUDIO =====

    @Test
    void uploadMedia_parsesRetvalAndCarriesEndpointToken(WireMockRuntimeInfo wmInfo) {
        stubFor(post(urlPathEqualTo("/upload"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBody("<?xml version=\"1.0\" encoding=\"UTF-8\" "
                                + "standalone=\"no\"?><retval>1</retval>")));

        try (MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer())) {
            UploadEndpoint endpoint = new UploadEndpoint(
                    wmInfo.getHttpBaseUrl() + "/upload", "video-tok-from-endpoint");
            MediaUploadedInfo info = uploadApi.uploadMedia(
                    endpoint, "data".getBytes(), "vid.mp4");

            assertThat(info.token()).isEqualTo("video-tok-from-endpoint");
            assertThat(info.retval()).isEqualTo(1);
        }
    }

    @Test
    void uploadMedia_throwsWhenEndpointTokenIsNull(WireMockRuntimeInfo wmInfo) {
        try (MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer())) {
            UploadEndpoint endpoint = new UploadEndpoint(
                    wmInfo.getHttpBaseUrl() + "/upload", null);

            assertThatThrownBy(() -> uploadApi.uploadMedia(
                    endpoint, "data".getBytes(), "vid.mp4"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("token");
        }
    }

    @Test
    void uploadMedia_streamingPath(WireMockRuntimeInfo wmInfo, @TempDir Path tempDir)
            throws IOException {
        stubFor(post(urlPathEqualTo("/upload"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBody("<retval>1</retval>")));

        Path testFile = tempDir.resolve("audio.mp3");
        Files.writeString(testFile, "x");

        try (MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer())) {
            UploadEndpoint endpoint = new UploadEndpoint(
                    wmInfo.getHttpBaseUrl() + "/upload", "audio-tok");
            MediaUploadedInfo info = uploadApi.uploadMedia(endpoint, testFile, "audio.mp3");
            assertThat(info.token()).isEqualTo("audio-tok");
            assertThat(info.retval()).isEqualTo(1);
        }
    }

    @Test
    void uploadMedia_fileObject(WireMockRuntimeInfo wmInfo, @TempDir Path tempDir)
            throws IOException {
        stubFor(post(urlPathEqualTo("/upload"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBody("<retval>1</retval>")));

        Path testFile = tempDir.resolve("vid.mp4");
        Files.writeString(testFile, "x");

        try (MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer())) {
            UploadEndpoint endpoint = new UploadEndpoint(
                    wmInfo.getHttpBaseUrl() + "/upload", "vid-tok");
            MediaUploadedInfo info = uploadApi.uploadMedia(endpoint, testFile.toFile());
            assertThat(info.token()).isEqualTo("vid-tok");
        }
    }

    @Test
    void uploadMedia_throwsOnUnparseableBody(WireMockRuntimeInfo wmInfo) {
        stubFor(post(urlPathEqualTo("/upload"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<wrong-element>oops</wrong-element>")));

        try (MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer())) {
            UploadEndpoint endpoint = new UploadEndpoint(
                    wmInfo.getHttpBaseUrl() + "/upload", "tok");
            assertThatThrownBy(() -> uploadApi.uploadMedia(
                    endpoint, "data".getBytes(), "vid.mp4"))
                    .isInstanceOf(MaxClientException.class)
                    .hasMessageContaining("retval");
        }
    }

    // ===== Common =====

    @Test
    void throwsOnNonSuccessHttpStatus(WireMockRuntimeInfo wmInfo) {
        stubFor(post(urlPathEqualTo("/upload"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        try (MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer())) {
            UploadEndpoint endpoint = new UploadEndpoint(
                    wmInfo.getHttpBaseUrl() + "/upload", null);
            assertThatThrownBy(() -> uploadApi.uploadFile(
                    endpoint, "test".getBytes(), "test.txt"))
                    .isInstanceOf(MaxClientException.class)
                    .hasMessageContaining("500");
        }
    }

    @Test
    void constructsWithExplicitSerializer() {
        try (MaxUploadAPI uploadApi = new MaxUploadAPI(new JacksonMaxSerializer())) {
            assertThat(uploadApi).isNotNull();
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
