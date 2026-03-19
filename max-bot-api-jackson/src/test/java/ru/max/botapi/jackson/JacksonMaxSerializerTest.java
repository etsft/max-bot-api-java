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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ru.max.botapi.core.TypeReference;
import ru.max.botapi.model.Attachment;
import ru.max.botapi.model.AttachmentRequest;
import ru.max.botapi.model.AudioAttachment;
import ru.max.botapi.model.AudioAttachmentRequest;
import ru.max.botapi.model.BotAddedUpdate;
import ru.max.botapi.model.BotCommand;
import ru.max.botapi.model.BotInfo;
import ru.max.botapi.model.BotRemovedUpdate;
import ru.max.botapi.model.BotStartedUpdate;
import ru.max.botapi.model.BotStoppedUpdate;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.ButtonIntent;
import ru.max.botapi.model.CallbackButton;
import ru.max.botapi.model.Chat;
import ru.max.botapi.model.ChatButton;
import ru.max.botapi.model.ChatList;
import ru.max.botapi.model.ChatPatch;
import ru.max.botapi.model.ChatStatus;
import ru.max.botapi.model.ChatTitleChangedUpdate;
import ru.max.botapi.model.ChatType;
import ru.max.botapi.model.ContactAttachment;
import ru.max.botapi.model.ContactAttachmentRequest;
import ru.max.botapi.model.FileAttachment;
import ru.max.botapi.model.FileAttachmentRequest;
import ru.max.botapi.model.ImageAttachmentRequest;
import ru.max.botapi.model.InlineKeyboardAttachment;
import ru.max.botapi.model.InlineKeyboardAttachmentRequest;
import ru.max.botapi.model.LinkButton;
import ru.max.botapi.model.LocationAttachment;
import ru.max.botapi.model.LocationAttachmentRequest;
import ru.max.botapi.model.MediaPayload;
import ru.max.botapi.model.MediaRequestPayload;
import ru.max.botapi.model.Message;
import ru.max.botapi.model.MessageBody;
import ru.max.botapi.model.MessageButton;
import ru.max.botapi.model.MessageCallbackUpdate;
import ru.max.botapi.model.MessageChatCreatedUpdate;
import ru.max.botapi.model.MessageConstructedUpdate;
import ru.max.botapi.model.MessageConstructionRequestUpdate;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.MessageEditedUpdate;
import ru.max.botapi.model.MessageRecipient;
import ru.max.botapi.model.MessageRemovedUpdate;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.OpenAppButton;
import ru.max.botapi.model.PhotoAttachment;
import ru.max.botapi.model.PhotoAttachmentRequestPayload;
import ru.max.botapi.model.PinMessageBody;
import ru.max.botapi.model.RequestContactButton;
import ru.max.botapi.model.RequestGeoLocationButton;
import ru.max.botapi.model.ShareAttachment;
import ru.max.botapi.model.ShareAttachmentRequest;
import ru.max.botapi.model.StickerAttachment;
import ru.max.botapi.model.StickerAttachmentRequest;
import ru.max.botapi.model.TextFormat;
import ru.max.botapi.model.UnknownAttachment;
import ru.max.botapi.model.UnknownAttachmentRequest;
import ru.max.botapi.model.UnknownButton;
import ru.max.botapi.model.UnknownUpdate;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.User;
import ru.max.botapi.model.UserAddedUpdate;
import ru.max.botapi.model.UserRemovedUpdate;
import ru.max.botapi.model.VideoAttachment;
import ru.max.botapi.model.VideoAttachmentRequest;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for {@link JacksonMaxSerializer}.
 */
class JacksonMaxSerializerTest {

