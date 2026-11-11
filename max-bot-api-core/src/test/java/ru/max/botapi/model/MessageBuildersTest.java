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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageBuildersTest {

    @Test
    void textBodyHasOnlyText() {
        assertThat(NewMessageBody.text("Hi")).isEqualTo(new NewMessageBody("Hi", null, null, null, null));
        assertThat(NewMessageBody.text("<b>Hi</b>", TextFormat.HTML))
                .isEqualTo(new NewMessageBody("<b>Hi</b>", null, null, null, TextFormat.HTML));
    }

    @Test
    void withersReplaceOneFieldEach() {
        var keyboard = InlineKeyboardAttachmentRequest.builder()
                .row(CallbackButton.of("Yes", "yes"))
                .build();
        var link = new NewMessageLink(MessageLinkType.REPLY, "mid.1");

        NewMessageBody body = NewMessageBody.text("Hi")
                .withAttachments(keyboard)
                .withLink(link)
                .withNotify(false)
                .withFormat(TextFormat.MARKDOWN);

        assertThat(body).isEqualTo(new NewMessageBody("Hi", List.of(keyboard), link, false, TextFormat.MARKDOWN));
        assertThat(NewMessageBody.attachments(keyboard).text()).isNull();
    }

    @Test
    void nullTextAndNullAttachmentAreRejected() {
        assertThatThrownBy(() -> NewMessageBody.text(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> NewMessageBody.text("x").withAttachments((AttachmentRequest) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void keyboardBuilderCollectsRowsInOrder() {
        var keyboard = InlineKeyboardAttachmentRequest.builder()
                .row(CallbackButton.of("Yes", "yes"), CallbackButton.of("No", "no"))
                .row(new LinkButton("Docs", "https://dev.max.ru"))
                .build();

        assertThat(keyboard.payload().buttons()).containsExactly(
                List.of(new CallbackButton("Yes", "yes", null), new CallbackButton("No", "no", null)),
                List.of(new LinkButton("Docs", "https://dev.max.ru")));
    }

    @Test
    void keyboardBuilderRejectsEmptyRowsAndKeyboards() {
        assertThatThrownBy(() -> InlineKeyboardAttachmentRequest.builder().row())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InlineKeyboardAttachmentRequest.builder().build())
                .isInstanceOf(IllegalStateException.class);
    }
}
