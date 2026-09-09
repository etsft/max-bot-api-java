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

package ru.max.botapi.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson mix-in that maps the {@code VideoUrls} record components to the {@code mp4_1080},
 * {@code mp4_720}, ... JSON keys used by {@code GET /videos/{videoToken}}.
 *
 * <p>The mapper's {@code SNAKE_CASE} strategy only inserts underscores before capitals, so
 * {@code mp41080} would be looked up verbatim and every rendition would silently come back as
 * {@code null}. The components cannot be named {@code mp4_1080} instead: an underscore in a
 * member name is rejected by the project's checkstyle rules.</p>
 */
abstract class VideoUrlsMixIn {

    /**
     * Maps to {@code "mp4_1080"} in JSON.
     *
     * @return the 1080p MP4 URL
     */
    @JsonProperty("mp4_1080")
    abstract String mp41080();

    /**
     * Maps to {@code "mp4_720"} in JSON.
     *
     * @return the 720p MP4 URL
     */
    @JsonProperty("mp4_720")
    abstract String mp4720();

    /**
     * Maps to {@code "mp4_480"} in JSON.
     *
     * @return the 480p MP4 URL
     */
    @JsonProperty("mp4_480")
    abstract String mp4480();

    /**
     * Maps to {@code "mp4_360"} in JSON.
     *
     * @return the 360p MP4 URL
     */
    @JsonProperty("mp4_360")
    abstract String mp4360();

    /**
     * Maps to {@code "mp4_240"} in JSON.
     *
     * @return the 240p MP4 URL
     */
    @JsonProperty("mp4_240")
    abstract String mp4240();

    /**
     * Maps to {@code "mp4_144"} in JSON.
     *
     * @return the 144p MP4 URL
     */
    @JsonProperty("mp4_144")
    abstract String mp4144();

    /**
     * Maps to {@code "hls"} in JSON.
     *
     * @return the HLS playlist URL
     */
    @JsonProperty("hls")
    abstract String hls();
}
