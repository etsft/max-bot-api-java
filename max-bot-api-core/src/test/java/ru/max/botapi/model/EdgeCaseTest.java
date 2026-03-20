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
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Edge case and boundary condition tests for model records.
 */
class EdgeCaseTest {

    private static final User USER = new User(1L, "Alice", "@alice", false, 100L);
    private static final MessageRecipient RECIPIENT = new MessageRecipient(1L, ChatType.CHAT);
    private static final MessageBody BODY = new MessageBody("mid1", 1L, "Hello", null, null);

    // ===== User toString =====

    @Test
    void user_toString_containsAllFields() {
        var user = new User(42L, "TestUser", "@testuser", true, 999L);
        String str = user.toString();
        assertThat(str).contains("42");
        assertThat(str).contains("TestUser");
        assertThat(str).contains("@testuser");
        assertThat(str).contains("true");
        assertThat(str).contains("999");
    }

    // ===== Chat with null/full optional fields =====

    @Test
    void chat_withAllNullOptionalFields() {
        var chat = new Chat(1L, ChatType.DIALOG, ChatStatus.ACTIVE, null,
                null, 0L, 2, null, null, false, null, null, null, null, null, null);
        assertThat(chat.chatId()).isEqualTo(1L);
        assertThat(chat.title()).isNull();
        assertThat(chat.icon()).isNull();
        assertThat(chat.ownerId()).isNull();
        assertThat(chat.participants()).isNull();
        assertThat(chat.link()).isNull();
        assertThat(chat.description()).isNull();
        assertThat(chat.dialogWithUser()).isNull();
        assertThat(chat.messagesCount()).isNull();
        assertThat(chat.chatMessageId()).isNull();
        assertThat(chat.pinnedMessage()).isNull();
    }

    @Test
    void chat_withAllFieldsPopulated() {
        var icon = new Image("http://icon.png");
        var dialogUser = new UserWithPhoto(2L, "Bob", "@bob", false, 200L,
                "desc", "http://avatar", "http://full");
        var pinnedMsg = new Message(USER, RECIPIENT, 500L, null, BODY, null, null, null);
        var chat = new Chat(10L, ChatType.CHANNEL, ChatStatus.ACTIVE, "Full Chat",
                icon, 9999L, 100, 1L, java.util.Map.of("user1", 1L), true,
                "https://max.ru/chat", "A full chat", dialogUser, 42, "cmid_001", pinnedMsg);
        assertThat(chat.title()).isEqualTo("Full Chat");
        assertThat(chat.icon()).isEqualTo(icon);
        assertThat(chat.ownerId()).isEqualTo(1L);
        assertThat(chat.participants()).containsEntry("user1", 1L);
        assertThat(chat.isPublic()).isTrue();
        assertThat(chat.link()).isEqualTo("https://max.ru/chat");
        assertThat(chat.description()).isEqualTo("A full chat");
        assertThat(chat.dialogWithUser()).isEqualTo(dialogUser);
        assertThat(chat.messagesCount()).isEqualTo(42);
        assertThat(chat.chatMessageId()).isEqualTo("cmid_001");
        assertThat(chat.pinnedMessage()).isEqualTo(pinnedMsg);
    }

    // ===== MessageBody with empty vs null attachments =====

    @Test
    void messageBody_withEmptyAttachmentsList() {
        var body = new MessageBody("m1", 1L, "text", List.of(), null);
        assertThat(body.attachments()).isNotNull().isEmpty();
    }

    @Test
    void messageBody_withNullAttachmentsList() {
        var body = new MessageBody("m1", 1L, "text", null, null);
        assertThat(body.attachments()).isNull();
    }

    @Test
    void messageBody_withMaxLengthText() {
        String maxText = "A".repeat(4000);
        var body = new MessageBody("m1", 1L, maxText, null, null);
        assertThat(body.text()).hasSize(4000);
    }

    // ===== NewMessageBody edge cases =====

    @Test
    void newMessageBody_withEmptyText() {
        var body = new NewMessageBody("", null, null, null, null);
        assertThat(body.text()).isEmpty();
    }

    @Test
    void newMessageBody_allNullFields() {
        var body = new NewMessageBody(null, null, null, null, null);
        assertThat(body.text()).isNull();
        assertThat(body.attachments()).isNull();
        assertThat(body.link()).isNull();
        assertThat(body.notifyRecipients()).isNull();
        assertThat(body.format()).isNull();
    }

    // ===== ChatList edge cases =====

    @Test
    void chatList_withEmptyChatsAndNullMarker() {
        var list = new ChatList(List.of(), null);
        assertThat(list.chats()).isEmpty();
        assertThat(list.marker()).isNull();
    }

    @Test
    void chatList_withSingleChatAndMarker() {
        var chat = new Chat(1L, ChatType.CHAT, ChatStatus.ACTIVE, "G",
                null, 0L, 1, null, null, false, null, null, null, null, null, null);
        var list = new ChatList(List.of(chat), 100L);
        assertThat(list.chats()).hasSize(1);
        assertThat(list.marker()).isEqualTo(100L);
    }

