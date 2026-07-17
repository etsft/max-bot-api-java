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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MaxClientConfig}.
 */
class MaxClientConfigTest {

    private static final String TEST_CERTIFICATE = """
            -----BEGIN CERTIFICATE-----
            MIIDFzCCAf+gAwIBAgIUYMcbSPvqWdJ7PRIycUXMTuA2+qswDQYJKoZIhvcNAQEL
            BQAwGzEZMBcGA1UEAwwQbWF4LWJvdC1hcGktdGVzdDAeFw0yNjA3MTYwNTEwMzBa
            Fw0yNjA3MTcwNTEwMzBaMBsxGTAXBgNVBAMMEG1heC1ib3QtYXBpLXRlc3QwggEi
            MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDFwq7zwarcaPsqtEdRh5SQ2dke
            U+26GirILVObEObVqf+RTWlKR6st6Vzh+CKONUQ/8G+yuoRWEy9p4OnzTkEvw2fU
            GKODlZUzEOMg2CCH7RxcMOSa9Q0gIFy5o9bl4UGjHnzKTfFL7dY/svqklBUIgddx
            m+qaDjognkcJK9CUFbPHMnP9A811gwjmKlfYZPhmb7hBccNppdC8E5TABNdDflMb
            R/BM8wbv3GbRudCEfEnpOFiJDkUF+MXJ5nbahaw3ad1v9SBUBgPmpKOI5O1/C2Jc
            o2/I6DOterzlYTPFe1AFGsOnPJiHoXbPBDaxACiCrltKDQrjJbK3jd08a5xFAgMB
            AAGjUzBRMB0GA1UdDgQWBBQk9KGR6RN4ZNT0qDASmMIUis4d+jAfBgNVHSMEGDAW
            gBQk9KGR6RN4ZNT0qDASmMIUis4d+jAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3
            DQEBCwUAA4IBAQARpQ1qK7phToHvBp9KQYL+ODQ4p5FHFocUp38H+L7OIJLnIFa3
            yCEMzeMtq6Mc9uOVBKZ6w+yybCOYtnBvdynXVAYjXjSKRhBtwIVesJbGoo8OG7Le
            FvsR3BMhW79/3dwJONmCdEgs40hMGFVchwYQtTUdQHChiTDPFfRx99So51WlUNpk
            R5PID5KDZKwwKDuJfMiww0mpCHLwb1LFCyaJ00UJgGmRKdViNGf31d/xWg7d2YwS
            ursrWPO3xKY6fhrsyfwtUKtM08kuOpNhn7zYnyG93t4/pr/QGPA4Hpi9r69/ldla
            tqIGWJC4h9HN1Em6uyDuXHsIIa+9tkkNVpPM
            -----END CERTIFICATE-----
            """;

    @TempDir
    private Path tempDir;

