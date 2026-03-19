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
 * Sealed interface representing an incoming attachment from the MAX API.
 *
 * <p>Discriminated by the {@code type} field in JSON. Implementations cover all known
 * attachment types. {@link UnknownAttachment} serves as a forward-compatibility fallback
 * for unrecognized types.</p>
 */
public sealed interface Attachment permits
        PhotoAttachment, VideoAttachment, AudioAttachment,
        FileAttachment, StickerAttachment, ContactAttachment,
        InlineKeyboardAttachment, ShareAttachment, LocationAttachment,
        UnknownAttachment {

    /**
     * Returns the attachment type discriminator string.
     *
     * @return type string (e.g., "image", "video", "file")
     */
    String type();
}
