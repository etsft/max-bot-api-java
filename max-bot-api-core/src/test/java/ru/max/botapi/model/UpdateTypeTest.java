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
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateTypeTest {

    @Test
    void allValues_hasAll14KnownTypes() {
        assertThat(UpdateType.values()).hasSize(14);
    }

    @Test
    void value_returnsSnakeCaseString() {
        assertThat(UpdateType.MESSAGE_CREATED.value()).isEqualTo("message_created");
        assertThat(UpdateType.MESSAGE_CALLBACK.value()).isEqualTo("message_callback");
        assertThat(UpdateType.MESSAGE_EDITED.value()).isEqualTo("message_edited");
        assertThat(UpdateType.MESSAGE_REMOVED.value()).isEqualTo("message_removed");
        assertThat(UpdateType.BOT_ADDED.value()).isEqualTo("bot_added");
        assertThat(UpdateType.BOT_REMOVED.value()).isEqualTo("bot_removed");
        assertThat(UpdateType.USER_ADDED.value()).isEqualTo("user_added");
        assertThat(UpdateType.USER_REMOVED.value()).isEqualTo("user_removed");
        assertThat(UpdateType.BOT_STARTED.value()).isEqualTo("bot_started");
        assertThat(UpdateType.BOT_STOPPED.value()).isEqualTo("bot_stopped");
        assertThat(UpdateType.CHAT_TITLE_CHANGED.value()).isEqualTo("chat_title_changed");
        assertThat(UpdateType.MESSAGE_CONSTRUCTION_REQUEST.value())
                .isEqualTo("message_construction_request");
        assertThat(UpdateType.MESSAGE_CONSTRUCTED.value()).isEqualTo("message_constructed");
        assertThat(UpdateType.MESSAGE_CHAT_CREATED.value()).isEqualTo("message_chat_created");
    }

    @Test
    void of_returnsCorrectConstant() {
        assertThat(UpdateType.of("message_created")).isEqualTo(UpdateType.MESSAGE_CREATED);
        assertThat(UpdateType.of("bot_added")).isEqualTo(UpdateType.BOT_ADDED);
        assertThat(UpdateType.of("chat_title_changed"))
                .isEqualTo(UpdateType.CHAT_TITLE_CHANGED);
    }

    @Test
    void of_returnsNull_forUnknownType() {
        assertThat(UpdateType.of("some_future_update_type")).isNull();
        assertThat(UpdateType.of("")).isNull();
    }

    @Test
    void toStrings_convertsSet() {
        Set<UpdateType> types = Set.of(
                UpdateType.MESSAGE_CREATED, UpdateType.MESSAGE_CALLBACK);
        Set<String> strings = UpdateType.toStrings(types);
        assertThat(strings).containsExactlyInAnyOrder(
                "message_created", "message_callback");
    }

    @Test
    void toStrings_emptySet_returnsEmpty() {
        assertThat(UpdateType.toStrings(Set.of())).isEmpty();
    }

    @Test
    void allValuesStrings_hasAll14Values() {
        Set<String> all = UpdateType.allValues();
        assertThat(all).hasSize(14);
        assertThat(all).contains(
                "message_created", "message_callback", "message_edited",
                "message_removed", "bot_added", "bot_removed",
                "user_added", "user_removed", "bot_started", "bot_stopped",
                "chat_title_changed", "message_construction_request",
                "message_constructed", "message_chat_created");
    }

    @Test
    void toString_returnsApiValue() {
        assertThat(UpdateType.MESSAGE_CREATED.toString()).isEqualTo("message_created");
        assertThat(UpdateType.BOT_ADDED.toString()).isEqualTo("bot_added");
    }

    @Test
    void of_roundTrips_forAllConstants() {
        // Every UpdateType constant must round-trip through of(value())
        for (UpdateType type : UpdateType.values()) {
            assertThat(UpdateType.of(type.value()))
                    .as("of(\"%s\") should return %s", type.value(), type)
                    .isSameAs(type);
        }
    }

    @Test
    void subscriptionRequestBody_withUpdateTypes() {
        var body = new SubscriptionRequestBody(
                "https://example.com/webhook",
                List.of(UpdateType.MESSAGE_CREATED, UpdateType.BOT_ADDED),
                "secret");
        assertThat(body.url()).isEqualTo("https://example.com/webhook");
        assertThat(body.secret()).isEqualTo("secret");
        assertThat(body.updateTypes()).containsExactlyInAnyOrder(
                UpdateType.MESSAGE_CREATED, UpdateType.BOT_ADDED);
    }

    @Test
    void subscriptionRequestBody_nullTypes_isNull() {
        var body = new SubscriptionRequestBody(
                "https://example.com/webhook", null, null);
        assertThat(body.updateTypes()).isNull();
        assertThat(body.secret()).isNull();
    }
}
