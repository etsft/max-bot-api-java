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
 * Result of a successful upload to the MAX upload endpoint.
 *
 * <p>The MAX platform returns three different response shapes depending on the
 * {@link UploadType} of the upload, and the moment when the attachment token becomes
 * available also differs:</p>
 *
 * <ul>
 *   <li>{@link FileUploadedInfo} ({@code FILE}) — the upload response is JSON containing
 *       the {@code token} and a numeric {@code fileId}. The token is used directly in
 *       {@code FileAttachmentRequest}.</li>
 *   <li>{@link ImageUploadedInfo} ({@code IMAGE}) — the upload response is JSON containing
 *       a {@code photos} map keyed by an opaque server-generated identifier. The map values
 *       carry {@code token} fields that must be passed back via
 *       {@code PhotoAttachmentRequestPayload.photos}.</li>
 *   <li>{@link MediaUploadedInfo} ({@code VIDEO} or {@code AUDIO}) — the upload response is
 *       a tiny XML body ({@code <retval>1</retval>}) that carries no token. The actual
 *       attachment token must be taken from the {@link UploadEndpoint#token()} returned
 *       by {@code POST /uploads}, before the upload begins. {@code MediaUploadedInfo}
 *       carries that earlier token forward and exposes the parsed retval status.</li>
 * </ul>
 *
 * <p>This is a {@code sealed interface} so that {@code switch} pattern matching is
 * exhaustive; if the API ever introduces a new upload-type response shape the compile
 * error will surface immediately.</p>
 *
 * @see UploadType
 * @see UploadEndpoint
 */
public sealed interface UploadedInfo
        permits FileUploadedInfo, ImageUploadedInfo, MediaUploadedInfo {
}
