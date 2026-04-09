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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that all records with List/Map fields make defensive copies
 * in their compact constructors, ensuring true immutability.
 */
class DefensiveCopyTest {

    private static final User ALICE = new User(1L, "Alice", null, null, null, false, 100L);

    // ========== Non-nullable List fields ==========

    @Test
    void chatList_defensiveCopy() {
        var chat = new Chat(1L, ChatType.CHAT, ChatStatus.ACTIVE, "G",
                null, 0L, 1, null, null, false, null, null, null, null, null, null);
        var mutable = new ArrayList<>(List.of(chat));
        var chatList = new ChatList(mutable, null);
        mutable.clear();
        assertThat(chatList.chats()).hasSize(1);
        assertThatThrownBy(() -> chatList.chats().add(chat))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void chatMembersList_defensiveCopy() {
        var member = new ChatMember(1L, "A", null, null, null, false, 0L,
                null, null, null, 0L, false, false, 0L, null);
        var mutable = new ArrayList<>(List.of(member));
        var list = new ChatMembersList(mutable, null);
        mutable.clear();
        assertThat(list.members()).hasSize(1);
        assertThatThrownBy(() -> list.members().add(member))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void chatAdminsList_defensiveCopy() {
        var mutable = new ArrayList<>(List.of(1L, 2L));
        var admins = new ChatAdminsList(mutable);
        mutable.clear();
        assertThat(admins.userIds()).containsExactly(1L, 2L);
        assertThatThrownBy(() -> admins.userIds().add(3L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getSubscriptionsResult_defensiveCopy() {
        var sub = new Subscription("http://example.com", null);
        var mutable = new ArrayList<>(List.of(sub));
        var result = new GetSubscriptionsResult(mutable);
        mutable.clear();
        assertThat(result.subscriptions()).hasSize(1);
        assertThatThrownBy(() -> result.subscriptions().add(sub))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void messageList_defensiveCopy() {
        var body = new MessageBody("m1", 1L, "hi", null, null);
        var recipient = new MessageRecipient(1L, ChatType.CHAT);
        var msg = new Message(null, recipient, 0L, null, body, null, null, null);
        var mutable = new ArrayList<>(List.of(msg));
        var list = new MessageList(mutable, null);
        mutable.clear();
        assertThat(list.messages()).hasSize(1);
        assertThatThrownBy(() -> list.messages().add(msg))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void updateList_defensiveCopy() {
        var update = new BotStartedUpdate(0L, 1L, ALICE, null, null);
        var mutable = new ArrayList<Update>(List.of(update));
        var list = new UpdateList(mutable, null);
        mutable.clear();
        assertThat(list.updates()).hasSize(1);
        assertThatThrownBy(() -> list.updates().add(update))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void userIdsList_defensiveCopy() {
        var mutable = new ArrayList<>(List.of(10L, 20L));
        var ids = new UserIdsList(mutable);
        mutable.clear();
        assertThat(ids.userIds()).containsExactly(10L, 20L);
        assertThatThrownBy(() -> ids.userIds().add(30L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void keyboardPayload_defensiveCopy() {
        var btn = new CallbackButton("Click", "cb1", null);
        List<Button> row = new ArrayList<>(List.of(btn));
        List<List<Button>> mutable = new ArrayList<>(List.of(row));
        var payload = new InlineKeyboardAttachment.KeyboardPayload(mutable);
        mutable.clear();
        assertThat(payload.buttons()).hasSize(1);
        // Verify inner lists are also copied
        assertThatThrownBy(() -> payload.buttons().get(0).add(btn))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ========== Nullable List fields ==========

    @Test
    void botInfo_commands_defensiveCopy() {
        var cmd = new BotCommand("help", "Show help");
        var mutable = new ArrayList<>(List.of(cmd));
        var bot = new BotInfo(1L, "Bot", null, null, true, 0L,
                null, null, null, mutable);
        mutable.clear();
        assertThat(bot.commands()).hasSize(1);
        assertThatThrownBy(() -> bot.commands().add(cmd))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void botInfo_commands_null_preserved() {
        var bot = new BotInfo(1L, "Bot", null, null, true, 0L,
                null, null, null, null);
        assertThat(bot.commands()).isNull();
    }

    @Test
    void botPatch_commands_defensiveCopy() {
        var cmd = new BotCommand("start", "Start");
        var mutable = new ArrayList<>(List.of(cmd));
        var patch = new BotPatch(null, null, mutable, null);
        mutable.clear();
        assertThat(patch.commands()).hasSize(1);
        assertThatThrownBy(() -> patch.commands().add(cmd))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void botPatch_commands_null_preserved() {
        var patch = new BotPatch(null, null, null, null);
        assertThat(patch.commands()).isNull();
    }

    @Test
    void chatMember_permissions_defensiveCopy() {
        var mutable = new ArrayList<>(List.of(ChatPermission.READ_ALL_MESSAGES, ChatPermission.ADD_REMOVE_MEMBERS));
        var member = new ChatMember(1L, "A", null, null, null, false, 0L,
                null, null, null, 0L, false, false, 0L, mutable);
        mutable.clear();
        assertThat(member.permissions()).hasSize(2);
        assertThatThrownBy(() -> member.permissions().add(ChatPermission.ADD_ADMINS))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void chatMember_permissions_null_preserved() {
        var member = new ChatMember(1L, "A", null, null, null, false, 0L,
                null, null, null, 0L, false, false, 0L, null);
        assertThat(member.permissions()).isNull();
    }

    @Test
    void messageBody_attachments_defensiveCopy() {
        var attachment = new LocationAttachment(55.0, 37.0);
        var mutable = new ArrayList<Attachment>(List.of(attachment));
        var body = new MessageBody("m1", 1L, "text", mutable, null);
        mutable.clear();
        assertThat(body.attachments()).hasSize(1);
        assertThatThrownBy(() -> body.attachments().add(attachment))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void messageBody_markup_defensiveCopy() {
        var elem = new MarkupElement("bold", 0, 4);
        var mutable = new ArrayList<>(List.of(elem));
        var body = new MessageBody("m1", 1L, "text", null, mutable);
        mutable.clear();
        assertThat(body.markup()).hasSize(1);
        assertThatThrownBy(() -> body.markup().add(elem))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void messageBody_nullLists_preserved() {
        var body = new MessageBody("m1", 1L, "text", null, null);
        assertThat(body.attachments()).isNull();
        assertThat(body.markup()).isNull();
    }

    @Test
    void newMessageBody_attachments_defensiveCopy() {
        var req = new LocationAttachmentRequest(55.0, 37.0);
        var mutable = new ArrayList<AttachmentRequest>(List.of(req));
        var body = new NewMessageBody("text", mutable, null, null, null);
        mutable.clear();
        assertThat(body.attachments()).hasSize(1);
        assertThatThrownBy(() -> body.attachments().add(req))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void newMessageBody_attachments_null_preserved() {
        var body = new NewMessageBody("text", null, null, null, null);
        assertThat(body.attachments()).isNull();
    }

    @Test
    void subscription_updateTypes_defensiveCopy() {
        var mutable = new ArrayList<>(List.of("message_created", "message_edited"));
        var sub = new Subscription("http://example.com", mutable);
        mutable.clear();
        assertThat(sub.updateTypes()).hasSize(2);
        assertThatThrownBy(() -> sub.updateTypes().add("extra"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void subscription_updateTypes_null_preserved() {
        var sub = new Subscription("http://example.com", null);
        assertThat(sub.updateTypes()).isNull();
    }

    @Test
    void subscriptionRequestBody_updateTypes_defensiveCopy() {
        var mutable = new ArrayList<>(List.of("message_created"));
        var body = new SubscriptionRequestBody("http://example.com", mutable, null);
        mutable.clear();
        assertThat(body.updateTypes()).hasSize(1);
        assertThatThrownBy(() -> body.updateTypes().add("extra"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void subscriptionRequestBody_updateTypes_null_preserved() {
        var body = new SubscriptionRequestBody("http://example.com", null, null);
        assertThat(body.updateTypes()).isNull();
    }

    // ========== Map fields ==========

    @Test
    void chat_participants_defensiveCopy() {
        var mutable = new HashMap<>(Map.of("admin", 1L, "user", 2L));
        var chat = new Chat(1L, ChatType.CHAT, ChatStatus.ACTIVE, "G",
                null, 0L, 2, null, mutable, false, null, null, null, null, null, null);
        mutable.clear();
        assertThat(chat.participants()).hasSize(2);
        assertThatThrownBy(() -> chat.participants().put("extra", 3L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void chat_participants_null_preserved() {
        var chat = new Chat(1L, ChatType.CHAT, ChatStatus.ACTIVE, null,
                null, 0L, 0, null, null, false, null, null, null, null, null, null);
        assertThat(chat.participants()).isNull();
    }

    @Test
    void photoAttachmentRequestPayload_photos_defensiveCopy() {
        var ref = new PhotoAttachmentRequestPayload.TokenRef("tok1");
        var mutable = new HashMap<>(Map.of("small", ref));
        var payload = new PhotoAttachmentRequestPayload(null, null, mutable);
        mutable.clear();
        assertThat(payload.photos()).hasSize(1);
        assertThatThrownBy(() -> payload.photos().put("large", ref))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void photoAttachmentRequestPayload_photos_null_preserved() {
        var payload = new PhotoAttachmentRequestPayload("tok", null, null);
        assertThat(payload.photos()).isNull();
    }
}
