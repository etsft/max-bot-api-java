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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import ru.max.botapi.model.ActionRequestBody;
import ru.max.botapi.model.Chat;
import ru.max.botapi.model.ChatMember;
import ru.max.botapi.model.ChatMembersList;
import ru.max.botapi.model.ChatPatch;
import ru.max.botapi.model.SenderAction;
import ru.max.botapi.model.SimpleQueryResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Read and update operations on the configured test chat.
 *
 * <p>Nothing here removes anyone or leaves the chat; those live in {@link DestructiveLiveTest}.
 * The one mutation is the chat title, which is restored in teardown.</p>
 */
@Order(2)
@DisplayName("Live: chat")
@SuppressWarnings("deprecation") // exercises the endpoints MAX no longer documents
class ChatLiveTest extends LiveTestBase {

    private String originalTitle;

    @Test
    @Order(1)
    @DisplayName("getChat returns the configured chat")
    void getChat() {
        Chat chat = api().getChat(IntegrationConfig.chatId()).execute();

        assertThat(chat.chatId()).isEqualTo(IntegrationConfig.chatId());
        assertThat(chat.type()).isNotNull();
        assertThat(chat.status()).isNotNull();

        originalTitle = chat.title();
        if (chat.pinnedMessage() != null) {
            ModelAssertions.assertFullyMapped(chat.pinnedMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("getMembership reports the bot itself")
    void getMembership() {
        ChatMember me = api().getMembership(IntegrationConfig.chatId()).execute();

        assertThat(me.userId()).isPositive();
        assertThat(me.isBot()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("getMembers paginates")
    void getMembers() {
        ChatMembersList members = api().getMembers(IntegrationConfig.chatId())
                .count(2)
                .execute();

        assertThat(members.members()).isNotNull();
        assertThat(members.members()).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    @Order(4)
    @DisplayName("getAdmins lists chat administrators")
    void getAdmins() {
        // Requires the bot to be an administrator; a 403 here is a configuration problem.
        ChatMembersList admins = api().getAdmins(IntegrationConfig.chatId()).execute();

        assertThat(admins.members()).isNotNull();
    }

    @Test
    @Order(5)
    @DisplayName("sendAction accepts every SenderAction")
    void sendAction() {
        for (SenderAction action : SenderAction.values()) {
            SimpleQueryResult result = api()
                    .sendAction(new ActionRequestBody(action), IntegrationConfig.chatId())
                    .execute();

            assertThat(result.success())
                    .withFailMessage("sendAction(%s) was rejected: %s", action, result.message())
                    .isTrue();
        }
    }

    @Test
    @Order(6)
    @DisplayName("editChat changes the title")
    void editChat() {
        String title = "Live test " + System.currentTimeMillis();

        Chat updated = api()
                .editChat(new ChatPatch(title, null, null, null, false), IntegrationConfig.chatId())
                .execute();

        assertThat(updated.title()).isEqualTo(title);
    }

    @Test
    @Order(7)
    @DisplayName("getChatByLink resolves a public link")
    void getChatByLink() {
        String link = IntegrationConfig.chatLink();
        // GET /chats/{chatLink} resolves public links and usernames only. An invite link
        // (max.ru/join/...) addresses a private chat and has no public form, so there is
        // nothing here to exercise — a private test chat simply cannot supply one.
        Assumptions.assumeFalse(link.contains("/join/"),
                () -> IntegrationConfig.CHAT_LINK + " is an invite link (" + link + "); "
                        + "set it to a public link such as @my_chat to run this test");

        Chat chat = api().getChatByLink(link).execute();

        assertThat(chat.chatId()).isNotZero();
    }

    /**
     * Puts the original chat title back.
     */
    @AfterAll
    void restoreTitle() {
        if (originalTitle == null) {
            return;
        }
        cleanUp("restore chat title", () -> api()
                .editChat(new ChatPatch(originalTitle, null, null, null, false), IntegrationConfig.chatId())
                .execute());
    }
}
