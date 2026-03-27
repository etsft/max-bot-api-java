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

package ru.max.botapi.core;

import ru.max.botapi.model.Update;

/**
 * Functional interface for handling incoming {@link Update} events.
 *
 * <p>This is the shared handler contract for both webhook and long-polling
 * update delivery. Implement it once and use it with either transport —
 * switching {@code max.bot.mode} in Spring Boot requires no handler changes.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * UpdateHandler handler = update ->
 *     System.out.println("Received: " + update.updateType());
 * }</pre>
 */
@FunctionalInterface
public interface UpdateHandler {

    /**
     * Called for each incoming {@link Update}.
     *
     * @param update the incoming update; never {@code null}
     */
    void onUpdate(Update update);
}
