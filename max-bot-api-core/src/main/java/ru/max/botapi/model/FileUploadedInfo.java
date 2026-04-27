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

import java.util.Objects;

/**
 * Result of a successful upload of {@link UploadType#FILE}.
 *
 * <p>The MAX platform returns a JSON response of the form:</p>
 * <pre>{@code
 * { "fileId": 3343344796, "token": "..." }
 * }</pre>
 *
 * <p>Use {@link #token()} as the {@code token} field of {@code FileAttachmentRequest}
 * when sending a message that references this file.</p>
 *
 * @param fileId numeric server-side identifier of the uploaded file
 * @param token  attachment token to reference the file in messages
 */
public record FileUploadedInfo(long fileId, String token) implements UploadedInfo {

    /**
     * Creates a FileUploadedInfo.
     *
     * @param token must not be {@code null}
     */
    public FileUploadedInfo {
        Objects.requireNonNull(token, "token must not be null");
    }
}