    // ===== MessageList edge cases =====

    @Test
    void messageList_withEmptyMessages() {
        var list = new MessageList(List.of(), null);
        assertThat(list.messages()).isEmpty();
        assertThat(list.marker()).isNull();
    }

    // ===== UpdateList edge cases =====

    @Test
    void updateList_withEmptyUpdatesAndMarker() {
        var list = new UpdateList(List.of(), 999L);
        assertThat(list.updates()).isEmpty();
        assertThat(list.marker()).isEqualTo(999L);
    }

    @Test
    void updateList_singleUpdateNoMarker() {
        var upd = new BotStoppedUpdate(1700001000000L, 60001L, USER);
        var list = new UpdateList(List.of(upd), null);
        assertThat(list.updates()).hasSize(1);
        assertThat(list.marker()).isNull();
    }

    // ===== ChatMember =====

    @Test
    void chatMember_construction_allFields() {
        var member = new ChatMember(99L, "Charlie", "@charlie", false, 300L,
                "A member", "http://avatar.jpg", "http://full.jpg",
                400L, false, false, 50L, null);
        assertThat(member.userId()).isEqualTo(99L);
        assertThat(member.name()).isEqualTo("Charlie");
        assertThat(member.username()).isEqualTo("@charlie");
        assertThat(member.isBot()).isFalse();
        assertThat(member.description()).isEqualTo("A member");
        assertThat(member.avatarUrl()).isEqualTo("http://avatar.jpg");
        assertThat(member.fullAvatarUrl()).isEqualTo("http://full.jpg");
        assertThat(member.lastAccessTime()).isEqualTo(400L);
        assertThat(member.isOwner()).isFalse();
        assertThat(member.isAdmin()).isFalse();
        assertThat(member.joinTime()).isEqualTo(50L);
        assertThat(member.permissions()).isNull();
    }

    @Test
    void chatMember_withPermissions() {
        var perms = List.of(ChatPermission.WRITE, ChatPermission.PIN_MESSAGE, ChatPermission.READ_ALL_MESSAGES);
        var member = new ChatMember(1L, "Admin", null, false, 100L,
                null, null, null, 200L, false, true, 50L, perms);
        assertThat(member.permissions()).hasSize(3);
        assertThat(member.permissions()).contains(ChatPermission.WRITE, ChatPermission.PIN_MESSAGE);
    }

    @Test
    void chatMembersList_withEmptyMembers() {
        var list = new ChatMembersList(List.of(), null);
        assertThat(list.members()).isEmpty();
        assertThat(list.marker()).isNull();
    }

    // ===== SimpleQueryResult =====

    @Test
    void simpleQueryResult_successWithNullMessage() {
        var result = new SimpleQueryResult(true, null);
        assertThat(result.success()).isTrue();
        assertThat(result.message()).isNull();
    }

    @Test
    void simpleQueryResult_failureWithMessage() {
        var result = new SimpleQueryResult(false, "Something went wrong");
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Something went wrong");
    }

    // ===== CallbackAnswer =====

    @Test
    void callbackAnswer_withNotification() {
        var msgBody = new NewMessageBody("Response text", null, null, null, null);
        var answer = new CallbackAnswer(msgBody, "Notification!");
        assertThat(answer.notification()).isEqualTo("Notification!");
        assertThat(answer.message()).isNotNull();
        assertThat(answer.message().text()).isEqualTo("Response text");
    }

    @Test
    void callbackAnswer_minimal() {
        var answer = new CallbackAnswer(null, null);
        assertThat(answer.message()).isNull();
        assertThat(answer.notification()).isNull();
    }

    // ===== LinkedMessage =====

    @Test
    void linkedMessage_forwardType() {
        var linked = new LinkedMessage(MessageLinkType.FORWARD, USER, 10L, BODY);
        assertThat(linked.type()).isEqualTo(MessageLinkType.FORWARD);
        assertThat(linked.sender()).isEqualTo(USER);
        assertThat(linked.chatId()).isEqualTo(10L);
    }

    @Test
    void linkedMessage_replyType() {
        var linked = new LinkedMessage(MessageLinkType.REPLY, null, null, BODY);
        assertThat(linked.type()).isEqualTo(MessageLinkType.REPLY);
        assertThat(linked.sender()).isNull();
        assertThat(linked.chatId()).isNull();
    }

    // ===== ConstructedMessage =====

    @Test
    void constructedMessage_construction() {
        var linked = new LinkedMessage(MessageLinkType.REPLY, USER, 5L, BODY);
        var cm = new ConstructedMessage(USER, 12345L, linked, BODY);
        assertThat(cm.sender()).isEqualTo(USER);
        assertThat(cm.timestamp()).isEqualTo(12345L);
        assertThat(cm.link()).isEqualTo(linked);
        assertThat(cm.body()).isEqualTo(BODY);
    }

