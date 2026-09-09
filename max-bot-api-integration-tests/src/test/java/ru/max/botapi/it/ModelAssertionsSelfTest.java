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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.model.Attachment;
import ru.max.botapi.model.Message;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the harness itself: that {@link ModelAssertions} really rejects a payload the model
 * cannot represent, instead of quietly passing on the {@code Unknown*} fallback.
 *
 * <p>Needs no token and no network, so it runs on every invocation of the suite — including a
 * dry run — and keeps the strictness honest.</p>
 */
@Order(0)
@DisplayName("Live suite self-check")
class ModelAssertionsSelfTest {

    private final MaxSerializer serializer = new RecordingSerializer();

    @Test
    @DisplayName("an update the model cannot build is reported with its raw JSON")
    void rejectsUnmappedUpdate() {
        String json = """
                {"update_type": "message_created", "timestamp": 1700002000000, "user_locale": "ru"}""";
        Update update = serializer.deserialize(json, Update.class);

        assertThatThrownBy(() -> ModelAssertions.assertFullyMapped(update))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("message_created")
                .hasMessageContaining("user_locale");
    }

    @Test
    @DisplayName("an unrecognised update type is reported")
    void rejectsUnknownUpdateType() {
        String json = """
                {"update_type": "invented_later", "timestamp": 1700002000000}""";
        Update update = serializer.deserialize(json, Update.class);

        assertThatThrownBy(() -> ModelAssertions.assertFullyMapped(update))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("invented_later");
    }

    @Test
    @DisplayName("an attachment the model cannot build is reported")
    void rejectsUnmappedAttachment() {
        Attachment attachment = serializer.deserialize("{\"type\": \"audio\"}", Attachment.class);

        assertThatThrownBy(() -> ModelAssertions.assertFullyMapped(attachment))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("audio");
    }

    @Test
    @DisplayName("an attachment nested in a message is reached")
    void walksIntoMessageAttachments() {
        String json = """
                {
                  "recipient": {"chat_id": 1, "chat_type": "chat"},
                  "timestamp": 1700001000000,
                  "body": {"mid": "m1", "seq": 1, "attachments": [{"type": "audio"}]}
                }""";
        Message message = serializer.deserialize(json, Message.class);

        assertThatThrownBy(() -> ModelAssertions.assertFullyMapped(message))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("audio");
    }

    @Test
    @DisplayName("a well-formed batch passes")
    void acceptsMappedBatch() {
        String json = """
                {"updates": [{
                   "update_type": "message_created",
                   "timestamp": 1700001000000,
                   "message": {
                     "recipient": {"chat_id": 1, "chat_type": "chat"},
                     "timestamp": 1700001000000,
                     "body": {"mid": "m1", "seq": 1, "text": "hi"}
                   }
                 }], "marker": 42}""";
        UpdateList updates = serializer.deserialize(json, UpdateList.class);

        ModelAssertions.assertFullyMapped(updates);
        assertThat(updates.marker()).isEqualTo(42L);
    }
}