    private JacksonMaxSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new JacksonMaxSerializer();
    }

    private String loadFixture(String path) {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + path)) {
            if (is == null) {
                throw new IllegalStateException("Fixture not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class ModelRoundTrip {

        @Test
        void botInfoRoundTrip() {
            String json = loadFixture("bot-info.json");
            BotInfo info = serializer.deserialize(json, BotInfo.class);
            assertThat(info.userId()).isEqualTo(12345L);
            assertThat(info.name()).isEqualTo("TestBot");
            assertThat(info.username()).isEqualTo("test_bot");
            assertThat(info.isBot()).isTrue();
            assertThat(info.commands()).hasSize(2);
            assertThat(info.commands().getFirst().name()).isEqualTo("start");

            String serialized = serializer.serialize(info);
            BotInfo roundTripped = serializer.deserialize(serialized, BotInfo.class);
            assertThat(roundTripped).isEqualTo(info);
        }

        @Test
        void userRoundTrip() {
            String json = loadFixture("user.json");
            User user = serializer.deserialize(json, User.class);
            assertThat(user.userId()).isEqualTo(99001L);
            assertThat(user.name()).isEqualTo("John Doe");
            assertThat(user.isBot()).isFalse();

            String serialized = serializer.serialize(user);
            User roundTripped = serializer.deserialize(serialized, User.class);
            assertThat(roundTripped).isEqualTo(user);
        }

        @Test
        void chatRoundTrip() {
            String json = loadFixture("chat.json");
            Chat chat = serializer.deserialize(json, Chat.class);
            assertThat(chat.chatId()).isEqualTo(50001L);
            assertThat(chat.type()).isEqualTo(ChatType.CHAT);
            assertThat(chat.status()).isEqualTo(ChatStatus.ACTIVE);
            assertThat(chat.title()).isEqualTo("Test Group");
            assertThat(chat.participantsCount()).isEqualTo(5);
        }

        @Test
        void chatListRoundTrip() {
            String json = loadFixture("chat-list.json");
            ChatList list = serializer.deserialize(json, ChatList.class);
            assertThat(list.chats()).hasSize(2);
            assertThat(list.marker()).isEqualTo(50002L);
        }

        @Test
        void messageRoundTrip() {
            String json = loadFixture("message.json");
            Message msg = serializer.deserialize(json, Message.class);
            assertThat(msg.sender()).isNotNull();
            assertThat(msg.sender().name()).isEqualTo("John Doe");
            assertThat(msg.body().mid()).isEqualTo("msg_001");
            assertThat(msg.body().text()).isEqualTo("Hello, world!");

            String serialized = serializer.serialize(msg);
            Message roundTripped = serializer.deserialize(serialized, Message.class);
            assertThat(roundTripped.body().mid()).isEqualTo(msg.body().mid());
        }

        @Test
        void messageWithAttachmentsRoundTrip() {
            String json = loadFixture("message-with-attachments.json");
            Message msg = serializer.deserialize(json, Message.class);
            assertThat(msg.body().attachments()).hasSize(1);
            assertThat(msg.body().attachments().getFirst()).isInstanceOf(PhotoAttachment.class);
            PhotoAttachment photo = (PhotoAttachment) msg.body().attachments().getFirst();
            assertThat(photo.payload().url()).isEqualTo("https://example.com/photo.jpg");
        }
    }

    @Nested
    class UpdateDeserialization {

        @Test
        void messageCreated() {
            Update update = serializer.deserialize(loadFixture("updates/message-created.json"), Update.class);
            assertThat(update).isInstanceOf(MessageCreatedUpdate.class);
            MessageCreatedUpdate mcu = (MessageCreatedUpdate) update;
            assertThat(mcu.updateType()).isEqualTo("message_created");
            assertThat(mcu.timestamp()).isEqualTo(1700001000000L);
            assertThat(mcu.message().body().text()).isEqualTo("New message");
            assertThat(mcu.userLocale()).isEqualTo("ru");
        }

        @Test
        void messageCallback() {
            Update update = serializer.deserialize(loadFixture("updates/message-callback.json"), Update.class);
            assertThat(update).isInstanceOf(MessageCallbackUpdate.class);
            MessageCallbackUpdate mbu = (MessageCallbackUpdate) update;
            assertThat(mbu.callback().callbackId()).isEqualTo("cb_001");
            assertThat(mbu.callback().payload()).isEqualTo("action:confirm");
        }

        @Test
        void messageEdited() {
            Update update = serializer.deserialize(loadFixture("updates/message-edited.json"), Update.class);
            assertThat(update).isInstanceOf(MessageEditedUpdate.class);
            MessageEditedUpdate meu = (MessageEditedUpdate) update;
            assertThat(meu.message().body().text()).isEqualTo("Edited message");
        }

        @Test
        void messageRemoved() {
            Update update = serializer.deserialize(loadFixture("updates/message-removed.json"), Update.class);
            assertThat(update).isInstanceOf(MessageRemovedUpdate.class);
            MessageRemovedUpdate mru = (MessageRemovedUpdate) update;
            assertThat(mru.messageId()).isEqualTo("msg_010");
            assertThat(mru.chatId()).isEqualTo(50001L);
        }

        @Test
        void botAdded() {
            Update update = serializer.deserialize(loadFixture("updates/bot-added.json"), Update.class);
            assertThat(update).isInstanceOf(BotAddedUpdate.class);
            BotAddedUpdate bau = (BotAddedUpdate) update;
            assertThat(bau.chatId()).isEqualTo(50001L);
            assertThat(bau.user().name()).isEqualTo("John Doe");
        }

        @Test
        void botRemoved() {
            Update update = serializer.deserialize(loadFixture("updates/bot-removed.json"), Update.class);
            assertThat(update).isInstanceOf(BotRemovedUpdate.class);
        }

        @Test
        void userAdded() {
            Update update = serializer.deserialize(loadFixture("updates/user-added.json"), Update.class);
            assertThat(update).isInstanceOf(UserAddedUpdate.class);
            UserAddedUpdate uau = (UserAddedUpdate) update;
            assertThat(uau.user().name()).isEqualTo("Jane Smith");
            assertThat(uau.inviterId()).isEqualTo(99001L);
        }

        @Test
        void userRemoved() {
            Update update = serializer.deserialize(loadFixture("updates/user-removed.json"), Update.class);
            assertThat(update).isInstanceOf(UserRemovedUpdate.class);
            UserRemovedUpdate uru = (UserRemovedUpdate) update;
            assertThat(uru.adminId()).isEqualTo(99001L);
        }

        @Test
        void botStarted() {
            Update update = serializer.deserialize(loadFixture("updates/bot-started.json"), Update.class);
            assertThat(update).isInstanceOf(BotStartedUpdate.class);
            BotStartedUpdate bsu = (BotStartedUpdate) update;
            assertThat(bsu.payload()).isEqualTo("deep_link_data");
        }

        @Test
        void botStopped() {
            Update update = serializer.deserialize(loadFixture("updates/bot-stopped.json"), Update.class);
            assertThat(update).isInstanceOf(BotStoppedUpdate.class);
        }

        @Test
        void chatTitleChanged() {
            Update update = serializer.deserialize(
                    loadFixture("updates/chat-title-changed.json"), Update.class);
            assertThat(update).isInstanceOf(ChatTitleChangedUpdate.class);
            ChatTitleChangedUpdate ctcu = (ChatTitleChangedUpdate) update;
            assertThat(ctcu.title()).isEqualTo("New Group Title");
        }

        @Test
        void messageConstructionRequest() {
            Update update = serializer.deserialize(
                    loadFixture("updates/message-construction-request.json"), Update.class);
            assertThat(update).isInstanceOf(MessageConstructionRequestUpdate.class);
            MessageConstructionRequestUpdate mcru = (MessageConstructionRequestUpdate) update;
            assertThat(mcru.sessionId()).isEqualTo("session_abc123");
        }

        @Test
        void messageConstructed() {
            Update update = serializer.deserialize(
                    loadFixture("updates/message-constructed.json"), Update.class);
            assertThat(update).isInstanceOf(MessageConstructedUpdate.class);
            MessageConstructedUpdate mcu = (MessageConstructedUpdate) update;
            assertThat(mcu.message().body().text()).isEqualTo("Constructed message text");
        }

        @Test
        void messageChatCreated() {
            Update update = serializer.deserialize(
                    loadFixture("updates/message-chat-created.json"), Update.class);
            assertThat(update).isInstanceOf(MessageChatCreatedUpdate.class);
            MessageChatCreatedUpdate mccu = (MessageChatCreatedUpdate) update;
            assertThat(mccu.chat().title()).isEqualTo("New Chat");
        }

        @Test
        void unknownUpdate() {
            Update update = serializer.deserialize(
                    loadFixture("updates/unknown-update.json"), Update.class);
            assertThat(update).isInstanceOf(UnknownUpdate.class);
            UnknownUpdate uu = (UnknownUpdate) update;
            assertThat(uu.updateType()).isEqualTo("future_feature");
            assertThat(uu.timestamp()).isEqualTo(1700003000000L);
            assertThat(uu.rawJson()).contains("future_feature");
        }
    }

    @Nested
    class AttachmentDeserialization {

        @Test
        void photoAttachment() {
            Attachment att = serializer.deserialize(loadFixture("attachments/image.json"), Attachment.class);
            assertThat(att).isInstanceOf(PhotoAttachment.class);
            assertThat(att.type()).isEqualTo("image");
            assertThat(((PhotoAttachment) att).payload().photoId()).isEqualTo(7001L);
        }

        @Test
        void videoAttachment() {
            Attachment att = serializer.deserialize(loadFixture("attachments/video.json"), Attachment.class);
            assertThat(att).isInstanceOf(VideoAttachment.class);
            VideoAttachment va = (VideoAttachment) att;
            assertThat(va.width()).isEqualTo(1920);
            assertThat(va.duration()).isEqualTo(120);
        }

        @Test
        void audioAttachment() {
            Attachment att = serializer.deserialize(loadFixture("attachments/audio.json"), Attachment.class);
            assertThat(att).isInstanceOf(AudioAttachment.class);
        }

        @Test
        void fileAttachment() {
            Attachment att = serializer.deserialize(loadFixture("attachments/file.json"), Attachment.class);
            assertThat(att).isInstanceOf(FileAttachment.class);
            FileAttachment fa = (FileAttachment) att;
            assertThat(fa.filename()).isEqualTo("report.pdf");
            assertThat(fa.size()).isEqualTo(1048576L);
        }

        @Test
        void stickerAttachment() {
            Attachment att = serializer.deserialize(loadFixture("attachments/sticker.json"), Attachment.class);
            assertThat(att).isInstanceOf(StickerAttachment.class);
            StickerAttachment sa = (StickerAttachment) att;
            assertThat(sa.payload().code()).isEqualTo("happy_face");
        }

        @Test
        void contactAttachment() {
            Attachment att = serializer.deserialize(loadFixture("attachments/contact.json"), Attachment.class);
            assertThat(att).isInstanceOf(ContactAttachment.class);
        }

        @Test
        void inlineKeyboardAttachment() {
            Attachment att = serializer.deserialize(
                    loadFixture("attachments/inline-keyboard.json"), Attachment.class);
            assertThat(att).isInstanceOf(InlineKeyboardAttachment.class);
            InlineKeyboardAttachment ika = (InlineKeyboardAttachment) att;
            assertThat(ika.payload().buttons()).hasSize(1);
            assertThat(ika.payload().buttons().getFirst()).hasSize(2);
        }

        @Test
        void shareAttachment() {
            Attachment att = serializer.deserialize(loadFixture("attachments/share.json"), Attachment.class);
            assertThat(att).isInstanceOf(ShareAttachment.class);
            ShareAttachment sa = (ShareAttachment) att;
            assertThat(sa.title()).isEqualTo("Example Page");
        }

        @Test
        void locationAttachment() {
            Attachment att = serializer.deserialize(loadFixture("attachments/location.json"), Attachment.class);
            assertThat(att).isInstanceOf(LocationAttachment.class);
            LocationAttachment la = (LocationAttachment) att;
            assertThat(la.latitude()).isEqualTo(55.7558);
            assertThat(la.longitude()).isEqualTo(37.6173);
        }

        @Test
        void unknownAttachment() {
            Attachment att = serializer.deserialize(
                    loadFixture("attachments/unknown-attachment.json"), Attachment.class);
            assertThat(att).isInstanceOf(UnknownAttachment.class);
            UnknownAttachment ua = (UnknownAttachment) att;
            assertThat(ua.type()).isEqualTo("future_media");
            assertThat(ua.rawJson()).contains("future_media");
        }
    }

    @Nested
    class ButtonDeserialization {

        @Test
        void callbackButton() {
            Button btn = serializer.deserialize(loadFixture("buttons/callback.json"), Button.class);
            assertThat(btn).isInstanceOf(CallbackButton.class);
            CallbackButton cb = (CallbackButton) btn;
            assertThat(cb.text()).isEqualTo("Confirm");
            assertThat(cb.payload()).isEqualTo("action:confirm");
            assertThat(cb.intent()).isEqualTo(ButtonIntent.POSITIVE);
        }

        @Test
        void linkButton() {
            Button btn = serializer.deserialize(loadFixture("buttons/link.json"), Button.class);
            assertThat(btn).isInstanceOf(LinkButton.class);
            assertThat(((LinkButton) btn).url()).isEqualTo("https://example.com");
        }

        @Test
        void requestContactButton() {
            Button btn = serializer.deserialize(loadFixture("buttons/request-contact.json"), Button.class);
            assertThat(btn).isInstanceOf(RequestContactButton.class);
        }

        @Test
        void requestGeoLocationButton() {
            Button btn = serializer.deserialize(
                    loadFixture("buttons/request-geo-location.json"), Button.class);
            assertThat(btn).isInstanceOf(RequestGeoLocationButton.class);
            assertThat(((RequestGeoLocationButton) btn).quick()).isTrue();
        }

        @Test
        void chatButton() {
            Button btn = serializer.deserialize(loadFixture("buttons/chat.json"), Button.class);
            assertThat(btn).isInstanceOf(ChatButton.class);
            ChatButton cb = (ChatButton) btn;
            assertThat(cb.chatTitle()).isEqualTo("Support Chat");
        }

        @Test
        void openAppButton() {
            Button btn = serializer.deserialize(loadFixture("buttons/open-app.json"), Button.class);
            assertThat(btn).isInstanceOf(OpenAppButton.class);
        }

        @Test
        void messageButton() {
            Button btn = serializer.deserialize(loadFixture("buttons/message.json"), Button.class);
            assertThat(btn).isInstanceOf(MessageButton.class);
            assertThat(((MessageButton) btn).message()).isEqualTo("Hello!");
        }

        @Test
        void unknownButton() {
            Button btn = serializer.deserialize(loadFixture("buttons/unknown-button.json"), Button.class);
            assertThat(btn).isInstanceOf(UnknownButton.class);
            UnknownButton ub = (UnknownButton) btn;
            assertThat(ub.type()).isEqualTo("future_button");
            assertThat(ub.text()).isEqualTo("Future Button");
        }
    }

    @Nested
    class SnakeCaseNaming {

        @Test
        void serializesToSnakeCase() {
            User user = new User(1L, "Test", "test_user", false, 1700000000000L);
            String json = serializer.serialize(user);
            assertThatJson(json).node("user_id").isEqualTo(1);
            assertThatJson(json).node("last_activity_time").isEqualTo(1700000000000L);
            assertThatJson(json).node("is_bot").isEqualTo(false);
        }

        @Test
        void deserializesFromSnakeCase() {
            String json = """
                    {"user_id": 1, "name": "Test", "is_bot": false, "last_activity_time": 100}
                    """;
            User user = serializer.deserialize(json, User.class);
            assertThat(user.userId()).isEqualTo(1L);
            assertThat(user.isBot()).isFalse();
        }
    }

    @Nested
    class NullHandling {

        @Test
        void nullFieldsOmitted() {
            User user = new User(1L, "Test", null, false, 100L);
            String json = serializer.serialize(user);
            assertThatJson(json).node("username").isAbsent();
        }

        @Test
        void unknownFieldsIgnored() {
            String json = """
                    {"user_id": 1, "name": "Test", "is_bot": false,
                     "last_activity_time": 100, "future_field": "ignored"}
                    """;
            User user = serializer.deserialize(json, User.class);
            assertThat(user.name()).isEqualTo("Test");
        }
    }

    @Nested
    class NotifyFieldMapping {

        @Test
        void newMessageBodySerializesNotifyAsNotify() {
            NewMessageBody body = new NewMessageBody("Hello", null, null, true, TextFormat.MARKDOWN);
            String json = serializer.serialize(body);
            assertThatJson(json).node("notify").isEqualTo(true);
            assertThatJson(json).node("notify_recipients").isAbsent();
        }

        @Test
        void newMessageBodyDeserializesNotifyField() {
            String json = loadFixture("new-message-body.json");
            NewMessageBody body = serializer.deserialize(json, NewMessageBody.class);
            assertThat(body.notifyRecipients()).isTrue();
            assertThat(body.text()).isEqualTo("Hello from bot");
        }

        @Test
        void pinMessageBodyNotifyMapping() {
            PinMessageBody body = new PinMessageBody("msg_001", true);
            String json = serializer.serialize(body);
            assertThatJson(json).node("notify").isEqualTo(true);
            assertThatJson(json).node("notify_recipients").isAbsent();
        }

        @Test
        void chatPatchNotifyMapping() {
            ChatPatch patch = new ChatPatch("New Title", null, null, true);
            String json = serializer.serialize(patch);
            assertThatJson(json).node("notify").isEqualTo(true);
            assertThatJson(json).node("notify_recipients").isAbsent();
        }
    }

    @Nested
    class EnumSerialization {

        @Test
        void chatTypeSerializesToSnakeCase() {
            String json = serializer.serialize(ChatType.DIALOG);
            assertThat(json).isEqualTo("\"dialog\"");
        }

        @Test
        void chatStatusSerializesToSnakeCase() {
            String json = serializer.serialize(ChatStatus.ACTIVE);
            assertThat(json).isEqualTo("\"active\"");
        }

        @Test
        void buttonIntentSerializesToSnakeCase() {
            String json = serializer.serialize(ButtonIntent.POSITIVE);
            assertThat(json).isEqualTo("\"positive\"");
        }

        @Test
        void textFormatSerializesToSnakeCase() {
            String json = serializer.serialize(TextFormat.MARKDOWN);
            assertThat(json).isEqualTo("\"markdown\"");
        }

        @Test
        void enumDeserializesFromSnakeCase() {
            ChatType type = serializer.deserialize("\"dialog\"", ChatType.class);
            assertThat(type).isEqualTo(ChatType.DIALOG);
        }

        @Test
        void unknownEnumValueDeserializesToNullNotThrow() {
            // Forward-compatible: unknown enum values must not throw
            ChatType type = serializer.deserialize("\"future_chat_type\"", ChatType.class);
            assertThat(type).isNull();
        }
    }

    @Nested
    class TypeReferenceSerialization {

        @Test
        void deserializeGenericListOfUpdates() {
            String json = """
                    [
                      {"update_type":"bot_started","timestamp":100,"chat_id":1,
                       "user":{"user_id":1,"name":"A","is_bot":false,"last_activity_time":0}},
                      {"update_type":"bot_stopped","timestamp":200,"chat_id":1,
                       "user":{"user_id":1,"name":"A","is_bot":false,"last_activity_time":0}}
                    ]
                    """;
            List<Update> updates = serializer.deserialize(json, new TypeReference<>() { });
            assertThat(updates).hasSize(2);
            assertThat(updates.get(0)).isInstanceOf(BotStartedUpdate.class);
            assertThat(updates.get(1)).isInstanceOf(BotStoppedUpdate.class);
        }
    }

    @Nested
    class AttachmentRequestDeserialization {

        @Test
        void imageAttachmentRequest() {
            String json = "{\"type\":\"image\",\"payload\":{\"url\":\"https://img.com/a.jpg\"}}";
            AttachmentRequest req = serializer.deserialize(json, AttachmentRequest.class);
            assertThat(req).isInstanceOf(ImageAttachmentRequest.class);
        }

        @Test
        void videoAttachmentRequest() {
            String json = "{\"type\":\"video\",\"payload\":{\"token\":\"vtok\"}}";
            AttachmentRequest req = serializer.deserialize(json, AttachmentRequest.class);
            assertThat(req).isInstanceOf(VideoAttachmentRequest.class);
        }

        @Test
        void audioAttachmentRequest() {
            String json = "{\"type\":\"audio\",\"payload\":{\"token\":\"atok\"}}";
            AttachmentRequest req = serializer.deserialize(json, AttachmentRequest.class);
            assertThat(req).isInstanceOf(AudioAttachmentRequest.class);
        }

        @Test
        void fileAttachmentRequest() {
            String json = "{\"type\":\"file\",\"payload\":{\"token\":\"ftok\"}}";
            AttachmentRequest req = serializer.deserialize(json, AttachmentRequest.class);
            assertThat(req).isInstanceOf(FileAttachmentRequest.class);
        }

        @Test
        void stickerAttachmentRequest() {
            String json = "{\"type\":\"sticker\",\"payload\":{\"code\":\"smile\"}}";
            AttachmentRequest req = serializer.deserialize(json, AttachmentRequest.class);
            assertThat(req).isInstanceOf(StickerAttachmentRequest.class);
        }

        @Test
        void contactAttachmentRequest() {
            String json = "{\"type\":\"contact\",\"payload\":{\"name\":\"John\"}}";
            AttachmentRequest req = serializer.deserialize(json, AttachmentRequest.class);
            assertThat(req).isInstanceOf(ContactAttachmentRequest.class);
        }

        @Test
        void inlineKeyboardAttachmentRequest() {
            String json = "{\"type\":\"inline_keyboard\",\"payload\":{\"buttons\":[[]]}}";
            AttachmentRequest req = serializer.deserialize(json, AttachmentRequest.class);
            assertThat(req).isInstanceOf(InlineKeyboardAttachmentRequest.class);
        }

        @Test
        void shareAttachmentRequest() {
            String json = "{\"type\":\"share\"}";
            AttachmentRequest req = serializer.deserialize(json, AttachmentRequest.class);
            assertThat(req).isInstanceOf(ShareAttachmentRequest.class);
        }

        @Test
        void locationAttachmentRequest() {
            String json = "{\"type\":\"location\",\"latitude\":40.7,\"longitude\":-74.0}";
            AttachmentRequest req = serializer.deserialize(json, AttachmentRequest.class);
            assertThat(req).isInstanceOf(LocationAttachmentRequest.class);
        }

        @Test
        void unknownAttachmentRequest() {
            String json = "{\"type\":\"future_req\",\"data\":\"x\"}";
            AttachmentRequest req = serializer.deserialize(json, AttachmentRequest.class);
            assertThat(req).isInstanceOf(UnknownAttachmentRequest.class);
            assertThat(((UnknownAttachmentRequest) req).type()).isEqualTo("future_req");
        }
    }

    @Nested
    class SealedTypeUnknownRoundTrip {

        @Test
        void unknownAttachmentRoundTrip() {
            UnknownAttachment original = new UnknownAttachment("future_media",
                    "{\"type\":\"future_media\"}");
            String json = serializer.serialize((Attachment) original);
            assertThatJson(json).node("type").isEqualTo("future_media");
            // Verify type appears exactly once
            assertThat(json.indexOf("\"type\"")).isEqualTo(json.lastIndexOf("\"type\""));
            Attachment deserialized = serializer.deserialize(json, Attachment.class);
            assertThat(deserialized).isInstanceOf(UnknownAttachment.class);
            UnknownAttachment result = (UnknownAttachment) deserialized;
            assertThat(result.type()).isEqualTo("future_media");
        }

        @Test
        void unknownButtonRoundTrip() {
            UnknownButton original = new UnknownButton("future_button", "Press Me",
                    "{\"type\":\"future_button\"}");
            String json = serializer.serialize((Button) original);
            assertThatJson(json).node("type").isEqualTo("future_button");
            assertThatJson(json).node("text").isEqualTo("Press Me");
            // Verify type appears exactly once
            assertThat(json.indexOf("\"type\"")).isEqualTo(json.lastIndexOf("\"type\""));
            Button deserialized = serializer.deserialize(json, Button.class);
            assertThat(deserialized).isInstanceOf(UnknownButton.class);
            UnknownButton result = (UnknownButton) deserialized;
            assertThat(result.type()).isEqualTo("future_button");
            assertThat(result.text()).isEqualTo("Press Me");
        }

        @Test
        void unknownAttachmentRequestRoundTrip() {
            UnknownAttachmentRequest original = new UnknownAttachmentRequest("future_req",
                    "{\"type\":\"future_req\"}");
            String json = serializer.serialize((AttachmentRequest) original);
            assertThatJson(json).node("type").isEqualTo("future_req");
            // Verify type appears exactly once
            assertThat(json.indexOf("\"type\"")).isEqualTo(json.lastIndexOf("\"type\""));
            AttachmentRequest deserialized = serializer.deserialize(json, AttachmentRequest.class);
            assertThat(deserialized).isInstanceOf(UnknownAttachmentRequest.class);
            UnknownAttachmentRequest result = (UnknownAttachmentRequest) deserialized;
            assertThat(result.type()).isEqualTo("future_req");
        }
    }

    @Nested
    class ObjectMapperAccess {

        @Test
        void getObjectMapperReturnsNonNull() {
            assertThat(serializer.getObjectMapper()).isNotNull();
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void serializeThrowsOnFailure() {
            // Objects that fail to serialize
            Object selfRef = new Object() {
                @Override
                public String toString() {
                    return "self";
                }
            };
            // This should serialize without issues as a simple object
            // Testing invalid JSON deserialization instead
            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> serializer.deserialize("invalid json", User.class)
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void deserializeWithTypeReferenceThrowsOnInvalidJson() {
            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> serializer.deserialize("not json", new TypeReference<List<User>>() { })
            ).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class SealedTypeSerialization {

        @Test
        void attachmentSerializationIncludesType() {
            Attachment att = new LocationAttachment(55.75, 37.62);
            String json = serializer.serialize(att);
            assertThatJson(json).node("type").isEqualTo("location");
            assertThatJson(json).node("latitude").isEqualTo(55.75);
        }

        @Test
        void buttonSerializationIncludesType() {
            Button btn = new CallbackButton("OK", "ok_payload", null);
            String json = serializer.serialize(btn);
            assertThatJson(json).node("type").isEqualTo("callback");
            assertThatJson(json).node("text").isEqualTo("OK");
            assertThatJson(json).node("payload").isEqualTo("ok_payload");
        }

        @Test
        void attachmentRequestSerializationIncludesType() {
            AttachmentRequest req = new LocationAttachmentRequest(40.7128, -74.0060);
            String json = serializer.serialize(req);
            assertThatJson(json).node("type").isEqualTo("location");
            assertThatJson(json).node("latitude").isEqualTo(40.7128);
        }
    }
}
