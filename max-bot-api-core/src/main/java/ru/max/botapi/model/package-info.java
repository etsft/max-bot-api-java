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
 * Model types for the MAX Bot API — records, sealed interfaces, and enums.
 *
 * <p>This package has zero external dependencies. All types are immutable
 * Java 21 records or sealed interfaces designed for exhaustive pattern matching.</p>
 *
 * <p>Union types (discriminated by a {@code type} or {@code update_type} field)
 * are modeled as sealed interfaces with an {@code Unknown*} fallback subtype
 * for forward compatibility with future API additions.</p>
 */

package ru.max.botapi.model;
