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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collection;
import java.util.Objects;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import ru.max.botapi.model.Nullable;

/**
 * Configuration for the MAX Bot API client.
 *
 * @param baseUrl              API base URL
 * @param connectTimeout       HTTP connection timeout
 * @param requestTimeout       HTTP request timeout
 * @param longPollTimeout      timeout for long-polling requests
 * @param maxRetries           maximum number of retries for retryable errors
 * @param enableRateLimiting   whether to enable client-side rate limiting
 * @param maxRequestsPerSecond maximum requests per second when rate limiting is enabled
 * @param sslContext           SSL context for HTTPS requests, or {@code null} for the JVM default
 */
public record MaxClientConfig(
        String baseUrl,
        Duration connectTimeout,
        Duration requestTimeout,
        Duration longPollTimeout,
        int maxRetries,
        boolean enableRateLimiting,
        int maxRequestsPerSecond,
        @Nullable SSLContext sslContext
) {

    private static final String[] BUNDLED_TRUSTED_CERTIFICATE_RESOURCES = {
            "/ru/max/botapi/client/certificates/russian_trusted_root_ca_pem.crt",
            "/ru/max/botapi/client/certificates/russian_trusted_sub_ca_2024_pem.crt"
    };

    /**
     * Creates a MaxClientConfig.
     *
     * @param baseUrl         must not be {@code null}
     * @param connectTimeout  must not be {@code null}
     * @param requestTimeout  must not be {@code null}
     * @param longPollTimeout must not be {@code null}; must be less than {@code requestTimeout}
     * @throws IllegalArgumentException if {@code longPollTimeout >= requestTimeout}
     */
    public MaxClientConfig {
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
        Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
        Objects.requireNonNull(longPollTimeout, "longPollTimeout must not be null");
        if (longPollTimeout.compareTo(requestTimeout) >= 0) {
            throw new IllegalArgumentException(
                    "longPollTimeout (" + longPollTimeout.toSeconds() + "s) must be less than "
                    + "requestTimeout (" + requestTimeout.toSeconds() + "s); otherwise the HTTP "
                    + "request will time out before the server can respond");
        }
    }

    /**
     * Returns a configuration with sensible defaults.
     *
     * @return default configuration
     */
    public static MaxClientConfig defaults() {
        return builder().build();
    }

    /**
     * Returns the default SSL context used by this client.
     *
     * <p>The context trusts both the JVM default certificate authorities and the
     * bundled Russian Trusted Root/Sub CA certificates required by MAX.</p>
     *
     * @return the default SSL context
     */
    public static SSLContext defaultSslContext() {
        return DefaultSslContextHolder.INSTANCE;
    }

    /**
     * Returns a new builder initialized with default values.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MaxClientConfig}.
     */
    public static class Builder {

        private String baseUrl = "https://platform-api2.max.ru";
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration requestTimeout = Duration.ofSeconds(60);
        private Duration longPollTimeout = Duration.ofSeconds(30);
        private int maxRetries = 3;
        private boolean enableRateLimiting = true;
        private int maxRequestsPerSecond = 30;
        private @Nullable SSLContext sslContext;
        private boolean customSslContext;
        private boolean useBundledTrustedCertificates = true;
        private Path[] trustedCertificateFiles = new Path[0];

        Builder() {
        }

        /**
         * Sets the API base URL.
         *
         * @param baseUrl the base URL
         * @return this builder
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = Objects.requireNonNull(baseUrl);
            return this;
        }

        /**
         * Sets the connection timeout.
         *
         * @param connectTimeout the connection timeout
         * @return this builder
         */
        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = Objects.requireNonNull(connectTimeout);
            return this;
        }

        /**
         * Sets the request timeout.
         *
         * @param requestTimeout the request timeout
         * @return this builder
         */
        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = Objects.requireNonNull(requestTimeout);
            return this;
        }

        /**
         * Sets the long-poll timeout.
         *
         * @param longPollTimeout the long-poll timeout
         * @return this builder
         */
        public Builder longPollTimeout(Duration longPollTimeout) {
            this.longPollTimeout = Objects.requireNonNull(longPollTimeout);
            return this;
        }

        /**
         * Sets the maximum number of retries.
         *
         * @param maxRetries the maximum retry count
         * @return this builder
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Sets whether to enable rate limiting.
         *
         * @param enableRateLimiting {@code true} to enable
         * @return this builder
         */
        public Builder enableRateLimiting(boolean enableRateLimiting) {
            this.enableRateLimiting = enableRateLimiting;
            return this;
        }

        /**
         * Sets the maximum requests per second.
         *
         * @param maxRequestsPerSecond the max RPS
         * @return this builder
         */
        public Builder maxRequestsPerSecond(int maxRequestsPerSecond) {
            this.maxRequestsPerSecond = maxRequestsPerSecond;
            return this;
        }

        /**
         * Sets a custom SSL context for HTTPS requests.
         *
         * @param sslContext the SSL context to use
         * @return this builder
         */
        public Builder sslContext(SSLContext sslContext) {
            this.sslContext = Objects.requireNonNull(sslContext);
            this.customSslContext = true;
            return this;
        }

        /**
         * Adds X.509 certificate files to the HTTPS trust configuration.
         *
         * <p>The resulting SSL context trusts both the default JDK certificate authorities
         * and the bundled MAX trusted certificates. PEM and DER encoded {@code .crt}/{@code .cer}
         * files provided here are added on top.</p>
         *
         * @param certificateFiles certificate files to trust
         * @return this builder
         * @throws IllegalArgumentException if no certificate files are provided
         */
        public Builder trustedCertificates(Path... certificateFiles) {
            this.trustedCertificateFiles = requireCertificateFiles(certificateFiles);
            this.customSslContext = false;
            return this;
        }

        /**
         * Disables bundled MAX trusted certificates and uses the JVM default trust store only.
         *
         * <p>If {@link #trustedCertificates(Path...)} is also configured, those certificates are
         * added to the JVM default trust store without the bundled certificates.</p>
         *
         * @return this builder
         */
        public Builder withoutBundledTrustedCertificates() {
            this.useBundledTrustedCertificates = false;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return the built MaxClientConfig
         * @throws MaxClientException if configured certificates cannot be loaded
         */
        public MaxClientConfig build() {
            SSLContext resolvedSslContext = customSslContext
                    ? sslContext
                    : resolveSslContext(useBundledTrustedCertificates, trustedCertificateFiles);
            return new MaxClientConfig(
                    baseUrl, connectTimeout, requestTimeout, longPollTimeout,
                    maxRetries, enableRateLimiting, maxRequestsPerSecond, resolvedSslContext
            );
        }
    }

    private static @Nullable SSLContext resolveSslContext(
            boolean useBundledTrustedCertificates,
            Path... certificateFiles
    ) {
        if (useBundledTrustedCertificates && certificateFiles.length == 0) {
            return defaultSslContext();
        }
        if (!useBundledTrustedCertificates && certificateFiles.length == 0) {
            return null;
        }
        return createSslContext(useBundledTrustedCertificates, certificateFiles);
    }

    private static SSLContext createSslContext(boolean useBundledTrustedCertificates, Path... certificateFiles) {
        Objects.requireNonNull(certificateFiles, "certificateFiles must not be null");
        if (!useBundledTrustedCertificates && certificateFiles.length == 0) {
            throw new IllegalArgumentException("certificateFiles must not be empty");
        }
        try {
            KeyStore additionalTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            additionalTrustStore.load(null, null);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            int certificateIndex = 0;
            if (useBundledTrustedCertificates) {
                for (String resourceName : BUNDLED_TRUSTED_CERTIFICATE_RESOURCES) {
                    certificateIndex = loadCertificateResource(
                            certificateFactory, additionalTrustStore, resourceName, certificateIndex);
                }
            }
            for (Path certificateFile : certificateFiles) {
                certificateIndex = loadCertificateFile(
                        certificateFactory, additionalTrustStore, certificateFile, certificateIndex);
            }

            TrustManagerFactory defaultFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            defaultFactory.init((KeyStore) null);

            TrustManagerFactory additionalFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            additionalFactory.init(additionalTrustStore);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] {
                    new CompositeX509TrustManager(
                            findX509TrustManager(defaultFactory.getTrustManagers()),
                            findX509TrustManager(additionalFactory.getTrustManagers()))
            }, null);
            return context;
        } catch (IOException | GeneralSecurityException e) {
            throw new MaxClientException("Failed to load trusted certificates", e);
        }
    }

    private static Path[] requireCertificateFiles(Path... certificateFiles) {
        Objects.requireNonNull(certificateFiles, "certificateFiles must not be null");
        if (certificateFiles.length == 0) {
            throw new IllegalArgumentException("certificateFiles must not be empty");
        }
        Path[] trustedFiles = certificateFiles.clone();
        for (Path certificateFile : trustedFiles) {
            Objects.requireNonNull(certificateFile, "certificateFile must not be null");
        }
        return trustedFiles;
    }

    private static int loadCertificateFile(CertificateFactory certificateFactory, KeyStore trustStore,
            Path certificateFile, int certificateIndex) throws IOException, GeneralSecurityException {
        Objects.requireNonNull(certificateFile, "certificateFile must not be null");
        try (InputStream input = Files.newInputStream(certificateFile)) {
            return loadCertificates(
                    certificateFactory, trustStore, input, certificateFile.toString(), certificateIndex);
        }
    }

    private static int loadCertificateResource(CertificateFactory certificateFactory, KeyStore trustStore,
            String resourceName, int certificateIndex) throws IOException, GeneralSecurityException {
        try (InputStream input = MaxClientConfig.class.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new CertificateException("Certificate resource not found: " + resourceName);
            }
            return loadCertificates(certificateFactory, trustStore, input, resourceName, certificateIndex);
        }
    }

    private static int loadCertificates(CertificateFactory certificateFactory, KeyStore trustStore,
            InputStream input, String source, int certificateIndex) throws GeneralSecurityException {
        Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(input);
        if (certificates.isEmpty()) {
            throw new CertificateException("No X.509 certificates found in " + source);
        }
        int nextIndex = certificateIndex;
        for (Certificate certificate : certificates) {
            trustStore.setCertificateEntry("max-extra-ca-" + nextIndex, certificate);
            nextIndex++;
        }
        return nextIndex;
    }

    private static X509TrustManager findX509TrustManager(TrustManager[] trustManagers) throws CertificateException {
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager x509TrustManager) {
                return x509TrustManager;
            }
        }
        throw new CertificateException("No X.509 trust manager available");
    }

    private static final class DefaultSslContextHolder {

        private static final SSLContext INSTANCE = createSslContext(true);
    }

    private static final class CompositeX509TrustManager implements X509TrustManager {

        private final X509TrustManager defaultTrustManager;
        private final X509TrustManager additionalTrustManager;

        private CompositeX509TrustManager(
                X509TrustManager defaultTrustManager,
                X509TrustManager additionalTrustManager
        ) {
            this.defaultTrustManager = defaultTrustManager;
            this.additionalTrustManager = additionalTrustManager;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                defaultTrustManager.checkClientTrusted(chain, authType);
            } catch (CertificateException defaultException) {
                try {
                    additionalTrustManager.checkClientTrusted(chain, authType);
                } catch (CertificateException additionalException) {
                    throwWithSuppressedDefault(additionalException, defaultException);
                }
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException defaultException) {
                try {
                    additionalTrustManager.checkServerTrusted(chain, authType);
                } catch (CertificateException additionalException) {
                    throwWithSuppressedDefault(additionalException, defaultException);
                }
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            X509Certificate[] defaultIssuers = defaultTrustManager.getAcceptedIssuers();
            X509Certificate[] additionalIssuers = additionalTrustManager.getAcceptedIssuers();
            X509Certificate[] issuers = new X509Certificate[defaultIssuers.length + additionalIssuers.length];
            System.arraycopy(defaultIssuers, 0, issuers, 0, defaultIssuers.length);
            System.arraycopy(additionalIssuers, 0, issuers, defaultIssuers.length, additionalIssuers.length);
            return issuers;
        }

        private static void throwWithSuppressedDefault(
                CertificateException additionalException,
                CertificateException defaultException
        ) throws CertificateException {
            additionalException.addSuppressed(defaultException);
            throw additionalException;
        }
    }
}
