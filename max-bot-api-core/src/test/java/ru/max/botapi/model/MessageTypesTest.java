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
 * Tests for Message-related record types.
 */
class MessageTypesTest {

    private static final MessageRecipient RECIPIENT =
            new MessageRecipient(1L, ChatType.CHAT);
    private static final MessageBody BODY =
            new MessageBody("mid1", 1L, "Hello", null, null);

    @Test
    void messageRecipient_construction() {
        assertThat(RECIPIENT.chatId()).isEqualTo(1L);
        assertThat(RECIPIENT.chatType()).isEqualTo(ChatType.CHAT);
    }

    @Test
    void messageBody_construction() {
        assertThat(BODY.mid()).isEqualTo("mid1");
        assertThat(BODY.seq()).isEqualTo(1L);
        assertThat(BODY.text()).isEqualTo("Hello");
        assertThat(BODY.attachments()).isNull();
        assertThat(BODY.markup()).isNull();
    }

    @Test
    void markupElement_construction() {
        var elem = new MarkupElement("bold", 0, 5);
        assertThat(elem.type()).isEqualTo("bold");
        assertThat(elem.from()).isZero();
        assertThat(elem.length()).isEqualTo(5);
    }

    @Test
    void messageStat_construction() {
        var stat = new MessageStat(42);
        assertThat(stat.views()).isEqualTo(42);
    }

    @Test
    void message_construction() {
        var sender = new User(1L, "Alice", null, null, null, false, 100L);
        var msg = new Message(sender, RECIPIENT, 1000L, null, BODY, null, null, null);
        assertThat(msg.sender()).isEqualTo(sender);
        assertThat(msg.recipient()).isEqualTo(RECIPIENT);
        assertThat(msg.timestamp()).isEqualTo(1000L);
        assertThat(msg.link()).isNull();
        assertThat(msg.stat()).isNull();
        assertThat(msg.url()).isNull();
        assertThat(msg.constructor()).isNull();
    }

    @Test
    void message_nullSender_allowed() {
        var msg = new Message(null, RECIPIENT, 0L, null, BODY, null, null, null);
        assertThat(msg.sender()).isNull();
    }

    @Test
    void message_nullRecipient_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Message(null, null, 0L, null, BODY, null, null, null));
    }

    @Test
    void linkedMessage_construction() {
        var linked = new LinkedMessage(MessageLinkType.REPLY, null, 1L, BODY);
        assertThat(linked.type()).isEqualTo(MessageLinkType.REPLY);
        assertThat(linked.sender()).isNull();
        assertThat(linked.chatId()).isEqualTo(1L);
    }

    @Test
    void newMessageBody_construction() {
        var body = new NewMessageBody("text", null, null, null, TextFormat.MARKDOWN);
        assertThat(body.text()).isEqualTo("text");
        assertThat(body.format()).isEqualTo(TextFormat.MARKDOWN);
    }

    @Test
    void newMessageLink_construction() {
        var link = new NewMessageLink(MessageLinkType.FORWARD, "mid99");
        assertThat(link.type()).isEqualTo(MessageLinkType.FORWARD);
        assertThat(link.mid()).isEqualTo("mid99");
    }

    @Test
    void messageList_construction() {
        var msg = new Message(null, RECIPIENT, 0L, null, BODY, null, null, null);
        var list = new MessageList(List.of(msg), 10L);
        assertThat(list.messages()).hasSize(1);
        assertThat(list.marker()).isEqualTo(10L);
    }

    @Test
    void sendMessageResult_construction() {
        var msg = new Message(null, RECIPIENT, 0L, null, BODY, null, null, null);
        var result = new SendMessageResult(msg);
        assertThat(result.message()).isEqualTo(msg);
    }

    @Test
    void message_equality() {
        var msg1 = new Message(null, RECIPIENT, 100L, null, BODY, null, null, null);
        var msg2 = new Message(null, RECIPIENT, 100L, null, BODY, null, null, null);
        assertThat(msg1).isEqualTo(msg2);
        assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
    }
}
