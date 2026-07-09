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

/**
 * Tests for the Update sealed hierarchy — construction and exhaustive switch.
 */
class UpdateTest {

    private static final User USER = new User(1L, "Alice", null, null, null, false, 100L);
    private static final MessageRecipient RECIPIENT =
            new MessageRecipient(1L, ChatType.CHAT);
    private static final MessageBody BODY = new MessageBody("m1", 1L, "Hi", null, null);
    private static final Message MSG = new Message(USER, RECIPIENT, 100L, null, BODY,
            null, null, null);

    @Test
    void messageCreatedUpdate() {
        var upd = new MessageCreatedUpdate(1000L, MSG, "en");
        assertThat(upd.updateType()).isEqualTo("message_created");
        assertThat(upd.timestamp()).isEqualTo(1000L);
        assertThat(upd.message()).isEqualTo(MSG);
        assertThat(upd.userLocale()).isEqualTo("en");
    }

    @Test
    void messageCallbackUpdate() {
        var cb = new Callback(100L, "cb1", "data", USER);
        var upd = new MessageCallbackUpdate(200L, cb, MSG, "ru");
        assertThat(upd.updateType()).isEqualTo("message_callback");
        assertThat(upd.callback().callbackId()).isEqualTo("cb1");
    }

    @Test
    void messageEditedUpdate() {
        var upd = new MessageEditedUpdate(300L, MSG);
        assertThat(upd.updateType()).isEqualTo("message_edited");
    }

    @Test
    void messageRemovedUpdate() {
        var upd = new MessageRemovedUpdate(400L, "mid1", 1L, 2L);
        assertThat(upd.updateType()).isEqualTo("message_removed");
        assertThat(upd.messageId()).isEqualTo("mid1");
    }

    @Test
    void botAddedUpdate() {
        var upd = new BotAddedUpdate(500L, 1L, USER, false);
        assertThat(upd.updateType()).isEqualTo("bot_added");
        assertThat(upd.isChannel()).isFalse();
    }

    @Test
    void botRemovedUpdate() {
        var upd = new BotRemovedUpdate(600L, 1L, USER, true);
        assertThat(upd.updateType()).isEqualTo("bot_removed");
        assertThat(upd.isChannel()).isTrue();
    }

    @Test
    void userAddedUpdate() {
        var upd = new UserAddedUpdate(700L, 1L, USER, 99L, false);
        assertThat(upd.updateType()).isEqualTo("user_added");
        assertThat(upd.inviterId()).isEqualTo(99L);
    }

    @Test
    void userRemovedUpdate() {
        var upd = new UserRemovedUpdate(800L, 1L, USER, null, false);
        assertThat(upd.updateType()).isEqualTo("user_removed");
        assertThat(upd.adminId()).isNull();
    }

    @Test
    void botStartedUpdate() {
        var upd = new BotStartedUpdate(900L, 1L, USER, "deep_link", "en");
        assertThat(upd.updateType()).isEqualTo("bot_started");
        assertThat(upd.payload()).isEqualTo("deep_link");
    }

    @Test
    void botStoppedUpdate() {
        var upd = new BotStoppedUpdate(1000L, 1L, USER);
        assertThat(upd.updateType()).isEqualTo("bot_stopped");
    }

    @Test
    void chatTitleChangedUpdate() {
        var upd = new ChatTitleChangedUpdate(1100L, 1L, USER, "New Title");
        assertThat(upd.updateType()).isEqualTo("chat_title_changed");
        assertThat(upd.title()).isEqualTo("New Title");
    }

    @Test
    void dialogClearedUpdate() {
        var upd = new DialogClearedUpdate(1200L, 1L, USER, "en");
        assertThat(upd.updateType()).isEqualTo("dialog_cleared");
        assertThat(upd.chatId()).isEqualTo(1L);
    }

    @Test
    void dialogMutedUpdate() {
        var upd = new DialogMutedUpdate(1300L, 1L, USER, 9999L, "en");
        assertThat(upd.updateType()).isEqualTo("dialog_muted");
        assertThat(upd.mutedUntil()).isEqualTo(9999L);
    }

    @Test
    void dialogUnmutedUpdate() {
        var upd = new DialogUnmutedUpdate(1400L, 1L, USER, "en");
        assertThat(upd.updateType()).isEqualTo("dialog_unmuted");
        assertThat(upd.chatId()).isEqualTo(1L);
    }

    @Test
    void dialogRemovedUpdate() {
        var upd = new DialogRemovedUpdate(1450L, 1L, USER, "en");
        assertThat(upd.updateType()).isEqualTo("dialog_removed");
        assertThat(upd.chatId()).isEqualTo(1L);
    }

    @Test
    void unknownUpdate() {
        var upd = new UnknownUpdate("future_event", 1500L, "{}");
        assertThat(upd.updateType()).isEqualTo("future_event");
    }

    @Test
    void exhaustiveSwitch_coversAll16Types() {
        var cb = new Callback(0L, "cb", null, USER);

        Update[] all = {
                new MessageCreatedUpdate(0, MSG, null),
                new MessageCallbackUpdate(0, cb, null, null),
                new MessageEditedUpdate(0, MSG),
                new MessageRemovedUpdate(0, "m", 1, 1),
                new BotAddedUpdate(0, 1, USER, false),
                new BotRemovedUpdate(0, 1, USER, false),
                new UserAddedUpdate(0, 1, USER, null, false),
                new UserRemovedUpdate(0, 1, USER, null, false),
                new BotStartedUpdate(0, 1, USER, null, null),
                new BotStoppedUpdate(0, 1, USER),
                new ChatTitleChangedUpdate(0, 1, USER, "t"),
                new DialogClearedUpdate(0, 1, USER, null),
                new DialogMutedUpdate(0, 1, USER, 0, null),
                new DialogUnmutedUpdate(0, 1, USER, null),
                new DialogRemovedUpdate(0, 1, USER, null),
                new UnknownUpdate("x", 0, "{}")
        };

        assertThat(all).hasSize(16);

        for (Update upd : all) {
            String desc = switch (upd) {
                case MessageCreatedUpdate u -> u.updateType();
                case MessageCallbackUpdate u -> u.updateType();
                case MessageEditedUpdate u -> u.updateType();
                case MessageRemovedUpdate u -> u.updateType();
                case BotAddedUpdate u -> u.updateType();
                case BotRemovedUpdate u -> u.updateType();
                case UserAddedUpdate u -> u.updateType();
                case UserRemovedUpdate u -> u.updateType();
                case BotStartedUpdate u -> u.updateType();
                case BotStoppedUpdate u -> u.updateType();
                case ChatTitleChangedUpdate u -> u.updateType();
                case DialogClearedUpdate u -> u.updateType();
                case DialogMutedUpdate u -> u.updateType();
                case DialogUnmutedUpdate u -> u.updateType();
                case DialogRemovedUpdate u -> u.updateType();
                case UnknownUpdate u -> u.updateType();
            };
            assertThat(desc).isNotBlank();
        }
    }
}
