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
 * Type of file to upload to MAX platform.
 *
 * <p>The {@link #value()} method returns the exact string the MAX API expects in the
 * {@code type} query parameter of {@code POST /uploads}. Always use {@code value()} rather
 * than {@code name().toLowerCase()} so that the mapping is explicit and survives future
 * enum renames.</p>
 */
public enum UploadType {

    /** Image upload — API value {@code "image"}. */
    IMAGE("image"),

    /** Video upload — API value {@code "video"}. */
    VIDEO("video"),

    /** Audio upload — API value {@code "audio"}. */
    AUDIO("audio"),

    /** Generic file upload — API value {@code "file"}. */
    FILE("file");

    private final String apiValue;

    UploadType(String apiValue) {
        this.apiValue = apiValue;
    }

    /**
     * Returns the string value expected by the MAX API for this upload type.
     *
     * @return the API-level type string, e.g. {@code "image"}
     */
    public String value() {
        return apiValue;
    }
}
