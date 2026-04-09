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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ru.max.botapi.core.TypeReference;
import ru.max.botapi.model.ActionRequestBody;
import ru.max.botapi.model.Attachment;
import ru.max.botapi.model.AttachmentRequest;
import ru.max.botapi.model.AudioAttachment;
import ru.max.botapi.model.BotCommand;
import ru.max.botapi.model.BotPatch;
import ru.max.botapi.model.ButtonIntent;
import ru.max.botapi.model.CallbackButton;
import ru.max.botapi.model.Chat;
import ru.max.botapi.model.ChatAdminsList;
import ru.max.botapi.model.ChatList;
import ru.max.botapi.model.ChatMember;
import ru.max.botapi.model.ChatMembersList;
import ru.max.botapi.model.ChatPatch;
import ru.max.botapi.model.ChatPermission;
import ru.max.botapi.model.ChatStatus;
import ru.max.botapi.model.ChatType;
import ru.max.botapi.model.ConstructedMessage;
import ru.max.botapi.model.ContactAttachment;
import ru.max.botapi.model.FileAttachment;
import ru.max.botapi.model.GetPinnedMessageResult;
import ru.max.botapi.model.GetSubscriptionsResult;
import ru.max.botapi.model.Image;
import ru.max.botapi.model.InlineKeyboardAttachment;
import ru.max.botapi.model.LocationAttachment;
import ru.max.botapi.model.LocationAttachmentRequest;
import ru.max.botapi.model.MediaPayload;
import ru.max.botapi.model.MediaRequestPayload;
import ru.max.botapi.model.Message;
import ru.max.botapi.model.MessageBody;
import ru.max.botapi.model.MessageChatCreatedUpdate;
import ru.max.botapi.model.MessageConstructedUpdate;
import ru.max.botapi.model.MessageConstructionRequestUpdate;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.MessageLinkType;
import ru.max.botapi.model.MessageList;
import ru.max.botapi.model.MessageRecipient;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.NewMessageLink;
import ru.max.botapi.model.PhotoAttachment;
import ru.max.botapi.model.SendMessageResult;
import ru.max.botapi.model.SenderAction;
import ru.max.botapi.model.SimpleQueryResult;
import ru.max.botapi.model.StickerAttachment;
import ru.max.botapi.model.Subscription;
import ru.max.botapi.model.TextFormat;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateList;
import ru.max.botapi.model.UploadEndpoint;
import ru.max.botapi.model.UploadedInfo;
import ru.max.botapi.model.User;
import ru.max.botapi.model.UserIdsList;
import ru.max.botapi.model.VideoAttachment;
import ru.max.botapi.model.VideoAttachmentDetails;
import ru.max.botapi.model.VideoAttachmentRequest;
import ru.max.botapi.model.VideoThumbnail;
import ru.max.botapi.testsupport.FixtureLoader;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Additional serialization/deserialization tests for types NOT covered
 * in {@link JacksonMaxSerializerTest}.
 */
class AdditionalSerializationTest {

