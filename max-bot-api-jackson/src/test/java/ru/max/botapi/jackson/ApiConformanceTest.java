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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.max.botapi.model.AudioAttachment;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.ChatMember;
import ru.max.botapi.model.ChatPatch;
import ru.max.botapi.model.ChatPermission;
import ru.max.botapi.model.ClipboardButton;
import ru.max.botapi.model.CommentCreatedUpdate;
import ru.max.botapi.model.CommentEditedUpdate;
import ru.max.botapi.model.CommentMessage;
import ru.max.botapi.model.CommentRemovedUpdate;
import ru.max.botapi.model.ContactAttachment;
import ru.max.botapi.model.DialogClearedUpdate;
import ru.max.botapi.model.DialogMutedUpdate;
import ru.max.botapi.model.DialogRemovedUpdate;
import ru.max.botapi.model.DialogUnmutedUpdate;
import ru.max.botapi.model.GetSubscriptionsResult;
import ru.max.botapi.model.MarkupElement;
import ru.max.botapi.model.Message;
import ru.max.botapi.model.MessageBody;
import ru.max.botapi.model.MessageButton;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.MessageRemovedUpdate;
import ru.max.botapi.model.NewCommentBody;
import ru.max.botapi.model.OpenAppButton;
import ru.max.botapi.model.PhotoAttachmentRequestPayload;
import ru.max.botapi.model.Subscription;
import ru.max.botapi.model.TextFormat;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateType;
import ru.max.botapi.model.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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
                      "update_types": ["comment_created", "some_future_event"]
                    }
                  ]
                }
                """;

        GetSubscriptionsResult result =
                serializer.deserialize(json, GetSubscriptionsResult.class);

        Subscription subscription = result.subscriptions().getFirst();
        assertThat(subscription.updateTypes()).containsExactly(UpdateType.COMMENT_CREATED);
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

    @Test
    void clipboardButton_roundTrips() {
        String json = serializer.serialize(new ClipboardButton("Copy", "PROMO-1"));

        assertThat(json).contains("\"type\":\"clipboard\"")
                .contains("\"payload\":\"PROMO-1\"");
        assertThat(serializer.deserialize(json, ru.max.botapi.model.Button.class))
                .isEqualTo(new ClipboardButton("Copy", "PROMO-1"));
    }

    @Test
    void newCommentBody_serializesWithoutAttachments() {
        String json = serializer.serialize(
                new NewCommentBody("Hi", null, TextFormat.MARKDOWN));

        assertThat(json).contains("\"text\":\"Hi\"")
                .contains("\"format\":\"markdown\"")
                .doesNotContain("attachments");
    }

    @Test
    void dialogUpdates_deserializeToTheirOwnTypes() {
        assertThat(parse("dialog_cleared")).isInstanceOf(DialogClearedUpdate.class);
        assertThat(parse("dialog_muted")).isInstanceOf(DialogMutedUpdate.class);
        assertThat(parse("dialog_unmuted")).isInstanceOf(DialogUnmutedUpdate.class);

        Update removed = parse("dialog_removed");
        assertThat(removed).isInstanceOf(DialogRemovedUpdate.class);
        assertThat(((DialogRemovedUpdate) removed).chatId()).isEqualTo(42L);
        assertThat(removed.updateType()).isEqualTo("dialog_removed");
    }

    @Test
    void dialogMuted_carriesMutedUntilAndUserLocale() {
        String json = """
                {
                  "update_type": "dialog_muted",
                  "timestamp": 1700000000000,
                  "chat_id": 42,
                  "user": {"user_id": 7, "first_name": "Ivan", "is_bot": false},
                  "muted_until": 1700000600000,
                  "user_locale": "ru"
                }
                """;

        var muted = (DialogMutedUpdate) serializer.deserialize(json, Update.class);

        assertThat(muted.mutedUntil()).isEqualTo(1700000600000L);
        assertThat(muted.userLocale()).isEqualTo("ru");
    }

    @Test
    void commentUpdates_carryTheCommentAndItsPostId() {
        String json = """
                {
                  "update_type": "comment_created",
                  "timestamp": 1700000000000,
                  "message": {
                    "recipient": {
                      "chat_id": 42,
                      "chat_type": "channel",
                      "post_id": "mid.post123"
                    },
                    "timestamp": 1700000000000,
                    "body": {"mid": "mid.c1", "seq": 1, "text": "Nice post"}
                  }
                }
                """;

        Update update = serializer.deserialize(json, Update.class);

        assertThat(update).isInstanceOf(CommentCreatedUpdate.class);
        CommentMessage comment = ((CommentCreatedUpdate) update).message();
        assertThat(comment).isNotNull();
        assertThat(comment.body().text()).isEqualTo("Nice post");
        assertThat(comment.recipient().postId()).isEqualTo("mid.post123");

        assertThat(serializer.deserialize(
                json.replace("comment_created", "comment_edited"), Update.class))
                .isInstanceOf(CommentEditedUpdate.class);
    }

    @Test
    void commentRemoved_carriesTheIdentifiersOfTheRemovedComment() {
        // The comment is gone, so this event has no message object — only identifiers.
        String json = """
                {
                  "update_type": "comment_removed",
                  "timestamp": 1700000000000,
                  "message_id": "mid.comment",
                  "chat_id": 42,
                  "user_id": 7,
                  "post_id": "mid.post"
                }
                """;

        assertThat(serializer.deserialize(json, Update.class)).isEqualTo(
                new CommentRemovedUpdate(1700000000000L, "mid.comment", 42L, 7L, "mid.post"));
    }

    @Test
    void messageRemoved_carriesThePostId() {
        var removed = (MessageRemovedUpdate) serializer.deserialize("""
                {"update_type": "message_removed", "timestamp": 1, "message_id": "mid.c",
                 "chat_id": 42, "user_id": 7, "post_id": "mid.post"}
                """, Update.class);

        assertThat(removed.postId()).isEqualTo("mid.post");
    }

    @Test
    void forwardOnlyMessage_hasNoBody() {
        String message = """
                {
                  "recipient": {"chat_id": 1, "chat_type": "chat"},
                  "timestamp": 1,
                  "link": {
                    "type": "forward",
                    "chat_id": 2,
                    "message": {"mid": "mid.x", "seq": 1, "text": "forwarded"}
                  },
                  "body": null
                }
                """;

        Message parsed = serializer.deserialize(message, Message.class);
        assertThat(parsed.body()).isNull();
        assertThat(parsed.link()).isNotNull();

        Update update = serializer.deserialize("""
                {"update_type": "message_created", "timestamp": 1, "message": %s}
                """.formatted(message), Update.class);
        assertThat(update).isInstanceOf(MessageCreatedUpdate.class);
    }

    @Test
    void markup_keepsTheLinkTargetAndTheMentionedUser() {
        MessageBody body = serializer.deserialize("""
                {"mid": "m", "seq": 1, "text": "see @ivan",
                 "markup": [
                   {"type": "link", "from": 0, "length": 3, "url": "https://max.ru"},
                   {"type": "user_mention", "from": 4, "length": 5, "user_link": "@ivan"},
                   {"type": "user_mention", "from": 4, "length": 5, "user_id": 7}
                 ]}
                """, MessageBody.class);

        assertThat(body.markup())
                .extracting(MarkupElement::url, MarkupElement::userLink, MarkupElement::userId)
                .containsExactly(
                        tuple("https://max.ru", null, null),
                        tuple(null, "@ivan", null),
                        tuple(null, null, 7L));
    }

    @Test
    void user_withoutLastActivityTime_leavesItUnset() {
        // Privacy settings can hide it; zero would read as a real timestamp.
        User user = serializer.deserialize("""
                {"user_id": 7, "first_name": "Ivan", "username": null, "is_bot": false}
                """, User.class);

        assertThat(user.lastActivityTime()).isNull();
    }

    @Test
    void subscription_carriesItsCreationTime() {
        GetSubscriptionsResult result = serializer.deserialize("""
                {"subscriptions": [{"url": "https://example.com/webhook",
                                    "time": 1700000000000, "update_types": null}]}
                """, GetSubscriptionsResult.class);

        assertThat(result.subscriptions().getFirst().time()).isEqualTo(1700000000000L);
    }

    @Test
    void audioAttachment_carriesItsTranscription() {
        var audio = (AudioAttachment) serializer.deserialize("""
                {"type": "audio", "payload": {"url": "https://a", "token": "t"},
                 "transcription": "hello"}
                """, ru.max.botapi.model.Attachment.class);

        assertThat(audio.transcription()).isEqualTo("hello");
    }

    @Test
    void chatPatch_setsTheIconFromATokenOrUploadedPhotos() {
        String byToken = serializer.serialize(new ChatPatch(null, null,
                new PhotoAttachmentRequestPayload("tok", null, null), null, null));
        assertThat(byToken).isEqualTo("{\"icon\":{\"token\":\"tok\"}}");

        String byPhotos = serializer.serialize(new ChatPatch(null, null,
                new PhotoAttachmentRequestPayload(null, null,
                        Map.of("p1", new PhotoAttachmentRequestPayload.TokenRef("tok"))),
                null, null));
        assertThat(byPhotos).isEqualTo("{\"icon\":{\"photos\":{\"p1\":{\"token\":\"tok\"}}}}");
    }

    @Test
    void openAppButton_usesTheDocumentedFields() {
        assertThat(serializer.deserialize("""
                {"type": "open_app", "text": "Open", "web_app": "some_bot", "contact_id": 123}
                """, Button.class))
                .isEqualTo(new OpenAppButton("Open", "some_bot", 123L, null));

        assertThat(serializer.serialize(OpenAppButton.ofWebApp("Open", "some_bot")))
                .contains("\"type\":\"open_app\"")
                .contains("\"web_app\":\"some_bot\"")
                .doesNotContain("url")
                .doesNotContain("contact_id");
    }

    @Test
    void messageButton_sendsItsOwnText() {
        assertThat(serializer.deserialize("""
                {"type": "message", "text": "/start"}
                """, Button.class))
                .isEqualTo(new MessageButton("/start"));
    }

    @Test
    void everyDocumentedUpdateType_hasItsOwnUpdateClass() {
        List<UpdateType> types = List.of(
                UpdateType.DIALOG_CLEARED, UpdateType.DIALOG_MUTED,
                UpdateType.DIALOG_UNMUTED, UpdateType.DIALOG_REMOVED,
                UpdateType.COMMENT_CREATED, UpdateType.COMMENT_EDITED,
                UpdateType.COMMENT_REMOVED);

        for (UpdateType type : types) {
            Update update = serializer.deserialize("""
                    {"update_type": "%s", "timestamp": 1}
                    """.formatted(type.value()), Update.class);
            assertThat(update.updateType()).isEqualTo(type.value());
        }
    }

    private Update parse(String updateType) {
        return serializer.deserialize("""
                {
                  "update_type": "%s",
                  "timestamp": 1700000000000,
                  "chat_id": 42,
                  "user": {
                    "user_id": 7,
                    "first_name": "Ivan",
                    "is_bot": false,
                    "last_activity_time": 0
                  }
                }
                """.formatted(updateType), Update.class);
    }
}