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

package ru.max.botapi.jackson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ru.max.botapi.model.Attachment;
import ru.max.botapi.model.Message;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.UnknownAttachment;
import ru.max.botapi.model.UnknownUpdate;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that a payload the model cannot represent degrades to the forward-compatibility
 * fallback types instead of failing the enclosing document.
 */
class LenientDeserializationTest {

    private static final String VALID_MESSAGE = """
            {
              "sender": {"user_id": 99001, "name": "John Doe", "is_bot": false},
              "recipient": {"chat_id": 50001, "chat_type": "chat"},
              "timestamp": 1700001000000,
              "body": {"mid": "msg_010", "seq": 10, "text": "New message"}
            }""";

    private static final String VALID_UPDATE = """
            {"update_type": "message_created", "timestamp": 1700001000000, "message": %s}"""
            .formatted(VALID_MESSAGE);

    /** A {@code message_created} without the required {@code message} field. */
    private static final String MALFORMED_UPDATE = """
            {"update_type": "message_created", "timestamp": 1700002000000, "user_locale": "ru"}""";

    private JacksonMaxSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new JacksonMaxSerializer();
    }

    @Nested
    class MalformedUpdate {

        @Test
        void knownTypeMissingRequiredFieldBecomesUnknownUpdate() {
            Update update = serializer.deserialize(MALFORMED_UPDATE, Update.class);

            assertThat(update).isInstanceOf(UnknownUpdate.class);
            UnknownUpdate unknown = (UnknownUpdate) update;
            assertThat(unknown.updateType()).isEqualTo("message_created");
            assertThat(unknown.timestamp()).isEqualTo(1700002000000L);
            assertThat(unknown.rawJson()).contains("user_locale");
        }

        @Test
        void nullMessageBecomesUnknownUpdate() {
            Update update = serializer.deserialize("""
                    {"update_type": "message_created", "timestamp": 1700002000000, "message": null}""",
                    Update.class);

            assertThat(update).isInstanceOf(UnknownUpdate.class);
            assertThat(update.updateType()).isEqualTo("message_created");
        }

        @Test
        void validUpdateIsStillDeserializedNormally() {
            Update update = serializer.deserialize(VALID_UPDATE, Update.class);

            assertThat(update).isInstanceOf(MessageCreatedUpdate.class);
            assertThat(((MessageCreatedUpdate) update).message().body().text()).isEqualTo("New message");
        }
    }

    @Nested
    class MalformedBatch {

        @Test
        void malformedUpdateDoesNotKillTheBatchAndMarkerSurvives() {
            String json = """
                    {"updates": [%s, %s, %s], "marker": 987654}"""
                    .formatted(VALID_UPDATE, MALFORMED_UPDATE, VALID_UPDATE);

            UpdateList list = serializer.deserialize(json, UpdateList.class);

            assertThat(list.marker()).isEqualTo(987654L);
            assertThat(list.updates()).hasSize(3);
            assertThat(list.updates().get(0)).isInstanceOf(MessageCreatedUpdate.class);
            assertThat(list.updates().get(1)).isInstanceOf(UnknownUpdate.class);
            assertThat(list.updates().get(2)).isInstanceOf(MessageCreatedUpdate.class);
        }

        @Test
        void nonObjectElementIsSkippedAndMarkerSurvives() {
            String json = """
                    {"updates": [%s, "not-an-object", 42], "marker": 111}""".formatted(VALID_UPDATE);

            UpdateList list = serializer.deserialize(json, UpdateList.class);

            assertThat(list.marker()).isEqualTo(111L);
            assertThat(list.updates()).hasSize(1);
            assertThat(list.updates().get(0)).isInstanceOf(MessageCreatedUpdate.class);
        }

        @Test
        void updatesOfUnexpectedShapeYieldEmptyBatchWithMarker() {
            UpdateList list = serializer.deserialize(
                    "{\"updates\": {\"oops\": true}, \"marker\": 222}", UpdateList.class);

            assertThat(list.marker()).isEqualTo(222L);
            assertThat(list.updates()).isEmpty();
        }

        @Test
        void absentUpdatesAndMarkerYieldEmptyBatch() {
            UpdateList list = serializer.deserialize("{}", UpdateList.class);

            assertThat(list.marker()).isNull();
            assertThat(list.updates()).isEmpty();
        }

        @Test
        void nullMarkerIsReadAsNull() {
            String json = """
                    {"updates": [%s], "marker": null}""".formatted(VALID_UPDATE);

            UpdateList list = serializer.deserialize(json, UpdateList.class);

            assertThat(list.marker()).isNull();
            assertThat(list.updates()).hasSize(1);
        }

        @Test
        void wellFormedBatchIsUnaffected() {
            String json = """
                    {"updates": [%s, %s], "marker": 555}""".formatted(VALID_UPDATE, VALID_UPDATE);

            UpdateList list = serializer.deserialize(json, UpdateList.class);

            assertThat(list.marker()).isEqualTo(555L);
            assertThat(list.updates()).allMatch(MessageCreatedUpdate.class::isInstance);
        }
    }

    @Nested
    class MalformedAttachment {

        @Test
        void audioWithoutPayloadBecomesUnknownAttachment() {
            Attachment attachment = serializer.deserialize("{\"type\": \"audio\"}", Attachment.class);

            assertThat(attachment).isInstanceOf(UnknownAttachment.class);
            assertThat(attachment.type()).isEqualTo("audio");
        }

        @Test
        void messageWithMalformedAttachmentIsStillDeserialized() {
            String json = """
                    {
                      "recipient": {"chat_id": 50001, "chat_type": "chat"},
                      "timestamp": 1700001000000,
                      "body": {
                        "mid": "msg_011", "seq": 11, "text": "Voice",
                        "attachments": [{"type": "audio"}]
                      }
                    }""";

            Message message = serializer.deserialize(json, Message.class);

            assertThat(message.body().attachments()).hasSize(1);
            assertThat(message.body().attachments().get(0)).isInstanceOf(UnknownAttachment.class);
        }
    }
}
