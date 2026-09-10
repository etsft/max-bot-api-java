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
 * Permissions that can be granted to chat and channel administrators.
 *
 * <p>Three permissions were renamed in the MAX API. The old names are still returned in
 * responses, so they are listed here as well, but they must not be used when granting
 * permissions via {@code POST /chats/{chatId}/members/admins}. Use {@link #canonical()} to
 * fold an old name into the name that replaced it before comparing permissions.
 */
public enum ChatPermission {

    /** Permission to read all messages in the chat or channel. */
    READ_ALL_MESSAGES,

    /** Permission to add or remove members. */
    ADD_REMOVE_MEMBERS,

    /** Permission to add new administrators. */
    ADD_ADMINS,

    /** Permission to change chat info (title, icon, etc.). */
    CHANGE_CHAT_INFO,

    /** Permission to pin messages. */
    PIN_MESSAGE,

    /**
     * Permission to edit and delete messages in group chats, and to write posts and comments
     * in channels. Replaces {@link #POST_EDIT_DELETE_MESSAGE}.
     */
    WRITE,

    /** Permission to make calls in a group chat. Not available in channels. */
    CAN_CALL,

    /** Permission to edit the group chat invite link. Not available in channels. */
    EDIT_LINK,

    /**
     * Permission to delete posts and comments in channels. Not available in group chats.
     * Replaces {@link #DELETE_MESSAGE}.
     */
    DELETE,

    /**
     * Permission to edit posts and comments in channels. Not available in group chats.
     * Replaces {@link #EDIT_MESSAGE}.
     */
    EDIT,

    /**
     * Permission to view channel statistics. Granted to channel owners by default and
     * available to no one else, so it is only ever seen in responses.
     */
    VIEW_STATS,

    /**
     * Former name of {@link #WRITE}.
     *
     * @deprecated still returned in responses; use {@link #WRITE} when granting permissions
     */
    @Deprecated
    POST_EDIT_DELETE_MESSAGE,

    /**
     * Former name of {@link #EDIT}.
     *
     * @deprecated still returned in responses; use {@link #EDIT} when granting permissions
     */
    @Deprecated
    EDIT_MESSAGE,

    /**
     * Former name of {@link #DELETE}.
     *
     * @deprecated still returned in responses; use {@link #DELETE} when granting permissions
     */
    @Deprecated
    DELETE_MESSAGE;

    /**
     * Returns the permission this one is known by today: an old name folds into the name that
     * replaced it, every other permission returns itself.
     *
     * <p>Responses may carry either name for the same right, so compare permissions through
     * this method rather than directly.
     *
     * @return the canonical permission, never {@code null}
     */
    public ChatPermission canonical() {
        return switch (this) {
            case POST_EDIT_DELETE_MESSAGE -> WRITE;
            case EDIT_MESSAGE -> EDIT;
            case DELETE_MESSAGE -> DELETE;
            default -> this;
        };
    }
}
