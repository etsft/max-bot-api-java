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
 * Result of {@code POST /messages/{messageId}/comments}.
 *
 * @param message the created comment
 */
public record SendCommentResult(CommentMessage message) {

    /**
     * Creates a SendCommentResult.
     *
     * @param message must not be {@code null}
     */
    public SendCommentResult {
        Objects.requireNonNull(message, "message must not be null");
    }
}
