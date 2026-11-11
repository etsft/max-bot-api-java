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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelListsTest {

    @Test
    void responseListsDropNullElements() {
        MessageList list = new MessageList(Arrays.asList(null, message("a"), null), null);
        assertThat(list.messages()).extracting(m -> m.body().mid()).containsExactly("a");

        ChatMember member = new ChatMember(1L, "n", null, null, null, false, null, null, null, null,
                0L, false, false, 0L, Arrays.asList(ChatPermission.WRITE, null), null);
        assertThat(member.permissions()).containsExactly(ChatPermission.WRITE);

        MessageBody body = new MessageBody("m", 1L, null, Arrays.asList(null, new UnknownAttachment("x", "{}")),
                Arrays.asList((MarkupElement) null));
        assertThat(body.attachments()).hasSize(1);
        assertThat(body.markup()).isEmpty();
    }

    @Test
    void keyboardDropsNullRowsAndButtons() {
        var payload = new InlineKeyboardAttachment.KeyboardPayload(Arrays.asList(
                null, Arrays.asList(new CallbackButton("t", "p", null), null)));
        assertThat(payload.buttons()).hasSize(1);
        assertThat(payload.buttons().getFirst()).hasSize(1);
    }

    @Test
    void requiredListStillRejectsNull() {
        assertThatThrownBy(() -> new MessageList(null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("messages must not be null");
    }

    @Test
    void copiesAreImmutable() {
        List<Long> ids = ModelLists.withoutNulls(Arrays.asList(1L, null), "ids");
        assertThatThrownBy(() -> ids.add(2L)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(ModelLists.withoutNullsOrNull(null)).isNull();
    }

    private static Message message(String mid) {
        return new Message(null, new MessageRecipient(1L, ChatType.CHAT, null, null), 1L, null,
                new MessageBody(mid, 1L, "t", null, null), null, null, null);
    }
}
