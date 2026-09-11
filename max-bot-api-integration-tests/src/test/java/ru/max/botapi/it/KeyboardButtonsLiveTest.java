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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import ru.max.botapi.client.MaxApiException;
import ru.max.botapi.model.BotInfo;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.InlineKeyboardAttachment;
import ru.max.botapi.model.InlineKeyboardAttachmentRequest;
import ru.max.botapi.model.Message;
import ru.max.botapi.model.MessageButton;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.OpenAppButton;
import ru.max.botapi.model.SendMessageResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keyboard buttons whose shape comes from the schema alone: {@code message} and
 * {@code open_app}.
 *
 * <p>Each button goes out on its own message, so a refusal names the button that caused it,
 * and is then read back with {@code getMessageById}. That covers both directions: MAX has to
 * accept the button as this library sends it, and the button MAX stores has to come back as
 * the same concrete type rather than {@code UnknownButton}.</p>
 *
 * <p>The {@code open_app} buttons name the test bot itself, so no extra configuration is
 * needed. There is no case for {@code contact_id} on its own: the schema allows it, but MAX
 * refuses such a button with {@code "Field 'webApp' cannot be null"}, and the library no
 * longer builds one.</p>
 */
@Order(11)
@DisplayName("Live: keyboard buttons")
class KeyboardButtonsLiveTest extends LiveTestBase {

    private static final String OPEN_APP_HINT = " The button names the test bot itself; if the "
            + "error says it has no mini-application, that is the bot's configuration, "
            + "not the button's shape.";

    private final List<String> sentMessageIds = new ArrayList<>();

    private BotInfo bot;

    /**
     * Reads the bot the {@code open_app} buttons point at.
     */
    @BeforeAll
    void readBot() {
        bot = api().getMyInfo().execute();
    }

    @Test
    @Order(1)
    @DisplayName("a message button is accepted and read back with its text")
    void messageButton() {
        MessageButton sent = new MessageButton("/live-suite");

        Button readBack = sendAndReadBack("message", sent, "");

        assertThat(readBack).isEqualTo(sent);
    }

    @Test
    @Order(2)
    @DisplayName("an open_app button naming the bot by username is accepted and read back")
    void openAppButtonByWebApp() {
        Button readBack = sendAndReadBack("open_app (web_app)",
                new OpenAppButton("Open app", botUsername(), null, "live-suite"), OPEN_APP_HINT);

        assertOpenApp(readBack);
    }

    @Test
    @Order(3)
    @DisplayName("an open_app button naming the bot by username and id is accepted and read back")
    void openAppButtonByWebAppAndContactId() {
        Button readBack = sendAndReadBack("open_app (web_app + contact_id)",
                new OpenAppButton("Open app", botUsername(), bot.userId(), null), OPEN_APP_HINT);

        assertOpenApp(readBack);
    }

    private String botUsername() {
        String username = bot.username();
        Assumptions.assumeTrue(username != null && !username.isBlank(),
                "the test bot has no username to put in web_app");
        return username;
    }

    private static void assertOpenApp(Button readBack) {
        assertThat(readBack).isInstanceOf(OpenAppButton.class);
        OpenAppButton app = (OpenAppButton) readBack;
        assertThat(app.text()).isEqualTo("Open app");
        assertThat(app.webApp()).isNotBlank();
    }

    /**
     * Sends a one-button keyboard to the test chat and returns the button as MAX stored it.
     */
    private Button sendAndReadBack(String what, Button button, String hintOnRefusal) {
        NewMessageBody body = new NewMessageBody(
                "Live suite: " + what + " button",
                List.of(new InlineKeyboardAttachmentRequest(
                        new InlineKeyboardAttachment.KeyboardPayload(List.of(List.of(button))))),
                null, false, null);

        SendMessageResult sent;
        try {
            sent = api().sendMessage(body)
                    .chatId(IntegrationConfig.chatId())
                    .execute();
        } catch (MaxApiException e) {
            throw new AssertionError("MAX refused the " + what + " button: HTTP "
                    + e.statusCode() + ", " + e.errorCode() + ": " + e.errorMessage() + "."
                    + hintOnRefusal, e);
        }
        ModelAssertions.assertFullyMapped(sent.message());
        String messageId = Objects.requireNonNull(sent.message().body(), "sent message body").mid();
        sentMessageIds.add(messageId);

        Message readBack = api().getMessageById(messageId).execute();
        ModelAssertions.assertFullyMapped(readBack);

        List<Button> buttons = buttonsOf(readBack);
        assertThat(buttons).as("buttons of the message read back").hasSize(1);
        System.out.println(what + " button read back as " + buttons.getFirst());
        return buttons.getFirst();
    }

    private static List<Button> buttonsOf(Message message) {
        assertThat(message.body()).as("body of the message read back").isNotNull();
        return Objects.requireNonNullElse(message.body().attachments(), List.of()).stream()
                .filter(InlineKeyboardAttachment.class::isInstance)
                .map(InlineKeyboardAttachment.class::cast)
                .flatMap(keyboard -> keyboard.payload().buttons().stream())
                .flatMap(List::stream)
                .toList();
    }

    /**
     * Removes the messages this class posted.
     */
    @AfterAll
    void removeButtonMessages() {
        for (String messageId : sentMessageIds) {
            cleanUp("delete button message " + messageId,
                    () -> api().deleteMessage(messageId).execute());
        }
    }
}
