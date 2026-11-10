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

package ru.max.botapi.gson;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ru.max.botapi.core.TypeReference;
import ru.max.botapi.model.ActionRequestBody;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.CallbackButton;
import ru.max.botapi.model.TextFormat;
import ru.max.botapi.model.UnknownButton;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateList;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the Gson-specific adapter branches that the parity suite does not reach.
 */
class GsonAdaptersTest {

    private GsonMaxSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new GsonMaxSerializer();
    }

    @Nested
    class Enums {

        @Test
        void nullEnumIsReadAndWrittenAsNull() {
            assertThat(serializer.deserialize("{\"action\":null}", Operation.class).action()).isNull();
            assertThat(serializer.serialize(Arrays.asList(TextFormat.HTML, null))).isEqualTo("[\"html\",null]");
        }

        @Test
        void objectForEnumIsRejected() {
            assertThatThrownBy(() -> serializer.deserialize("{\"action\":{}}", ActionRequestBody.class))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void scalarTokensAreMatchedByText() {
            assertThat(serializer.deserialize("true", Flag.class)).isEqualTo(Flag.TRUE);
            assertThat(serializer.deserialize("1", Flag.class)).isNull();
        }

        @Test
        void constantWithBodyUsesItsEnumType() {
            assertThat(serializer.serialize(Flag.WITH_BODY)).isEqualTo("\"with_body\"");
            assertThat(serializer.deserialize("\"with_body\"", Flag.class)).isSameAs(Flag.WITH_BODY);
        }
    }

    @Nested
    class SealedTypes {

        @Test
        void nullElementsAreWrittenAsNull() {
            var buttons = new TypeReference<List<Button>>() { };
            List<Button> list = Arrays.asList(new CallbackButton("Yes", "yes", null), null);

            assertThatJson(serializer.getGson().toJson(list, buttons.getType()))
                    .isEqualTo("[{\"type\":\"callback\",\"text\":\"Yes\",\"payload\":\"yes\"},null]");
            assertThat(serializer.serialize(Arrays.asList((CallbackButton) null))).isEqualTo("[null]");
        }

        @Test
        void unknownButtonWithoutTextHasEmptyText() {
            assertThat(serializer.deserialize("{\"type\":\"teleport\"}", Button.class))
                    .isEqualTo(new UnknownButton("teleport", "", "{\"type\":\"teleport\"}"));
        }

        @Test
        void nonObjectUpdateIsRejected() {
            assertThatThrownBy(() -> serializer.deserialize("\"message_created\"", Update.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .rootCause().hasMessageContaining("Expected a JSON object for Update");
        }

        @Test
        void oversizedMalformedUpdateStillFallsBack() {
            String text = "x".repeat(5000);
            String json = "{\"update_type\":\"message_created\",\"timestamp\":1,\"padding\":\"" + text + "\"}";

            assertThat(serializer.deserialize(json, Update.class).updateType()).isEqualTo("message_created");
        }
    }

    @Nested
    class Batches {

        @Test
        void nullElementIsSkipped() {
            UpdateList list = serializer.deserialize(
                    "{\"updates\":[null,{\"update_type\":\"bot_stopped\",\"timestamp\":1}],\"marker\":3}",
                    UpdateList.class);

            assertThat(list.updates()).hasSize(1);
            assertThat(list.marker()).isEqualTo(3L);
        }

        @Test
        void nullBatchIsNull() {
            assertThat(serializer.deserialize("null", UpdateList.class)).isNull();
        }

        @Test
        void batchRoundTrips() {
            UpdateList list = new UpdateList(List.of(), 5L);

            assertThat(serializer.deserialize(serializer.serialize(list), UpdateList.class)).isEqualTo(list);
        }
    }

    @Nested
    class Failures {

        @Test
        void serializingAnInaccessibleJdkTypeFails() {
            assertThatThrownBy(() -> serializer.serialize(new Holder(Thread.currentThread())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Failed to serialize object");
        }

        @Test
        void nullJsonFails() {
            assertThatThrownBy(() -> serializer.deserialize(null, ActionRequestBody.class))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /** Enum with a constant that has a body, and names that are not all letters. */
    enum Flag {
        TRUE,
        WITH_BODY {
            @Override
            public String toString() {
                return "body";
            }
        }
    }

    record Operation(TextFormat action) {
    }

    record Holder(Thread thread) {
    }
}
