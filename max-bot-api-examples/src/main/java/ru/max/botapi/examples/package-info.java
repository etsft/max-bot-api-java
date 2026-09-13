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

/**
 * Example bots demonstrating the MAX Bot API client library.
 *
 * <p>Each example is a standalone application with a {@code main} method.
 * Set the {@code MAX_BOT_TOKEN} environment variable before running.</p>
 *
 * <ul>
 *   <li>{@link ru.max.botapi.examples.EchoBot} — echoes back every incoming message</li>
 *   <li>{@link ru.max.botapi.examples.KeyboardBot} — demonstrates inline keyboards and callbacks</li>
 *   <li>{@link ru.max.botapi.examples.FileUploadBot} — uploads a generated text file</li>
 *   <li>{@link ru.max.botapi.examples.ImageUploadBot} — uploads an image ({@code MAX_IMAGE_PATH})</li>
 *   <li>{@link ru.max.botapi.examples.VideoUploadBot} — uploads a video ({@code MAX_VIDEO_PATH})</li>
 *   <li>{@link ru.max.botapi.examples.AudioUploadBot} — uploads an audio file ({@code MAX_AUDIO_PATH})</li>
 *   <li>{@link ru.max.botapi.examples.CommentsBot} — replies to comments on channel posts</li>
 *   <li>{@link ru.max.botapi.examples.WebhookBot} — echo bot on a webhook instead of long polling</li>
 * </ul>
 *
 * <p>Run one with
 * {@code ./gradlew :max-bot-api-examples:run -PmainClass=ru.max.botapi.examples.<Name>};
 * without {@code -PmainClass} the {@code EchoBot} runs.</p>
 */
package ru.max.botapi.examples;
