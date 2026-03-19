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
 * A button that triggers a callback to the bot when pressed.
 *
 * @param text    display text
 * @param payload callback data sent to the bot
 * @param intent  optional visual style intent
 */
public record CallbackButton(
        String text,
        String payload,
        @Nullable ButtonIntent intent
) implements Button {

    /** {@inheritDoc} */
    @Override
    public String type() {
        return "callback";
    }

    /**
     * Creates a CallbackButton.
     *
     * @param text    must not be {@code null}
     * @param payload must not be {@code null}
     */
    public CallbackButton {
        Objects.requireNonNull(text, "text must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
    }
}
