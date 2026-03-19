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

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests for Chat-related record types.
 */
class ChatTypesTest {

    @Test
    void chat_construction() {
        var chat = new Chat(1L, ChatType.CHAT, ChatStatus.ACTIVE, "Test Chat",
                null, 1000L, 5, 1L, null, false, null, null, null, null, null, null);
        assertThat(chat.chatId()).isEqualTo(1L);
        assertThat(chat.type()).isEqualTo(ChatType.CHAT);
        assertThat(chat.status()).isEqualTo(ChatStatus.ACTIVE);
        assertThat(chat.title()).isEqualTo("Test Chat");
        assertThat(chat.participantsCount()).isEqualTo(5);
    }

    @Test
    void chat_nullType_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Chat(1L, null, ChatStatus.ACTIVE, null,
                        null, 0L, 0, null, null, false, null, null, null, null, null, null));
    }

    @Test
    void chat_nullStatus_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Chat(1L, ChatType.CHAT, null, null,
                        null, 0L, 0, null, null, false, null, null, null, null, null, null));
    }

    @Test
    void chat_equality() {
        var c1 = new Chat(1L, ChatType.DIALOG, ChatStatus.ACTIVE, null,
                null, 100L, 2, null, null, false, null, null, null, null, null, null);
        var c2 = new Chat(1L, ChatType.DIALOG, ChatStatus.ACTIVE, null,
                null, 100L, 2, null, null, false, null, null, null, null, null, null);
        assertThat(c1).isEqualTo(c2);
        assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
    }

    @Test
    void chatList_construction() {
        var chat = new Chat(1L, ChatType.CHAT, ChatStatus.ACTIVE, "G",
                null, 0L, 1, null, null, false, null, null, null, null, null, null);
        var list = new ChatList(List.of(chat), 42L);
        assertThat(list.chats()).hasSize(1);
        assertThat(list.marker()).isEqualTo(42L);
    }

    @Test
    void chatPatch_allNulls() {
        var patch = new ChatPatch(null, null, null, null);
        assertThat(patch.title()).isNull();
    }

    @Test
    void chatMember_construction() {
        var member = new ChatMember(1L, "Alice", null, false, 100L,
                null, null, null, 200L, true, true, 50L, List.of(ChatPermission.WRITE));
        assertThat(member.isOwner()).isTrue();
        assertThat(member.isAdmin()).isTrue();
        assertThat(member.permissions()).containsExactly(ChatPermission.WRITE);
    }

    @Test
    void chatMembersList_construction() {
        var list = new ChatMembersList(List.of(), null);
        assertThat(list.members()).isEmpty();
        assertThat(list.marker()).isNull();
    }

    @Test
    void chatAdminsList_construction() {
        var admins = new ChatAdminsList(List.of(1L, 2L, 3L));
        assertThat(admins.userIds()).containsExactly(1L, 2L, 3L);
    }

    @Test
    void actionRequestBody_construction() {
        var body = new ActionRequestBody(SenderAction.TYPING_ON);
        assertThat(body.action()).isEqualTo(SenderAction.TYPING_ON);
    }

    @Test
    void pinMessageBody_construction() {
        var pin = new PinMessageBody("mid123", true);
        assertThat(pin.messageId()).isEqualTo("mid123");
        assertThat(pin.notifyRecipients()).isTrue();
    }

    @Test
    void userIdsList_construction() {
        var ids = new UserIdsList(List.of(10L, 20L));
        assertThat(ids.userIds()).containsExactly(10L, 20L);
    }
}
