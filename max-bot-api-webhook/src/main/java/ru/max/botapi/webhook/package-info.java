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
 * Embedded webhook server for receiving MAX Bot API updates over HTTP/HTTPS.
 *
 * <p>Provides {@link ru.max.botapi.webhook.MaxWebhookServer}, a lightweight server backed by
 * {@code com.sun.net.httpserver.HttpServer} that dispatches incoming update payloads to a
 * user-supplied {@link ru.max.botapi.core.UpdateHandler}.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Virtual-thread executor for concurrent request handling</li>
 *   <li>Optional {@code X-Max-Bot-Api-Secret} header validation</li>
 *   <li>Plain HTTP and HTTPS (with {@link javax.net.ssl.SSLContext}) modes</li>
 *   <li>Helpers for registering and unregistering the webhook via the MAX Bot API</li>
 *   <li>Configurable port (default 8443) and URL path (default {@code /webhook})</li>
 * </ul>
 */
package ru.max.botapi.webhook;
