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

/**
 * Permissions that can be granted to chat members.
 */
public enum ChatPermission {

    /** Permission to read all messages in the chat. */
    READ_ALL_MESSAGES,

    /** Permission to add or remove members. */
    ADD_REMOVE_MEMBERS,

    /** Permission to add new administrators. */
    ADD_ADMINS,

    /** Permission to change chat info (title, icon, etc.). */
    CHANGE_CHAT_INFO,

    /** Permission to pin messages. */
    PIN_MESSAGE,

    /** Permission to write messages. */
    WRITE
}
