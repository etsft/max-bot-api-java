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

package ru.max.botapi.it;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import javax.imageio.ImageIO;

/**
 * Binary fixtures for the upload tests.
 *
 * <p>Images and plain files are generated so the suite runs with no preparation. Video and
 * audio are not: a synthesised container is likely to be rejected by the API for reasons that
 * have nothing to do with this library, which would turn a green run red for the wrong cause.
 * Those come from {@code MAX_IT_VIDEO_PATH} / {@code MAX_IT_AUDIO_PATH} or are skipped.</p>
 */
public final class Fixtures {

    private static final int IMAGE_SIZE = 64;

    private Fixtures() {
    }

    /**
     * Writes a small PNG and returns its path.
     *
     * @return path to the generated image
     */
    public static Path image() {
        Path target = directory().resolve("live-test-image.png");
        BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.decode("#2f6fed"));
            graphics.fillRect(0, 0, IMAGE_SIZE, IMAGE_SIZE);
            graphics.setColor(Color.WHITE);
            graphics.drawString("MAX", 8, IMAGE_SIZE / 2);
        } finally {
            graphics.dispose();
        }
        try {
            ImageIO.write(image, "png", target.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write the image fixture", e);
        }
        return target;
    }

    /**
     * Writes a small text file and returns its path.
     *
     * @return path to the generated file
     */
    public static Path textFile() {
        Path target = directory().resolve("live-test-file.txt");
        String content = "max-bot-api-java live integration test, generated at " + Instant.now();
        try {
            Files.writeString(target, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write the text fixture", e);
        }
        return target;
    }

    /**
     * Returns the real video file configured for the run, or skips the calling test.
     *
     * @return path to the video
     */
    public static Path video() {
        return IntegrationConfig.mediaPath(IntegrationConfig.VIDEO_PATH);
    }

    /**
     * Returns the real audio file configured for the run, or skips the calling test.
     *
     * @return path to the audio
     */
    public static Path audio() {
        return IntegrationConfig.mediaPath(IntegrationConfig.AUDIO_PATH);
    }

    private static Path directory() {
        Path dir = Path.of("build", "it-fixtures");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create the fixture directory " + dir, e);
        }
        return dir;
    }
}
