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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import ru.max.botapi.longpolling.MaxLongPollingConsumer;
import ru.max.botapi.model.ButtonIntent;
import ru.max.botapi.model.CallbackAnswer;
import ru.max.botapi.model.CallbackButton;
import ru.max.botapi.model.InlineKeyboardAttachment;
import ru.max.botapi.model.InlineKeyboardAttachmentRequest;
import ru.max.botapi.model.MessageCallbackUpdate;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.SendMessageResult;
import ru.max.botapi.model.SimpleQueryResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The callback round trip, which cannot be automated: {@code answerOnCallback} needs a
 * {@code callback_id}, and that only exists once a human has pressed an inline button.
 *
 * <p>Without {@code MAX_IT_INTERACTIVE} the whole class is reported as skipped.</p>
 */
@Order(6)
@DisplayName("Live: inline keyboard callback")
class CallbackLiveTest extends LiveTestBase {

    private static final int POLL_TIMEOUT_SECONDS = 5;

    private static final int PRESS_WAIT_SECONDS = 120;

    private static final String PAYLOAD = "live-suite-callback";

    private String messageId;

    /**
     * The callback arrives over long polling, so nothing may be diverting updates.
     */
    @BeforeAll
    void clearSubscriptions() {
        clearOwnWebhookSubscriptions();
    }

    @Test
    @Order(1)
    @DisplayName("a pressed inline button produces a callback that can be answered")
    void callbackRoundTrip() {
        InlineKeyboardAttachment.KeyboardPayload keyboard =
                new InlineKeyboardAttachment.KeyboardPayload(List.of(List.of(
                        new CallbackButton("Press me", PAYLOAD, ButtonIntent.POSITIVE))));

        NewMessageBody body = new NewMessageBody(
                "Live suite: press the button below",
                List.of(new InlineKeyboardAttachmentRequest(keyboard)),
                null, false, null);

        SendMessageResult sent = api().sendMessage(body)
                .chatId(IntegrationConfig.chatId())
                .execute();
        ModelAssertions.assertFullyMapped(sent.message());
        messageId = sent.message().body().mid();

        Prompts.step(Prompts.groupChat(),
                "Press the \"Press me\" button on the message the bot just posted there. "
                + "Nothing to type: the button is on the message itself.");

        MessageCallbackUpdate callback = awaitCallback();

        ModelAssertions.assertFullyMapped(callback);
        assertThat(callback.callback().callbackId()).isNotBlank();
        assertThat(callback.callback().payload()).isEqualTo(PAYLOAD);

        SimpleQueryResult answer = api()
                .answerOnCallback(new CallbackAnswer(null, "Received by the live suite"),
                        callback.callback().callbackId())
                .execute();

        assertThat(answer.success()).isTrue();
    }

    private MessageCallbackUpdate awaitCallback() {
        AtomicReference<MessageCallbackUpdate> holder = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
                .api(api())
                .pollTimeout(POLL_TIMEOUT_SECONDS)
                .handler(update -> {
                    if (update instanceof MessageCallbackUpdate cb
                            && PAYLOAD.equals(cb.callback().payload())) {
                        holder.set(cb);
                        latch.countDown();
                    }
                })
                .onError(e -> System.out.println("Long polling error: " + e))
                .build();

        consumer.start();
        try {
            if (!latch.await(PRESS_WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new AssertionError("No message_callback arrived within "
                        + PRESS_WAIT_SECONDS + "s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the callback", e);
        } finally {
            consumer.stop();
        }
        return holder.get();
    }

    /**
     * Removes the keyboard message.
     */
    @AfterAll
    void removeKeyboardMessage() {
        if (messageId != null) {
            cleanUp("delete keyboard message",
                    () -> api().deleteMessage(messageId).execute());
        }
    }
}