    // ===== SubscriptionRequestBody =====

    @Test
    void subscriptionRequestBody_withSecret() {
        var types = List.of("message_created", "message_edited");
        var body = new SubscriptionRequestBody("https://example.com/wh", types, "my_secret");
        assertThat(body.url()).isEqualTo("https://example.com/wh");
        assertThat(body.updateTypes()).containsExactly("message_created", "message_edited");
        assertThat(body.secret()).isEqualTo("my_secret");
    }

    // ===== ChatPatch =====

    @Test
    void chatPatch_allFieldsNull() {
        var patch = new ChatPatch(null, null, null, null);
        assertThat(patch.title()).isNull();
        assertThat(patch.icon()).isNull();
        assertThat(patch.pin()).isNull();
        assertThat(patch.notifyRecipients()).isNull();
    }

    @Test
    void chatPatch_withTitle() {
        var patch = new ChatPatch("New Title", null, null, null);
        assertThat(patch.title()).isEqualTo("New Title");
    }

    // ===== PinMessageBody =====

    @Test
    void pinMessageBody_withNotifyFalse() {
        var pin = new PinMessageBody("msg_42", false);
        assertThat(pin.messageId()).isEqualTo("msg_42");
        assertThat(pin.notifyRecipients()).isFalse();
    }

    @Test
    void pinMessageBody_nullMessageId_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new PinMessageBody(null, null));
    }

    // ===== Defensive copies =====

    @Test
    void chatList_defensiveCopy_inputListModification() {
        var chat = new Chat(1L, ChatType.CHAT, ChatStatus.ACTIVE, "G",
                null, 0L, 1, null, null, false, null, null, null, null, null, null);
        var mutableList = new ArrayList<>(List.of(chat));
        var list = new ChatList(mutableList, null);
        mutableList.add(chat); // modify original
        assertThat(list.chats()).hasSize(1); // record's list is unaffected
    }

    @Test
    void chatList_immutableList_cannotModify() {
        var chat = new Chat(1L, ChatType.CHAT, ChatStatus.ACTIVE, "G",
                null, 0L, 1, null, null, false, null, null, null, null, null, null);
        var list = new ChatList(List.of(chat), null);
        assertThatThrownBy(() -> list.chats().add(chat))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void messageList_defensiveCopy_inputListModification() {
        var msg = new Message(null, RECIPIENT, 0L, null, BODY, null, null, null);
        var mutableList = new ArrayList<>(List.of(msg));
        var msgList = new MessageList(mutableList, null);
        mutableList.add(msg); // modify original
        assertThat(msgList.messages()).hasSize(1); // record's list is unaffected
    }

    @Test
    void updateList_defensiveCopy_inputListModification() {
        var upd = new BotStoppedUpdate(1700001000000L, 60001L, USER);
        var mutableList = new ArrayList<Update>(List.of(upd));
        var updList = new UpdateList(mutableList, null);
        mutableList.add(upd); // modify original
        assertThat(updList.updates()).hasSize(1); // record's list is unaffected
    }

    @Test
    void chatAdminsList_defensiveCopy_inputListModification() {
        var mutableList = new ArrayList<>(List.of(1L, 2L));
        var admins = new ChatAdminsList(mutableList);
        mutableList.add(3L); // modify original
        assertThat(admins.userIds()).hasSize(2); // record's list is unaffected
    }

    @Test
    void userIdsList_defensiveCopy_inputListModification() {
        var mutableList = new ArrayList<>(List.of(10L, 20L));
        var ids = new UserIdsList(mutableList);
        mutableList.add(30L); // modify original
        assertThat(ids.userIds()).hasSize(2); // record's list is unaffected
    }

    @Test
    void newMessageBody_defensiveCopy_attachments() {
        var att = new LocationAttachmentRequest(55.0, 37.0);
        var mutableList = new ArrayList<AttachmentRequest>(List.of(att));
        var body = new NewMessageBody("text", mutableList, null, null, null);
        mutableList.add(att); // modify original
        assertThat(body.attachments()).hasSize(1); // record's list is unaffected
    }

    @Test
    void botInfo_defensiveCopy_commands() {
        var cmd = new BotCommand("start", "Start");
        var mutableList = new ArrayList<>(List.of(cmd));
        var bot = new BotInfo(1L, "Bot", null, true, 0L,
                null, null, null, mutableList);
        mutableList.add(cmd); // modify original
        assertThat(bot.commands()).hasSize(1); // record's list is unaffected
    }

    @Test
    void subscription_defensiveCopy_updateTypes() {
        var mutableList = new ArrayList<>(List.of("message_created"));
        var sub = new Subscription("https://example.com", mutableList);
        mutableList.add("bot_added"); // modify original
        assertThat(sub.updateTypes()).hasSize(1); // record's list is unaffected
    }
}
