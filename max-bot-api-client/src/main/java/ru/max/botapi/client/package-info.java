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
 * HTTP transport layer and client orchestrator for the MAX Bot API.
 *
 * <p>Provides {@link ru.max.botapi.client.MaxClient} as the main entry point,
 * with {@link ru.max.botapi.client.JdkHttpMaxTransportClient} as the default
 * HTTP transport implementation using {@code java.net.http.HttpClient} with
 * virtual threads.</p>
 */
package ru.max.botapi.client;
