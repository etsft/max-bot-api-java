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
 * Jackson mix-in that maps the {@code fileId} Java record component to the JSON key
 * {@code "fileId"} (camelCase) used by the MAX file upload endpoint, overriding the
 * mapper's default {@code SNAKE_CASE} naming strategy.
 *
 * <p>Unlike the rest of the MAX API (which is consistently {@code snake_case}), the file
 * upload response uses {@code "fileId"} verbatim. Without this mix-in the field would be
 * looked up as {@code "file_id"} and silently come back as {@code 0L}.</p>
 */
abstract class FileUploadedInfoMixIn {

    /**
     * Maps to {@code "fileId"} in JSON.
     *
     * @return server-side file identifier
     */
    @JsonProperty("fileId")
    abstract long fileId();
}