    @Test
    void defaultsHaveExpectedValues() {
        MaxClientConfig config = MaxClientConfig.defaults();
        assertThat(config.baseUrl()).isEqualTo("https://platform-api2.max.ru");
        assertThat(config.connectTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(config.longPollTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.maxRetries()).isEqualTo(3);
        assertThat(config.enableRateLimiting()).isTrue();
        assertThat(config.maxRequestsPerSecond()).isEqualTo(30);
        assertThat(config.sslContext()).isSameAs(MaxClientConfig.defaultSslContext());
    }

    @Test
    void builderProducesCorrectConfig() throws Exception {
        SSLContext sslContext = SSLContext.getDefault();
        MaxClientConfig config = MaxClientConfig.builder()
                .baseUrl("https://custom.api.ru")
                .connectTimeout(Duration.ofSeconds(5))
                .requestTimeout(Duration.ofSeconds(120))
                .longPollTimeout(Duration.ofSeconds(60))
                .maxRetries(5)
                .enableRateLimiting(false)
                .maxRequestsPerSecond(50)
                .sslContext(sslContext)
                .build();
        assertThat(config.baseUrl()).isEqualTo("https://custom.api.ru");
        assertThat(config.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(120));
        assertThat(config.longPollTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(config.maxRetries()).isEqualTo(5);
        assertThat(config.enableRateLimiting()).isFalse();
        assertThat(config.maxRequestsPerSecond()).isEqualTo(50);
        assertThat(config.sslContext()).isSameAs(sslContext);
    }

    @Test
    void builderDefaultsMatchStaticDefaults() {
        MaxClientConfig fromBuilder = MaxClientConfig.builder().build();
        MaxClientConfig defaults = MaxClientConfig.defaults();
        assertThat(fromBuilder).isEqualTo(defaults);
    }

    @Test
    void nullBaseUrlThrows() {
        assertThatThrownBy(() -> MaxClientConfig.builder().baseUrl(null).build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullSslContextThrows() {
        assertThatThrownBy(() -> MaxClientConfig.builder().sslContext(null).build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void withoutBundledTrustedCertificatesUsesJvmDefaultSslContext() {
        MaxClientConfig config = MaxClientConfig.builder()
                .withoutBundledTrustedCertificates()
                .build();

        assertThat(config.sslContext()).isNull();
    }

    @Test
    void trustedCertificatesCanBeUsedWithoutBundledCertificates() throws Exception {
        Path certificate = tempDir.resolve("test-ca.crt");
        Files.writeString(certificate, TEST_CERTIFICATE);

        MaxClientConfig config = MaxClientConfig.builder()
                .withoutBundledTrustedCertificates()
                .trustedCertificates(certificate)
                .build();

        assertThat(config.sslContext()).isNotNull();
        assertThat(config.sslContext()).isNotSameAs(MaxClientConfig.defaultSslContext());
    }

    @Test
    void customSslContextOverridesTrustedCertificates() throws Exception {
        Path certificate = tempDir.resolve("invalid.crt");
        Files.writeString(certificate, "not a certificate");
        SSLContext sslContext = SSLContext.getDefault();

        MaxClientConfig config = MaxClientConfig.builder()
                .trustedCertificates(certificate)
                .sslContext(sslContext)
                .build();

        assertThat(config.sslContext()).isSameAs(sslContext);
    }

    @Test
    void trustedCertificatesLoadsPemFile() throws Exception {
        Path certificate = tempDir.resolve("test-ca.crt");
        Files.writeString(certificate, TEST_CERTIFICATE);

        MaxClientConfig config = MaxClientConfig.builder()
                .trustedCertificates(certificate)
                .build();

        assertThat(config.sslContext()).isNotNull();
        assertThat(config.sslContext().getProtocol()).isEqualTo("TLS");
    }

    @Test
    void trustedCertificatesRejectsEmptyList() {
        assertThatThrownBy(() -> MaxClientConfig.builder().trustedCertificates())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void trustedCertificatesRejectsNullArray() {
        Path[] certificateFiles = null;

        assertThatThrownBy(() -> MaxClientConfig.builder().trustedCertificates(certificateFiles))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void trustedCertificatesRejectsNullFile() {
        assertThatThrownBy(() -> MaxClientConfig.builder().trustedCertificates((Path) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void trustedCertificatesRejectsInvalidFile() throws Exception {
        Path certificate = tempDir.resolve("invalid.crt");
        Files.writeString(certificate, "not a certificate");

        assertThatThrownBy(() -> MaxClientConfig.builder().trustedCertificates(certificate).build())
                .isInstanceOf(MaxClientException.class);
    }

    @Test
    void longPollTimeoutGreaterThanRequestTimeoutThrows() {
        assertThatThrownBy(() -> MaxClientConfig.builder()
                .requestTimeout(Duration.ofSeconds(30))
                .longPollTimeout(Duration.ofSeconds(30))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longPollTimeout")
                .hasMessageContaining("requestTimeout");
    }

    @Test
    void longPollTimeoutEqualToRequestTimeoutThrows() {
        assertThatThrownBy(() -> MaxClientConfig.builder()
                .requestTimeout(Duration.ofSeconds(30))
                .longPollTimeout(Duration.ofSeconds(31))
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
