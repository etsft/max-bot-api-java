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
 * A button that opens a mini-application (web view) when pressed.
 *
 * @param text    display text
 * @param url     URL of the mini-application
 * @param payload optional payload passed to the mini-application
 */
public record OpenAppButton(
        String text,
        String url,
        @Nullable String payload
) implements Button {

    /** {@inheritDoc} */
    @Override
    public String type() {
        return "open_app";
    }

    /**
     * Creates an OpenAppButton.
     *
     * @param text must not be {@code null}
     * @param url  must not be {@code null}
     */
    public OpenAppButton {
        Objects.requireNonNull(text, "text must not be null");
        Objects.requireNonNull(url, "url must not be null");
    }
}
