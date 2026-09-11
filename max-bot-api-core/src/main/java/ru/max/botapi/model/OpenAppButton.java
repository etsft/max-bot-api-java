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
 * A button that launches a bot's mini-application when pressed.
 *
 * <p>The bot whose mini-application opens is named either by {@code webApp} or by
 * {@code contactId}; {@link #ofWebApp(String, String)} and {@link #ofContactId(String, long)}
 * build the two forms.</p>
 *
 * @param text      display text
 * @param webApp    public name (username) of the bot, or a link to it
 * @param contactId ID of the bot
 * @param payload   optional start parameter passed to the mini-application in its
 *                  {@code initData}
 */
public record OpenAppButton(
        String text,
        @Nullable String webApp,
        @Nullable Long contactId,
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
     */
    public OpenAppButton {
        Objects.requireNonNull(text, "text must not be null");
    }

    /**
     * Creates a button that opens the mini-application of the bot with the given public name.
     *
     * @param text   display text; must not be {@code null}
     * @param webApp public name (username) of the bot, or a link to it; must not be {@code null}
     * @return the button
     */
    public static OpenAppButton ofWebApp(String text, String webApp) {
        Objects.requireNonNull(webApp, "webApp must not be null");
        return new OpenAppButton(text, webApp, null, null);
    }

    /**
     * Creates a button that opens the mini-application of the bot with the given ID.
     *
     * @param text      display text; must not be {@code null}
     * @param contactId ID of the bot
     * @return the button
     */
    public static OpenAppButton ofContactId(String text, long contactId) {
        return new OpenAppButton(text, null, contactId, null);
    }
}
