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
 * Long-polling update consumer for the MAX Bot API.
 *
 * <p>Provides {@link ru.max.botapi.longpolling.MaxLongPollingConsumer}, which polls the
 * {@code GET /updates} endpoint in a dedicated virtual thread and dispatches each received
 * {@link ru.max.botapi.model.Update} to a user-supplied
 * {@link ru.max.botapi.core.UpdateHandler}.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Virtual-thread-based polling loop — lightweight and non-blocking</li>
 *   <li>Exponential backoff on errors (1 s up to 30 s)</li>
 *   <li>Optional update-type filtering</li>
 *   <li>Configurable long-poll timeout</li>
 *   <li>Graceful shutdown via {@link ru.max.botapi.longpolling.MaxLongPollingConsumer#stop()}</li>
 * </ul>
 */
package ru.max.botapi.longpolling;
