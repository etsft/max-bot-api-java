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

package ru.max.botapi.testsupport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class that loads JSON fixtures from test resources on the classpath.
 *
 * <p>Fixtures are expected to reside under {@code /fixtures/} on the classpath.
 * For example, {@code loadFixture("bot/bot-info.json")} reads
 * {@code /fixtures/bot/bot-info.json}.
 */
public final class FixtureLoader {

    private FixtureLoader() {
        // utility class
    }

    /**
     * Loads a JSON fixture from the classpath.
     *
     * @param path relative path under {@code /fixtures/}, e.g. {@code "bot/bot-info.json"}
     * @return the fixture content as a string
     * @throws IllegalStateException if the fixture is not found
     */
    public static String loadFixture(String path) {
        String resourcePath = "/fixtures/" + path;
        try (InputStream is = FixtureLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Fixture not found on classpath: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read fixture: " + resourcePath, e);
        }
    }

    /**
     * Loads a JSON fixture from the classpath using a directory and filename.
     *
     * @param directory subdirectory under {@code /fixtures/}, e.g. {@code "bot"}
     * @param filename  the fixture filename, e.g. {@code "bot-info.json"}
     * @return the fixture content as a string
     * @throws IllegalStateException if the fixture is not found
     */
    public static String loadFixture(String directory, String filename) {
        return loadFixture(directory + "/" + filename);
    }
}
