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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

/**
 * Builds an {@link SSLContext} trusting both the JDK's default certificate authorities
 * and the Ministry of Digital Development root/intermediate certificates required to
 * reach some MAX API endpoints.
 */
public final class TrustStoreManager {

    private static final String[] CERTIFICATES = {
            "certs/rootca_ssl_rsa2022.crt",
            "certs/subca_ssl_rsa2024.crt"
    };

    private static final SSLContext SSL_CONTEXT;

    static {
        try {
            SSL_CONTEXT = createSslContext();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize SSL context", e);
        }
    }

    private TrustStoreManager() {
    }

    /**
     * Returns the shared {@link SSLContext} trusting the default JDK certificate
     * authorities plus the Ministry of Digital Development certificates.
     *
     * @return the shared SSL context
     */
    public static SSLContext getSslContext() {
        return SSL_CONTEXT;
    }

    private static SSLContext createSslContext() {
        try {
            KeyStore trustStore = loadDefaultTrustStore();
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            ClassLoader classLoader = TrustStoreManager.class.getClassLoader();

            for (String certificatePath : CERTIFICATES) {
                try (InputStream inputStream = classLoader.getResourceAsStream(certificatePath)) {
                    if (inputStream == null) {
                        throw new IllegalStateException("Certificate not found: " + certificatePath);
                    }

                    Certificate certificate = certificateFactory.generateCertificate(inputStream);
                    trustStore.setCertificateEntry(certificatePath, certificate);
                }
            }

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            return sslContext;
        } catch (Exception e) {
            throw new MaxSslInitializationException("Unable to initialize SSL context", e);
        }
    }

    private static KeyStore loadDefaultTrustStore() throws Exception {
        Path javaHome = Path.of(System.getProperty("java.home"));

        Path trustStorePath = Files.exists(javaHome.resolve("lib/security/cacerts"))
                ? javaHome.resolve("lib/security/cacerts")
                : javaHome.resolve("jre/lib/security/cacerts");

        if (!Files.exists(trustStorePath)) {
            throw new IllegalStateException("Default Java trust store not found.");
        }

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream inputStream = Files.newInputStream(trustStorePath)) {
            keyStore.load(inputStream, "changeit".toCharArray());
        }

        return keyStore;
    }
}
