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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for all enum types: values exist and are complete.
 */
class EnumTest {

    @Test
    void chatType_hasAllValues() {
        assertThat(ChatType.values())
                .containsExactly(ChatType.DIALOG, ChatType.CHAT, ChatType.CHANNEL);
    }

    @Test
    void chatStatus_hasAllValues() {
        assertThat(ChatStatus.values())
                .containsExactly(ChatStatus.ACTIVE, ChatStatus.REMOVED,
                        ChatStatus.LEFT, ChatStatus.CLOSED, ChatStatus.SUSPENDED);
    }

    @Test
    void chatPermission_hasAllValues() {
        assertThat(ChatPermission.values()).hasSize(11);
        assertThat(ChatPermission.valueOf("READ_ALL_MESSAGES")).isNotNull();
        assertThat(ChatPermission.valueOf("ADD_REMOVE_MEMBERS")).isNotNull();
        assertThat(ChatPermission.valueOf("ADD_ADMINS")).isNotNull();
        assertThat(ChatPermission.valueOf("CHANGE_CHAT_INFO")).isNotNull();
        assertThat(ChatPermission.valueOf("PIN_MESSAGE")).isNotNull();
        assertThat(ChatPermission.valueOf("WRITE")).isNotNull();
        assertThat(ChatPermission.valueOf("CAN_CALL")).isNotNull();
        assertThat(ChatPermission.valueOf("EDIT_LINK")).isNotNull();
        assertThat(ChatPermission.valueOf("DELETE")).isNotNull();
        assertThat(ChatPermission.valueOf("EDIT")).isNotNull();
        assertThat(ChatPermission.valueOf("VIEW_STATS")).isNotNull();
    }

    @Test
    void senderAction_hasAllValues() {
        assertThat(SenderAction.values()).hasSize(6);
        assertThat(SenderAction.valueOf("TYPING_ON")).isNotNull();
        assertThat(SenderAction.valueOf("SENDING_PHOTO")).isNotNull();
        assertThat(SenderAction.valueOf("SENDING_VIDEO")).isNotNull();
        assertThat(SenderAction.valueOf("SENDING_AUDIO")).isNotNull();
        assertThat(SenderAction.valueOf("SENDING_FILE")).isNotNull();
        assertThat(SenderAction.valueOf("MARK_SEEN")).isNotNull();
    }

    @Test
    void messageLinkType_hasAllValues() {
        assertThat(MessageLinkType.values())
                .containsExactly(MessageLinkType.FORWARD, MessageLinkType.REPLY);
    }

    @Test
    void textFormat_hasAllValues() {
        assertThat(TextFormat.values())
                .containsExactly(TextFormat.MARKDOWN, TextFormat.HTML);
    }

    @Test
    void uploadType_hasAllValues() {
        assertThat(UploadType.values())
                .containsExactly(UploadType.IMAGE, UploadType.VIDEO,
                        UploadType.AUDIO, UploadType.FILE);
    }

    @Test
    void buttonIntent_hasAllValues() {
        assertThat(ButtonIntent.values())
                .containsExactly(ButtonIntent.DEFAULT, ButtonIntent.POSITIVE,
                        ButtonIntent.NEGATIVE);
    }
}
