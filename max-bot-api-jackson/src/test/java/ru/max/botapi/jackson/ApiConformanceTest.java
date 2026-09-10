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
import org.junit.jupiter.api.Test;

import ru.max.botapi.model.ChatMember;
import ru.max.botapi.model.ChatPermission;
import ru.max.botapi.model.ContactAttachment;
import ru.max.botapi.model.GetSubscriptionsResult;
import ru.max.botapi.model.Subscription;
import ru.max.botapi.model.UpdateType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regressions pinning the library to the shapes documented at {@code dev.max.ru/docs-api}.
 */
class ApiConformanceTest {

    private JacksonMaxSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new JacksonMaxSerializer();
    }

    @Test
    @SuppressWarnings("deprecation") // the renamed permissions are asserted on purpose
    void chatMember_keepsRenamedPermissions() {
        // MAX renamed three permissions but still answers with the old names, and an
        // unrecognised name deserializes to null and is then dropped — which used to make a
        // bot look like it held no rights at all.
        String json = """
                {
                  "user_id": 1,
                  "name": "Admin",
                  "is_bot": false,
                  "last_activity_time": 0,
                  "last_access_time": 0,
                  "is_owner": false,
                  "is_admin": true,
                  "join_time": 0,
                  "permissions": ["read_all_messages", "edit_message",
                                  "delete_message", "post_edit_delete_message"],
                  "alias": "moderator"
                }
                """;

        ChatMember member = serializer.deserialize(json, ChatMember.class);

        assertThat(member.permissions()).containsExactly(
                ChatPermission.READ_ALL_MESSAGES,
                ChatPermission.EDIT_MESSAGE,
                ChatPermission.DELETE_MESSAGE,
                ChatPermission.POST_EDIT_DELETE_MESSAGE);
        assertThat(member.permissions().stream().map(ChatPermission::canonical))
                .containsExactly(
                        ChatPermission.READ_ALL_MESSAGES,
                        ChatPermission.EDIT,
                        ChatPermission.DELETE,
                        ChatPermission.WRITE);
        assertThat(member.alias()).isEqualTo("moderator");
    }

    @Test
    void subscriptions_surviveAnUnknownUpdateType() {
        // An update type this library does not know deserializes to null; the record must
        // drop it rather than reject the whole response.
        String json = """
                {
                  "subscriptions": [
                    {
                      "url": "https://example.com/webhook",
                      "update_types": ["message_created", "some_future_event"]
                    }
                  ]
                }
                """;

        GetSubscriptionsResult result =
                serializer.deserialize(json, GetSubscriptionsResult.class);

        Subscription subscription = result.subscriptions().getFirst();
        assertThat(subscription.updateTypes()).containsExactly(UpdateType.MESSAGE_CREATED);
    }

    @Test
    void contactPayload_readsMaxInfoAndHash() {
        String json = """
                {
                  "type": "contact",
                  "payload": {
                    "vcf_info": "BEGIN:VCARD\\r\\nEND:VCARD\\r\\n",
                    "max_info": {
                      "user_id": 7,
                      "first_name": "Ivan",
                      "is_bot": false,
                      "last_activity_time": 0
                    },
                    "hash": "deadbeef"
                  }
                }
                """;

        var attachment = (ContactAttachment) serializer.deserialize(
                json, ru.max.botapi.model.Attachment.class);

        assertThat(attachment.payload().hash()).isEqualTo("deadbeef");
        assertThat(attachment.payload().maxInfo()).isNotNull();
        assertThat(attachment.payload().maxInfo().userId()).isEqualTo(7L);
    }
}
