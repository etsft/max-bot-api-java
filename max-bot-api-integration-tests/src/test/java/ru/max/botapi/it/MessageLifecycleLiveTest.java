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

package ru.max.botapi.it;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import ru.max.botapi.model.GetPinnedMessageResult;
import ru.max.botapi.model.Message;
import ru.max.botapi.model.MessageLinkType;
import ru.max.botapi.model.MessageList;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.NewMessageLink;
import ru.max.botapi.model.PinMessageBody;
import ru.max.botapi.model.SendMessageResult;
import ru.max.botapi.model.SimpleQueryResult;
import ru.max.botapi.model.TextFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The full lifecycle of a message: create, read, edit, pin, unpin, delete.
 *
 * <p>These steps are one scenario rather than independent cases — every later step needs the
 * message the first one created — so they share state and run in declaration order.</p>
 */
@Order(3)
@DisplayName("Live: message lifecycle")
class MessageLifecycleLiveTest extends LiveTestBase {

    private String messageId;

    private String replyMessageId;

    @Test
    @Order(1)
    @DisplayName("sendMessage creates a message in the test chat")
    void sendMessage() {
        NewMessageBody body = new NewMessageBody(
                "Live integration test " + System.currentTimeMillis(), null, null, false, null);

        SendMessageResult result = api().sendMessage(body)
                .chatId(IntegrationConfig.chatId())
                .execute();

        Message message = result.message();
        ModelAssertions.assertFullyMapped(message);
        assertThat(message.body().mid()).isNotBlank();
        assertThat(message.recipient().chatId()).isEqualTo(IntegrationConfig.chatId());

        messageId = message.body().mid();
    }

    @Test
    @Order(2)
    @DisplayName("getMessageById returns the message just sent")
    void getMessageById() {
        Message message = api().getMessageById(messageId).execute();

        ModelAssertions.assertFullyMapped(message);
        assertThat(message.body().mid()).isEqualTo(messageId);
    }

    @Test
    @Order(3)
    @DisplayName("getMessages lists chat history")
    void getMessages() {
        MessageList messages = api().getMessages()
                .chatId(IntegrationConfig.chatId())
                .count(10)
                .execute();

        messages.messages().forEach(ModelAssertions::assertFullyMapped);
        assertThat(messages.messages())
                .extracting(m -> m.body().mid())
                .contains(messageId);
    }

    @Test
    @Order(4)
    @DisplayName("sendMessage with a reply link and Markdown formatting")
    void sendReply() {
        NewMessageBody body = new NewMessageBody(
                "*reply* from the live suite",
                null,
                new NewMessageLink(MessageLinkType.REPLY, messageId),
                false,
                TextFormat.MARKDOWN);

        SendMessageResult result = api().sendMessage(body)
                .chatId(IntegrationConfig.chatId())
                .execute();

        Message message = result.message();
        ModelAssertions.assertFullyMapped(message);
        assertThat(message.link()).isNotNull();
        assertThat(message.link().type()).isEqualTo(MessageLinkType.REPLY);

        replyMessageId = message.body().mid();
    }

    @Test
    @Order(5)
    @DisplayName("editMessage rewrites the text")
    void editMessage() {
        String edited = "Live integration test (edited) " + System.currentTimeMillis();

        SimpleQueryResult result = api()
                .editMessage(new NewMessageBody(edited, null, null, false, null), messageId)
                .execute();

        assertThat(result.success()).isTrue();
        assertThat(api().getMessageById(messageId).execute().body().text()).isEqualTo(edited);
    }

    @Test
    @Order(6)
    @DisplayName("pinMessage, getPinnedMessage, unpinMessage")
    void pinning() {
        SimpleQueryResult pinned = api()
                .pinMessage(new PinMessageBody(messageId, false), IntegrationConfig.chatId())
                .execute();
        assertThat(pinned.success()).isTrue();

        GetPinnedMessageResult current =
                api().getPinnedMessage(IntegrationConfig.chatId()).execute();
        assertThat(current.message()).isNotNull();
        ModelAssertions.assertFullyMapped(current.message());
        assertThat(current.message().body().mid()).isEqualTo(messageId);

        SimpleQueryResult unpinned =
                api().unpinMessage(IntegrationConfig.chatId()).execute();
        assertThat(unpinned.success()).isTrue();
    }

    @Test
    @Order(7)
    @DisplayName("deleteMessage removes the message")
    void deleteMessage() {
        SimpleQueryResult result = api().deleteMessage(messageId).execute();

        assertThat(result.success()).isTrue();
        messageId = null;
    }

    /**
     * Removes anything the scenario left behind when it failed part-way.
     */
    @AfterAll
    void removeLeftovers() {
        for (String leftover : List.of(nullSafe(messageId), nullSafe(replyMessageId))) {
            if (!leftover.isEmpty()) {
                cleanUp("delete message " + leftover,
                        () -> api().deleteMessage(leftover).execute());
            }
        }
        cleanUp("unpin", () -> api().unpinMessage(IntegrationConfig.chatId()).execute());
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
