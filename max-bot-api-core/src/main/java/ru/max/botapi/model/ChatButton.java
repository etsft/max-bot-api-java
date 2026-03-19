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
 * A button that creates a new chat when pressed.
 *
 * @param text            display text
 * @param chatTitle       title of the chat to create
 * @param chatDescription optional description of the chat
 * @param startPayload    optional payload sent to the bot when chat is created
 * @param uuid            optional unique identifier for the button
 */
public record ChatButton(
        String text,
        String chatTitle,
        @Nullable String chatDescription,
        @Nullable String startPayload,
        @Nullable String uuid
) implements Button {

    /** {@inheritDoc} */
    @Override
    public String type() {
        return "chat";
    }

    /**
     * Creates a ChatButton.
     *
     * @param text      must not be {@code null}
     * @param chatTitle must not be {@code null}
     */
    public ChatButton {
        Objects.requireNonNull(text, "text must not be null");
        Objects.requireNonNull(chatTitle, "chatTitle must not be null");
    }
}
