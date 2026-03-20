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

/**
 * Query builder classes for all 31 MAX Bot API endpoints.
 *
 * <p>Each class extends {@link ru.max.botapi.client.MaxQuery} and exposes fluent
 * setter methods for optional request parameters. Use these classes through
 * {@link ru.max.botapi.client.MaxBotAPI} factory methods.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MaxBotAPI api = MaxBotAPI.create("my-token");
 * SendMessageResult result = api.sendMessage(new NewMessageBody("Hello!"))
 *     .chatId(123456789L)
 *     .execute();
 * }</pre>
 */
package ru.max.botapi.client.queries;