    private JacksonMaxSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new JacksonMaxSerializer();
    }

    private static final User USER = new User(99001L, "John Doe", null, null, "johndoe", false, 1700000100000L);
    private static final MessageRecipient RECIPIENT = new MessageRecipient(50001L, ChatType.CHAT);
    private static final MessageBody BODY = new MessageBody("msg_001", 1L, "Hello, world!", null, null);
    private static final Message MSG = new Message(USER, RECIPIENT, 1700000500000L, null, BODY, null, null, null);

    // ===== Model Round-Trips Not Yet Covered =====

    @Nested
    class AdditionalRoundTrips {

        @Test
        void messageBody_roundTrip() {
            var body = new MessageBody("mid_rt", 42L, "Round trip text", null, null);
            String json = serializer.serialize(body);
            MessageBody deserialized = serializer.deserialize(json, MessageBody.class);
            assertThat(deserialized.mid()).isEqualTo("mid_rt");
            assertThat(deserialized.seq()).isEqualTo(42L);
            assertThat(deserialized.text()).isEqualTo("Round trip text");
        }

        @Test
        void messageList_roundTrip() {
            var list = new MessageList(List.of(MSG), 777L);
            String json = serializer.serialize(list);
            MessageList deserialized = serializer.deserialize(json, MessageList.class);
            assertThat(deserialized.messages()).hasSize(1);
            assertThat(deserialized.messages().getFirst().body().mid()).isEqualTo("msg_001");
            assertThat(deserialized.marker()).isEqualTo(777L);
        }

        @Test
        void updateList_deserialization() {
            // Update types require the update_type discriminator which is only present
            // in API JSON (not produced by our serializer), so test deserialization only.
            String json = """
                    {
                      "updates": [
                        {
                          "update_type": "message_created",
                          "timestamp": 1700001000000,
                          "message": {
                            "sender": {
                              "user_id": 99001, "name": "John Doe",
                              "is_bot": false, "last_activity_time": 1700000100000
                            },
                            "recipient": {"chat_id": 50001, "chat_type": "chat"},
                            "timestamp": 1700000500000,
                            "body": {"mid": "msg_001", "seq": 1, "text": "Hello"}
                          },
                          "user_locale": "ru"
                        }
                      ],
                      "marker": 12345
                    }
                    """;
            UpdateList deserialized = serializer.deserialize(json, UpdateList.class);
            assertThat(deserialized.updates()).hasSize(1);
            assertThat(deserialized.updates().getFirst()).isInstanceOf(MessageCreatedUpdate.class);
            assertThat(deserialized.marker()).isEqualTo(12345L);
        }

        @Test
        void chatMember_roundTrip() {
            var member = new ChatMember(99001L, "Alice", null, null, "@alice", false, 1700000100000L,
                    "A member", "http://avatar.jpg", "http://full.jpg",
                    1700000200000L, true, true, 1699000000000L,
                    List.of(ChatPermission.WRITE, ChatPermission.PIN_MESSAGE));
            String json = serializer.serialize(member);
            ChatMember deserialized = serializer.deserialize(json, ChatMember.class);
            assertThat(deserialized.userId()).isEqualTo(99001L);
            assertThat(deserialized.name()).isEqualTo("Alice");
            assertThat(deserialized.isOwner()).isTrue();
            assertThat(deserialized.isAdmin()).isTrue();
            assertThat(deserialized.permissions()).containsExactly(
                    ChatPermission.WRITE, ChatPermission.PIN_MESSAGE);
        }

        @Test
        void chatMember_firstName_lastName_deserialized() {
            String json = "{\"user_id\":1,\"name\":\"Ivan Petrov\",\"first_name\":\"Ivan\","
                    + "\"last_name\":\"Petrov\",\"is_bot\":false,\"last_activity_time\":100,"
                    + "\"last_access_time\":0,\"is_owner\":false,\"is_admin\":false,\"join_time\":0}";
            ChatMember deserialized = serializer.deserialize(json, ChatMember.class);
            assertThat(deserialized.firstName()).isEqualTo("Ivan");
            assertThat(deserialized.lastName()).isEqualTo("Petrov");
            assertThat(deserialized.name()).isEqualTo("Ivan Petrov");
        }

        @Test
        void chatMember_allNewPermissions_deserialized() {
            String json = "{\"user_id\":1,\"name\":\"Admin\",\"first_name\":\"Admin\","
                    + "\"is_bot\":false,\"last_activity_time\":100,"
                    + "\"last_access_time\":0,\"is_owner\":true,\"is_admin\":true,\"join_time\":0,"
                    + "\"permissions\":[\"write\",\"read_all_messages\",\"can_call\","
                    + "\"edit_link\",\"delete\",\"edit\",\"view_stats\"]}";
            ChatMember deserialized = serializer.deserialize(json, ChatMember.class);
            assertThat(deserialized.permissions()).contains(
                    ChatPermission.WRITE, ChatPermission.READ_ALL_MESSAGES,
                    ChatPermission.CAN_CALL, ChatPermission.EDIT_LINK,
                    ChatPermission.DELETE, ChatPermission.EDIT, ChatPermission.VIEW_STATS);
            assertThat(deserialized.permissions()).doesNotContainNull();
        }

        @Test
        void chatMembersList_roundTrip() {
            var member = new ChatMember(1L, "Bob", null, null, null, false, 100L,
                    null, null, null, 200L, false, false, 50L, null);
            var list = new ChatMembersList(List.of(member), 999L);
            String json = serializer.serialize(list);
            ChatMembersList deserialized = serializer.deserialize(json, ChatMembersList.class);
            assertThat(deserialized.members()).hasSize(1);
            assertThat(deserialized.marker()).isEqualTo(999L);
        }

        @Test
        void chatAdminsList_roundTrip() {
            var admins = new ChatAdminsList(List.of(1L, 2L, 3L));
            String json = serializer.serialize(admins);
            ChatAdminsList deserialized = serializer.deserialize(json, ChatAdminsList.class);
            assertThat(deserialized.userIds()).containsExactly(1L, 2L, 3L);
        }

        @Test
        void subscription_roundTrip() {
            var sub = new Subscription("https://example.com/webhook",
                    List.of("message_created", "bot_started"));
            String json = serializer.serialize(sub);
            Subscription deserialized = serializer.deserialize(json, Subscription.class);
            assertThat(deserialized.url()).isEqualTo("https://example.com/webhook");
            assertThat(deserialized.updateTypes()).containsExactly("message_created", "bot_started");
        }

        @Test
        void subscriptionsResult_roundTrip() {
            var sub = new Subscription("https://example.com/wh", null);
            var result = new GetSubscriptionsResult(List.of(sub));
            String json = serializer.serialize(result);
            GetSubscriptionsResult deserialized = serializer.deserialize(json,
                    GetSubscriptionsResult.class);
            assertThat(deserialized.subscriptions()).hasSize(1);
            assertThat(deserialized.subscriptions().getFirst().url())
                    .isEqualTo("https://example.com/wh");
        }

        @Test
        void uploadEndpoint_roundTrip() {
            var endpoint = new UploadEndpoint("https://upload.example.com/abc", "tok123");
            String json = serializer.serialize(endpoint);
            UploadEndpoint deserialized = serializer.deserialize(json, UploadEndpoint.class);
            assertThat(deserialized.url()).isEqualTo("https://upload.example.com/abc");
            assertThat(deserialized.token()).isEqualTo("tok123");
        }

        @Test
        void uploadedInfo_roundTrip() {
            var info = new UploadedInfo("upload_token_abc");
            String json = serializer.serialize(info);
            UploadedInfo deserialized = serializer.deserialize(json, UploadedInfo.class);
            assertThat(deserialized.token()).isEqualTo("upload_token_abc");
        }

        @Test
        void simpleQueryResult_roundTrip() {
            var result = new SimpleQueryResult(true, "Done");
            String json = serializer.serialize(result);
            SimpleQueryResult deserialized = serializer.deserialize(json, SimpleQueryResult.class);
            assertThat(deserialized.success()).isTrue();
            assertThat(deserialized.message()).isEqualTo("Done");
        }

        @Test
        void pinnedMessageResult_roundTrip() {
            var result = new GetPinnedMessageResult(MSG);
            String json = serializer.serialize(result);
            GetPinnedMessageResult deserialized = serializer.deserialize(json,
                    GetPinnedMessageResult.class);
            assertThat(deserialized.message()).isNotNull();
            assertThat(deserialized.message().body().mid()).isEqualTo("msg_001");
        }

        @Test
        void pinnedMessageResult_nullMessage_roundTrip() {
            var result = new GetPinnedMessageResult(null);
            String json = serializer.serialize(result);
            GetPinnedMessageResult deserialized = serializer.deserialize(json,
                    GetPinnedMessageResult.class);
            assertThat(deserialized.message()).isNull();
        }

        @Test
        void sendMessageResult_roundTrip() {
            var result = new SendMessageResult(MSG);
            String json = serializer.serialize(result);
            SendMessageResult deserialized = serializer.deserialize(json, SendMessageResult.class);
            assertThat(deserialized.message().body().text()).isEqualTo("Hello, world!");
        }

        @Test
        void videoAttachmentDetails_roundTrip() {
            var details = new VideoAttachmentDetails(
                    "https://cdn.example.com/video.mp4", "vtok",
                    new VideoThumbnail("https://cdn.example.com/thumb.jpg"), 1920, 1080, 120);
            String json = serializer.serialize(details);
            VideoAttachmentDetails deserialized = serializer.deserialize(json,
                    VideoAttachmentDetails.class);
            assertThat(deserialized.url()).isEqualTo("https://cdn.example.com/video.mp4");
            assertThat(deserialized.token()).isEqualTo("vtok");
            assertThat(deserialized.width()).isEqualTo(1920);
            assertThat(deserialized.height()).isEqualTo(1080);
            assertThat(deserialized.duration()).isEqualTo(120);
        }

        @Test
        void chatPatch_roundTrip() {
            var patch = new ChatPatch("Updated Title", new Image("http://icon.png"), "pin_123", true);
            String json = serializer.serialize(patch);
            assertThatJson(json).node("title").isEqualTo("Updated Title");
            assertThatJson(json).node("notify").isEqualTo(true);
            ChatPatch deserialized = serializer.deserialize(json, ChatPatch.class);
            assertThat(deserialized.title()).isEqualTo("Updated Title");
            assertThat(deserialized.icon().url()).isEqualTo("http://icon.png");
            assertThat(deserialized.pin()).isEqualTo("pin_123");
            assertThat(deserialized.notifyRecipients()).isTrue();
        }

        @Test
        void botPatch_roundTrip() {
            var cmd = new BotCommand("help", "Show help");
            var patch = new BotPatch("NewBot", "New desc", List.of(cmd), null);
            String json = serializer.serialize(patch);
            BotPatch deserialized = serializer.deserialize(json, BotPatch.class);
            assertThat(deserialized.name()).isEqualTo("NewBot");
            assertThat(deserialized.description()).isEqualTo("New desc");
            assertThat(deserialized.commands()).hasSize(1);
            assertThat(deserialized.commands().getFirst().name()).isEqualTo("help");
        }

        @Test
        void actionRequestBody_roundTrip() {
            var body = new ActionRequestBody(SenderAction.TYPING_ON);
            String json = serializer.serialize(body);
            assertThatJson(json).node("action").isEqualTo("typing_on");
            ActionRequestBody deserialized = serializer.deserialize(json, ActionRequestBody.class);
            assertThat(deserialized.action()).isEqualTo(SenderAction.TYPING_ON);
        }

        @Test
        void userIdsList_roundTrip() {
            var ids = new UserIdsList(List.of(111L, 222L, 333L));
            String json = serializer.serialize(ids);
            UserIdsList deserialized = serializer.deserialize(json, UserIdsList.class);
            assertThat(deserialized.userIds()).containsExactly(111L, 222L, 333L);
        }
    }

    // ===== NewMessageBody Variations =====

    @Nested
    class NewMessageBodyVariations {

        @Test
        void newMessageBody_withNotify_roundTrip() {
            var body = new NewMessageBody("Hello", null, null, true, null);
            String json = serializer.serialize(body);
            assertThatJson(json).node("notify").isEqualTo(true);
            assertThatJson(json).node("notify_recipients").isAbsent();
            NewMessageBody deserialized = serializer.deserialize(json, NewMessageBody.class);
            assertThat(deserialized.notifyRecipients()).isTrue();
        }

        @Test
        void newMessageBody_withMarkdown_roundTrip() {
            var body = new NewMessageBody("**bold**", null, null, null, TextFormat.MARKDOWN);
            String json = serializer.serialize(body);
            assertThatJson(json).node("format").isEqualTo("markdown");
            assertThatJson(json).node("notify").isAbsent(); // null → omitted by NON_NULL
            NewMessageBody deserialized = serializer.deserialize(json, NewMessageBody.class);
            assertThat(deserialized.format()).isEqualTo(TextFormat.MARKDOWN);
            assertThat(deserialized.text()).isEqualTo("**bold**");
        }

        @Test
        void newMessageBody_withLink_roundTrip() {
            var link = new NewMessageLink(MessageLinkType.REPLY, "mid_original");
            var body = new NewMessageBody("Reply text", null, link, null, null);
            String json = serializer.serialize(body);
            assertThatJson(json).node("link.type").isEqualTo("reply");
            assertThatJson(json).node("link.mid").isEqualTo("mid_original");
            NewMessageBody deserialized = serializer.deserialize(json, NewMessageBody.class);
            assertThat(deserialized.link()).isNotNull();
            assertThat(deserialized.link().type()).isEqualTo(MessageLinkType.REPLY);
            assertThat(deserialized.link().mid()).isEqualTo("mid_original");
        }

        @Test
        void newMessageBody_withForwardLink_roundTrip() {
            var link = new NewMessageLink(MessageLinkType.FORWARD, "fwd_mid");
            var body = new NewMessageBody(null, null, link, false, TextFormat.HTML);
            String json = serializer.serialize(body);
            assertThatJson(json).node("link.type").isEqualTo("forward");
            assertThatJson(json).node("format").isEqualTo("html");
            NewMessageBody deserialized = serializer.deserialize(json, NewMessageBody.class);
            assertThat(deserialized.link().type()).isEqualTo(MessageLinkType.FORWARD);
            assertThat(deserialized.format()).isEqualTo(TextFormat.HTML);
        }
    }

    // ===== Edge Case Serialization Scenarios =====

    @Nested
    class EdgeCaseSerialization {

        @Test
        void emptyList_serialization() {
            var body = new MessageBody("m1", 1L, null, List.of(), null);
            String json = serializer.serialize(body);
            assertThatJson(json).node("attachments").isArray().isEmpty();
        }

        @Test
        void nullList_serialization() {
            var body = new MessageBody("m1", 1L, null, null, null);
            String json = serializer.serialize(body);
            assertThatJson(json).node("attachments").isAbsent();
        }

        @Test
        void unknownField_ignored() {
            String json = """
                    {
                      "mid": "m1",
                      "seq": 1,
                      "text": "Hello",
                      "future_field_xyz": 42,
                      "another_unknown": {"nested": true}
                    }
                    """;
            MessageBody body = serializer.deserialize(json, MessageBody.class);
            assertThat(body.mid()).isEqualTo("m1");
            assertThat(body.text()).isEqualTo("Hello");
        }

        @Test
        void snakeCase_fieldMapping() {
            var member = new ChatMember(1L, "Test", null, null, null, true, 100L,
                    null, null, null, 200L, true, false, 50L, null);
            String json = serializer.serialize(member);
            assertThatJson(json).node("user_id").isEqualTo(1);
            assertThatJson(json).node("is_bot").isEqualTo(true);
            assertThatJson(json).node("last_activity_time").isEqualTo(100);
            assertThatJson(json).node("last_access_time").isEqualTo(200);
            assertThatJson(json).node("is_owner").isEqualTo(true);
            assertThatJson(json).node("is_admin").isEqualTo(false);
            assertThatJson(json).node("join_time").isEqualTo(50);
        }

        @Test
        void maxLengthText_4000chars() {
            String longText = "X".repeat(4000);
            var body = new NewMessageBody(longText, null, null, null, null);
            String json = serializer.serialize(body);
            NewMessageBody deserialized = serializer.deserialize(json, NewMessageBody.class);
            assertThat(deserialized.text()).hasSize(4000);
            assertThat(deserialized.text()).isEqualTo(longText);
        }

        @Test
        void unicodeText_roundTrip() {
            String unicode = "Привет мир! \uD83D\uDE00 日本語テスト 🇷🇺";
            var body = new NewMessageBody(unicode, null, null, null, null);
            String json = serializer.serialize(body);
            NewMessageBody deserialized = serializer.deserialize(json, NewMessageBody.class);
            assertThat(deserialized.text()).isEqualTo(unicode);
        }
    }

    // ===== Complex Attachment Scenarios =====

    @Nested
    class ComplexAttachments {

        @Test
        void messageWithAllAttachmentTypes() {
            // Build a message body with multiple different attachment types
            var photo = new PhotoAttachment(
                    new PhotoAttachment.PhotoPayload("http://photo.jpg", "ptok", 1L));
            var video = new VideoAttachment(
                    new VideoAttachment.VideoPayload("http://video.mp4", "vtok", null),
                    new VideoThumbnail("http://thumb.jpg"), 1920, 1080, 60);
            var audio = new AudioAttachment(new MediaPayload("http://audio.mp3", "atok"));
            var file = new FileAttachment(new MediaPayload("http://doc.pdf", "ftok"),
                    "doc.pdf", 1024L);
            var location = new LocationAttachment(55.75, 37.62);

            var body = new MessageBody("multi_mid", 1L, "Multi attachment",
                    List.of(photo, video, audio, file, location), null);
            String json = serializer.serialize(body);
            MessageBody deserialized = serializer.deserialize(json, MessageBody.class);
            assertThat(deserialized.attachments()).hasSize(5);
            assertThat(deserialized.attachments().get(0)).isInstanceOf(PhotoAttachment.class);
            assertThat(deserialized.attachments().get(1)).isInstanceOf(VideoAttachment.class);
            assertThat(deserialized.attachments().get(2)).isInstanceOf(AudioAttachment.class);
            assertThat(deserialized.attachments().get(3)).isInstanceOf(FileAttachment.class);
            assertThat(deserialized.attachments().get(4)).isInstanceOf(LocationAttachment.class);
        }

        @Test
        void constructedMessage_serialization() {
            var cm = new ConstructedMessage(USER, 1700002200000L, null, BODY);
            String json = serializer.serialize(cm);
            assertThatJson(json).node("timestamp").isEqualTo(1700002200000L);
            assertThatJson(json).node("body.mid").isEqualTo("msg_001");
            ConstructedMessage deserialized = serializer.deserialize(json,
                    ConstructedMessage.class);
            assertThat(deserialized.sender().name()).isEqualTo("John Doe");
            assertThat(deserialized.body().text()).isEqualTo("Hello, world!");
        }
    }

    // ===== Button Exhaustive Serialization =====

    @Nested
    class ButtonSerialization {

        @Test
        void allButtonTypes_serializeWithCorrectType() {
            var callback = new ru.max.botapi.model.CallbackButton("CB", "pay", ButtonIntent.POSITIVE);
            var link = new ru.max.botapi.model.LinkButton("Link", "https://example.com");
            var contact = new ru.max.botapi.model.RequestContactButton("Contact");
            var geo = new ru.max.botapi.model.RequestGeoLocationButton("Geo", true);
            var chat = new ru.max.botapi.model.ChatButton("Chat", "Title", "Desc", "sp", "uuid1");
            var openApp = new ru.max.botapi.model.OpenAppButton("App", "https://app.example.com", "data");
            var message = new ru.max.botapi.model.MessageButton("Msg", "Hello!");

            ru.max.botapi.model.Button[] buttons = {callback, link, contact, geo, chat, openApp, message};
            String[] expectedTypes = {"callback", "link", "request_contact", "request_geo_location",
                    "chat", "open_app", "message"};

            for (int i = 0; i < buttons.length; i++) {
                String json = serializer.serialize(buttons[i]);
                assertThatJson(json).node("type").isEqualTo(expectedTypes[i]);
                // Verify round-trip via Button interface deserialization
                ru.max.botapi.model.Button deserialized = serializer.deserialize(json,
                        ru.max.botapi.model.Button.class);
                assertThat(deserialized.type()).isEqualTo(expectedTypes[i]);
                assertThat(deserialized.text()).isEqualTo(buttons[i].text());
            }
        }
    }

    // ===== Enum Exhaustive Serialization =====

    @Nested
    class EnumExhaustiveSerialization {

        @Test
        void allChatTypes_roundTrip() {
            for (ChatType ct : ChatType.values()) {
                String json = serializer.serialize(ct);
                ChatType deserialized = serializer.deserialize(json, ChatType.class);
                assertThat(deserialized).isEqualTo(ct);
            }
        }

        @Test
        void allChatStatuses_roundTrip() {
            for (ChatStatus cs : ChatStatus.values()) {
                String json = serializer.serialize(cs);
                ChatStatus deserialized = serializer.deserialize(json, ChatStatus.class);
                assertThat(deserialized).isEqualTo(cs);
            }
        }

        @Test
        void allSenderActions_roundTrip() {
            for (SenderAction sa : SenderAction.values()) {
                String json = serializer.serialize(sa);
                SenderAction deserialized = serializer.deserialize(json, SenderAction.class);
                assertThat(deserialized).isEqualTo(sa);
            }
        }

        @Test
        void allChatPermissions_roundTrip() {
            for (ChatPermission cp : ChatPermission.values()) {
                String json = serializer.serialize(cp);
                ChatPermission deserialized = serializer.deserialize(json, ChatPermission.class);
                assertThat(deserialized).isEqualTo(cp);
            }
        }

        @Test
        void allMessageLinkTypes_roundTrip() {
            for (MessageLinkType mlt : MessageLinkType.values()) {
                String json = serializer.serialize(mlt);
                MessageLinkType deserialized = serializer.deserialize(json, MessageLinkType.class);
                assertThat(deserialized).isEqualTo(mlt);
            }
        }

        @Test
        void allTextFormats_roundTrip() {
            for (TextFormat tf : TextFormat.values()) {
                String json = serializer.serialize(tf);
                TextFormat deserialized = serializer.deserialize(json, TextFormat.class);
                assertThat(deserialized).isEqualTo(tf);
            }
        }

        @Test
        void allButtonIntents_roundTrip() {
            for (ButtonIntent bi : ButtonIntent.values()) {
                String json = serializer.serialize(bi);
                ButtonIntent deserialized = serializer.deserialize(json, ButtonIntent.class);
                assertThat(deserialized).isEqualTo(bi);
            }
        }
    }

    // ===== Fixture-Based Update Deserialization (for types that need deeper assertion) =====

    @Nested
    class FixtureBasedDeserialization {

        @Test
        void messageConstructionRequestUpdate_fromFixture() {
            Update update = serializer.deserialize(
                    FixtureLoader.loadFixture("updates/message-construction-request.json"), Update.class);
            assertThat(update).isInstanceOf(MessageConstructionRequestUpdate.class);
            MessageConstructionRequestUpdate mcru = (MessageConstructionRequestUpdate) update;
            assertThat(mcru.sessionId()).isEqualTo("session_abc123");
            assertThat(mcru.data()).isEqualTo("step1");
            assertThat(mcru.input()).isEqualTo("user input text");
            assertThat(mcru.userLocale()).isEqualTo("en");
            assertThat(mcru.user().userId()).isEqualTo(99001L);
        }

        @Test
        void messageConstructedUpdate_fromFixture() {
            Update update = serializer.deserialize(
                    FixtureLoader.loadFixture("updates/message-constructed.json"), Update.class);
            assertThat(update).isInstanceOf(MessageConstructedUpdate.class);
            MessageConstructedUpdate mcu = (MessageConstructedUpdate) update;
            assertThat(mcu.sessionId()).isEqualTo("session_abc123");
            assertThat(mcu.user().name()).isEqualTo("John Doe");
            assertThat(mcu.message().body().mid()).isEqualTo("cmsg_001");
            assertThat(mcu.message().body().text()).isEqualTo("Constructed message text");
        }

        @Test
        void messageChatCreatedUpdate_fromFixture() {
            Update update = serializer.deserialize(
                    FixtureLoader.loadFixture("updates/message-chat-created.json"), Update.class);
            assertThat(update).isInstanceOf(MessageChatCreatedUpdate.class);
            MessageChatCreatedUpdate mccu = (MessageChatCreatedUpdate) update;
            assertThat(mccu.chat().chatId()).isEqualTo(50003L);
            assertThat(mccu.chat().title()).isEqualTo("New Chat");
            assertThat(mccu.messageId()).isEqualTo("msg_020");
            assertThat(mccu.startPayload()).isEqualTo("start_data");
            assertThat(mccu.chat().participantsCount()).isEqualTo(2);
        }

        @Test
        void chatList_fromFixture_roundTrip() {
            String json = FixtureLoader.loadFixture("chat-list.json");
            ChatList list = serializer.deserialize(json, ChatList.class);
            assertThat(list.chats()).hasSize(2);
            assertThat(list.marker()).isEqualTo(50002L);
            String reserialized = serializer.serialize(list);
            ChatList roundTripped = serializer.deserialize(reserialized, ChatList.class);
            assertThat(roundTripped.chats()).hasSize(2);
            assertThat(roundTripped.marker()).isEqualTo(50002L);
        }

        @Test
        void newMessageBody_fromFixture_deserializesNotify() {
            String json = FixtureLoader.loadFixture("new-message-body.json");
            NewMessageBody body = serializer.deserialize(json, NewMessageBody.class);
            assertThat(body.text()).isEqualTo("Hello from bot");
            assertThat(body.notifyRecipients()).isTrue();
            assertThat(body.format()).isEqualTo(TextFormat.MARKDOWN);
        }
    }

    // ===== Attachment Request Serialization =====

    @Nested
    class AttachmentRequestSerialization {

        @Test
        void locationAttachmentRequest_serializesWithType() {
            AttachmentRequest req = new LocationAttachmentRequest(55.75, 37.62);
            String json = serializer.serialize(req);
            assertThatJson(json).node("type").isEqualTo("location");
            assertThatJson(json).node("latitude").isEqualTo(55.75);
            assertThatJson(json).node("longitude").isEqualTo(37.62);
        }

        @Test
        void videoAttachmentRequest_roundTrip() {
            var payload = new MediaRequestPayload("vtok_123");
            AttachmentRequest req = new VideoAttachmentRequest(payload);
            String json = serializer.serialize(req);
            assertThatJson(json).node("type").isEqualTo("video");
            assertThatJson(json).node("payload.token").isEqualTo("vtok_123");
            AttachmentRequest deserialized = serializer.deserialize(json, AttachmentRequest.class);
            assertThat(deserialized).isInstanceOf(VideoAttachmentRequest.class);
        }

        @Test
        void newMessageBody_withAttachments_roundTrip() {
            var att = new LocationAttachmentRequest(40.0, -74.0);
            var body = new NewMessageBody("With attachment", List.of(att), null, null, null);
            String json = serializer.serialize(body);
            assertThatJson(json).node("attachments").isArray().hasSize(1);
            assertThatJson(json).node("attachments[0].type").isEqualTo("location");
            NewMessageBody deserialized = serializer.deserialize(json, NewMessageBody.class);
            assertThat(deserialized.attachments()).hasSize(1);
        }
    }

    // ===== SimpleQueryResult variations =====

    @Nested
    class SimpleQueryResultVariations {

        @Test
        void simpleQueryResult_falseWithMessage() {
            String json = """
                    {"success": false, "message": "Chat not found"}
                    """;
            SimpleQueryResult result = serializer.deserialize(json, SimpleQueryResult.class);
            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Chat not found");
        }

        @Test
        void simpleQueryResult_trueNoMessage() {
            String json = """
                    {"success": true}
                    """;
            SimpleQueryResult result = serializer.deserialize(json, SimpleQueryResult.class);
            assertThat(result.success()).isTrue();
            assertThat(result.message()).isNull();
        }
    }
}
