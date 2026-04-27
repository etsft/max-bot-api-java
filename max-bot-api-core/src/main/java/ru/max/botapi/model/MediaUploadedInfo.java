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
 * Result of a successful upload of {@link UploadType#VIDEO} or {@link UploadType#AUDIO}.
 *
 * <p>For video and audio uploads the MAX platform returns a tiny XML body
 * ({@code <retval>1</retval>}) and does NOT include a token in the upload response.
 * The attachment token is instead obtained earlier, from the
 * {@link UploadEndpoint#token() upload endpoint response}, and must be carried forward
 * by the caller. {@code MediaUploadedInfo} encapsulates both pieces of state:</p>
 *
 * <ul>
 *   <li>{@link #token()} — the attachment token taken from the {@code UploadEndpoint},
 *       to be passed to {@code MediaRequestPayload} when sending the message.</li>
 *   <li>{@link #retval()} — the parsed integer body of the upload response. The MAX
 *       platform returns {@code 1} for a successful upload.</li>
 * </ul>
 *
 * <p>After the upload completes it may take a short while before the media is fully
 * processed and ready to be referenced in a message. Sending the message earlier may
 * yield {@code attachment.not.ready}; clients can retry the message send (not the upload)
 * with backoff.</p>
 *
 * @param token  attachment token from {@code UploadEndpoint}, used in the outgoing message
 * @param retval status integer parsed from the XML upload response ({@code 1} = success)
 */
public record MediaUploadedInfo(String token, int retval) implements UploadedInfo {

    /**
     * Creates a MediaUploadedInfo.
     *
     * @param token must not be {@code null}
     */
    public MediaUploadedInfo {
        Objects.requireNonNull(token, "token must not be null");
    }
}
