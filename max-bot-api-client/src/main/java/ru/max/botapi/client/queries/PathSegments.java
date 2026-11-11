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

package ru.max.botapi.client.queries;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Builds URL path segments from caller-supplied values.
 *
 * <p>Identifiers such as message ids and video tokens are put into the request path. A value
 * holding {@code /}, {@code ?}, {@code #} or a space would otherwise change which endpoint is
 * called or produce a malformed URI, so every byte outside the RFC 3986 unreserved set
 * ({@code A-Z a-z 0-9 - . _ ~}) is percent-encoded. Ordinary ids like {@code mid.0a1b} pass
 * through unchanged.</p>
 */
final class PathSegments {

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private PathSegments() {
    }

    /**
     * Checks that the value is present and percent-encodes it as a single path segment.
     *
     * @param value the raw value; must not be {@code null}
     * @param name  the parameter name used in the exception message
     * @return the encoded segment
     * @throws NullPointerException if {@code value} is {@code null}
     */
    static String encode(String value, String name) {
        return encode(Objects.requireNonNull(value, () -> name + " must not be null"));
    }

    /**
     * Percent-encodes the value as a single path segment.
     *
     * @param value the raw value
     * @return the encoded segment
     */
    static String encode(String value) {
        StringBuilder sb = null;
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            if (isUnreserved(b)) {
                if (sb != null) {
                    sb.append((char) b);
                }
                continue;
            }
            if (sb == null) {
                sb = new StringBuilder(bytes.length + 16);
                sb.append(value, 0, i); // bytes before i are ASCII, so indexes match
            }
            sb.append('%').append(HEX[b >> 4]).append(HEX[b & 0x0F]);
        }
        return sb == null ? value : sb.toString();
    }

    private static boolean isUnreserved(int b) {
        return (b >= 'A' && b <= 'Z') || (b >= 'a' && b <= 'z') || (b >= '0' && b <= '9')
                || b == '-' || b == '.' || b == '_' || b == '~';
    }
}
