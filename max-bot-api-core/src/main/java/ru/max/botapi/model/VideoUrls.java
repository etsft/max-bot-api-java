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

package ru.max.botapi.model;

/**
 * Playback and download URLs of a video, returned inside {@link VideoAttachmentDetails}.
 *
 * <p>MAX transcodes an uploaded video into several renditions and returns only the ones that
 * already exist: a video polled right after upload typically carries a single low resolution,
 * and higher ones appear as transcoding proceeds. Every component is therefore optional.</p>
 *
 * @param mp41080 MP4 URL, 1080p
 * @param mp4720  MP4 URL, 720p
 * @param mp4480  MP4 URL, 480p
 * @param mp4360  MP4 URL, 360p
 * @param mp4240  MP4 URL, 240p
 * @param mp4144  MP4 URL, 144p
 * @param hls     HLS playlist URL
 */
public record VideoUrls(
        @Nullable String mp41080,
        @Nullable String mp4720,
        @Nullable String mp4480,
        @Nullable String mp4360,
        @Nullable String mp4240,
        @Nullable String mp4144,
        @Nullable String hls
) {
}
