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

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.core.TypeReference;
import ru.max.botapi.jackson.JacksonMaxSerializer;
import ru.max.botapi.model.ActionRequestBody;
import ru.max.botapi.model.Attachment;
import ru.max.botapi.model.AttachmentRequest;
import ru.max.botapi.model.BotInfo;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.CallbackButton;
import ru.max.botapi.model.Chat;
import ru.max.botapi.model.ChatMember;
import ru.max.botapi.model.ChatPatch;
import ru.max.botapi.model.FileUploadedInfo;
import ru.max.botapi.model.InlineKeyboardAttachment;
import ru.max.botapi.model.InlineKeyboardAttachmentRequest;
import ru.max.botapi.model.LocationAttachmentRequest;
import ru.max.botapi.model.Message;
import ru.max.botapi.model.MessageList;
import ru.max.botapi.model.MessageStat;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.PhotoAttachmentRequestPayload;
import ru.max.botapi.model.PinMessageBody;
import ru.max.botapi.model.SenderAction;
import ru.max.botapi.model.TextFormat;
import ru.max.botapi.model.UnknownButton;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateList;
import ru.max.botapi.model.User;
import ru.max.botapi.model.UserIdsList;
import ru.max.botapi.model.VideoAttachmentDetails;
import ru.max.botapi.model.VideoUrls;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

import static ru.max.botapi.testsupport.FixtureLoader.loadFixture;

/**
 * Pins {@link GsonMaxSerializer} to the behaviour of {@link JacksonMaxSerializer}: the same
 * JSON must deserialize to equal models (or fail in both), and the same models must serialize
 * to equivalent JSON.
 */
class SerializerParityTest {

    private static final MaxSerializer JACKSON = new JacksonMaxSerializer();
    private static final MaxSerializer GSON = new GsonMaxSerializer();

    static Stream<Arguments> fixtures() {
        return Stream.of(
                fixture("bot-info.json", BotInfo.class),
                fixture("user.json", User.class),
                fixture("chat.json", Chat.class),
                fixture("message.json", Message.class),
                fixture("message-with-attachments.json", Message.class),
                fixture("new-message-body.json", NewMessageBody.class),
                fixture("attachments/audio.json", Attachment.class),
                fixture("attachments/contact.json", Attachment.class),
                fixture("attachments/file.json", Attachment.class),
                fixture("attachments/image.json", Attachment.class),
                fixture("attachments/inline-keyboard.json", Attachment.class),
                fixture("attachments/location.json", Attachment.class),
                fixture("attachments/share.json", Attachment.class),
                fixture("attachments/sticker.json", Attachment.class),
                fixture("attachments/unknown-attachment.json", Attachment.class),
                fixture("attachments/video.json", Attachment.class),
                fixture("buttons/callback.json", Button.class),
                fixture("buttons/chat.json", Button.class),
                fixture("buttons/link.json", Button.class),
                fixture("buttons/message.json", Button.class),
                fixture("buttons/open-app.json", Button.class),
                fixture("buttons/request-contact.json", Button.class),
                fixture("buttons/request-geo-location.json", Button.class),
                fixture("buttons/unknown-button.json", Button.class),
                fixture("updates/bot-added.json", Update.class),
                fixture("updates/bot-removed.json", Update.class),
                fixture("updates/bot-started.json", Update.class),
                fixture("updates/bot-stopped.json", Update.class),
                fixture("updates/chat-title-changed.json", Update.class),
                fixture("updates/message-callback.json", Update.class),
                fixture("updates/message-chat-created.json", Update.class),
                fixture("updates/message-constructed.json", Update.class),
                fixture("updates/message-construction-request.json", Update.class),
                fixture("updates/message-created.json", Update.class),
                fixture("updates/message-edited.json", Update.class),
                fixture("updates/message-removed.json", Update.class),
                fixture("updates/unknown-update.json", Update.class),
                fixture("updates/user-added.json", Update.class),
                fixture("updates/user-removed.json", Update.class));
    }

    /** Inputs Jackson reads successfully; Gson must read them to the same value. */
    static Stream<Arguments> coercions() {
        String user = "{\"user_id\":%s,\"first_name\":\"A\"}";
        String userWith = "{\"user_id\":1,\"first_name\":\"A\",%s}";
        String location = "{\"type\":\"location\",\"latitude\":%s,\"longitude\":0}";
        String chat = "{\"chat_id\":1,\"type\":\"chat\",\"status\":\"active\",%s}";
        return Stream.of(
                // integers
                Arguments.of(user.formatted("1.5"), User.class),
                Arguments.of(user.formatted("-1.5"), User.class),
                Arguments.of(user.formatted("1e2"), User.class),
                Arguments.of(user.formatted("\"\""), User.class),
                Arguments.of(user.formatted("\" \""), User.class),
                Arguments.of(user.formatted("\" 42 \""), User.class),
                Arguments.of(user.formatted("\"+42\""), User.class),
                Arguments.of(userWith.formatted("\"last_activity_time\":\"\""), User.class),
                Arguments.of(userWith.formatted("\"last_activity_time\":2.9"), User.class),
                Arguments.of("{\"views\":1.5}", MessageStat.class),
                Arguments.of("{\"views\":\"\"}", MessageStat.class),
                Arguments.of("{\"token\":\"t\",\"width\":\"\",\"height\":7.9}", VideoAttachmentDetails.class),
                Arguments.of("{\"user_ids\":[1.5,\"7\",3]}", UserIdsList.class),
                Arguments.of(chat.formatted("\"participants_count\":2.5,\"messages_count\":\"\","
                        + "\"participants\":{\"a\":1.5,\"b\":\"3\"}"), Chat.class),
                // booleans
                Arguments.of(userWith.formatted("\"is_bot\":0"), User.class),
                Arguments.of(userWith.formatted("\"is_bot\":1"), User.class),
                Arguments.of(userWith.formatted("\"is_bot\":2"), User.class),
                Arguments.of(userWith.formatted("\"is_bot\":-1"), User.class),
                Arguments.of(userWith.formatted("\"is_bot\":9999999999999"), User.class),
                Arguments.of(userWith.formatted("\"is_bot\":\"True\""), User.class),
                Arguments.of(userWith.formatted("\"is_bot\":\"TRUE\""), User.class),
                Arguments.of(userWith.formatted("\"is_bot\":\" false \""), User.class),
                Arguments.of(userWith.formatted("\"is_bot\":\"\""), User.class),
                Arguments.of("{\"text\":\"t\",\"notify\":\"\"}", NewMessageBody.class),
                // doubles
                Arguments.of(location.formatted("\"NaN\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"Infinity\""), AttachmentRequest.class),
                Arguments.of(location.formatted("1e400"), AttachmentRequest.class),
                Arguments.of(location.formatted("\"-Infinity\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"+Infinity\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"INF\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"-INF\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"1e+5\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"+.5\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"-5.\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\" 1e2 \""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"0001.5\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"1.25\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"\""), AttachmentRequest.class),
                // discriminators and fallback fields
                Arguments.of("{\"type\":null,\"text\":\"t\"}", Button.class),
                Arguments.of("{\"type\":{\"a\":1},\"text\":\"t\"}", Button.class),
                Arguments.of("{\"type\":[],\"text\":\"t\"}", Button.class),
                Arguments.of("{\"type\":\"teleport\",\"text\":{\"a\":1}}", Button.class),
                Arguments.of("{\"type\":null}", Attachment.class),
                Arguments.of("{\"type\":{}}", Attachment.class),
                Arguments.of("{\"type\":null}", AttachmentRequest.class),
                Arguments.of("{\"type\":[1]}", AttachmentRequest.class),
                Arguments.of("{\"update_type\":null,\"timestamp\":1}", Update.class),
                Arguments.of("{\"update_type\":{},\"timestamp\":1}", Update.class),
                Arguments.of("{\"update_type\":\"warp\",\"timestamp\":\"abc\"}", Update.class),
                Arguments.of("{\"update_type\":\"warp\",\"timestamp\":2.5}", Update.class),
                Arguments.of("{\"updates\":[null,{\"update_type\":\"bot_started\"}],\"marker\":9}",
                        UpdateList.class));
    }

    static Stream<Arguments> edgeCases() {
        String location = "{\"type\":\"location\",\"latitude\":%s,\"longitude\":0}";
        return Stream.of(
                // documents
                Arguments.of("null", User.class),
                Arguments.of("", User.class),
                Arguments.of("   ", User.class),
                Arguments.of("{", User.class),
                Arguments.of("[]", User.class),
                Arguments.of("{\"user_id\":1,\"first_name\":\"A\"} trailing", User.class),
                Arguments.of("{\"user_id\":1,\"first_name\":\"A\",\"extra\":{\"x\":[1]}}", User.class),
                // primitives and coercions
                Arguments.of("{\"user_id\":null,\"first_name\":\"A\",\"is_bot\":null}", User.class),
                Arguments.of("{\"user_id\":\"42\",\"first_name\":\"A\"}", User.class),
                Arguments.of("{\"user_id\":1,\"first_name\":123}", User.class),
                Arguments.of("{\"user_id\":1,\"first_name\":\"A\",\"is_bot\":\"true\"}", User.class),
                Arguments.of("{\"user_id\":1}", User.class),
                // enums
                Arguments.of("{\"action\":\"TYPING_ON\"}", ActionRequestBody.class),
                Arguments.of("{\"action\":\"typing_on\"}", ActionRequestBody.class),
                Arguments.of("{\"action\":\"teleport\"}", ActionRequestBody.class),
                Arguments.of("{\"action\":\"\"}", ActionRequestBody.class),
                Arguments.of("{\"text\":\"t\",\"format\":\"Html\"}", NewMessageBody.class),
                Arguments.of("{\"text\":\"t\",\"format\":\"rtf\"}", NewMessageBody.class),
                Arguments.of("{\"user_id\":1,\"permissions\":[\"write\",\"teleport\",\"edit_message\"]}",
                        ChatMember.class),
                // naming exceptions
                Arguments.of("{\"text\":\"t\",\"notify\":false}", NewMessageBody.class),
                Arguments.of("{\"message_id\":\"m\",\"notify\":true}", PinMessageBody.class),
                Arguments.of("{\"fileId\":7,\"token\":\"t\"}", FileUploadedInfo.class),
                Arguments.of("{\"file_id\":7,\"token\":\"t\"}", FileUploadedInfo.class),
                Arguments.of("{\"token\":\"t\",\"urls\":{\"mp4_1080\":\"a\",\"mp4_144\":\"b\",\"hls\":\"c\"}}",
                        VideoAttachmentDetails.class),
                // sealed hierarchies
                Arguments.of("{\"type\":\"callback\",\"text\":\"t\"}", Button.class),
                Arguments.of("{\"type\":\"teleport\",\"text\":\"t\",\"x\":1}", Button.class),
                Arguments.of("{\"text\":\"t\"}", Button.class),
                Arguments.of("{\"type\":7,\"text\":5}", Button.class),
                Arguments.of("\"callback\"", Button.class),
                Arguments.of("{\"type\":\"audio\"}", Attachment.class),
                Arguments.of("{\"type\":\"hologram\",\"payload\":{}}", Attachment.class),
                Arguments.of("{\"type\":\"image\"}", AttachmentRequest.class),
                Arguments.of("{\"type\":\"hologram\"}", AttachmentRequest.class),
                Arguments.of("{\"update_type\":\"message_created\",\"timestamp\":5}", Update.class),
                Arguments.of("{\"update_type\":\"message_created\",\"timestamp\":\"5\"}", Update.class),
                Arguments.of("{\"update_type\":\"warp\",\"timestamp\":\"7\"}", Update.class),
                Arguments.of("{\"timestamp\":1}", Update.class),
                Arguments.of("[1]", Update.class),
                // coercions Jackson may reject; the outcome must match either way
                Arguments.of("{\"user_id\":1e30,\"first_name\":\"A\"}", User.class),
                Arguments.of("{\"user_id\":\"1.5\",\"first_name\":\"A\"}", User.class),
                Arguments.of("{\"user_id\":\"1e2\",\"first_name\":\"A\"}", User.class),
                Arguments.of("{\"user_id\":\"0x10\",\"first_name\":\"A\"}", User.class),
                Arguments.of("{\"user_id\":true,\"first_name\":\"A\"}", User.class),
                Arguments.of("{\"user_id\":{},\"first_name\":\"A\"}", User.class),
                Arguments.of("{\"user_id\":[],\"first_name\":\"A\"}", User.class),
                Arguments.of("{\"views\":3000000000}", MessageStat.class),
                Arguments.of("{\"views\":\"3000000000\"}", MessageStat.class),
                Arguments.of("{\"user_id\":1,\"first_name\":\"A\",\"is_bot\":1.5}", User.class),
                Arguments.of("{\"user_id\":1,\"first_name\":\"A\",\"is_bot\":0.0}", User.class),
                Arguments.of("{\"user_id\":1,\"first_name\":\"A\",\"is_bot\":\"yes\"}", User.class),
                Arguments.of("{\"user_id\":1,\"first_name\":\"A\",\"is_bot\":\"1\"}", User.class),
                Arguments.of("{\"user_id\":1,\"first_name\":\"A\",\"is_bot\":\"tRuE\"}", User.class),
                Arguments.of("{\"user_id\":1,\"first_name\":\"A\",\"is_bot\":[]}", User.class),
                Arguments.of("{\"type\":\"location\",\"latitude\":\"nan\",\"longitude\":0}",
                        AttachmentRequest.class),
                Arguments.of("{\"type\":\"location\",\"latitude\":true,\"longitude\":0}",
                        AttachmentRequest.class),
                Arguments.of("{\"user_ids\":[\"\"]}", UserIdsList.class),
                Arguments.of("{\"user_ids\":[null]}", UserIdsList.class),
                Arguments.of("{\"chat_id\":1,\"type\":\"chat\",\"status\":\"active\","
                        + "\"participants\":{\"a\":\"\"}}", Chat.class),
                Arguments.of(location.formatted("\" -Infinity \""), AttachmentRequest.class),
                Arguments.of(location.formatted("\" NaN \""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"0x1p3\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"1d\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"1_000\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"inf\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"NAN\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"+NaN\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"\\u00a01.5\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\"1.5e\""), AttachmentRequest.class),
                Arguments.of(location.formatted("\".\""), AttachmentRequest.class),
                Arguments.of(location.formatted("1."), AttachmentRequest.class),
                Arguments.of(location.formatted("\"" + "1".repeat(1001) + "\""), AttachmentRequest.class),
                Arguments.of(location.formatted("1".repeat(1001)), AttachmentRequest.class),
                Arguments.of(location.formatted("\"" + "1".repeat(1000) + "\""), AttachmentRequest.class),
                Arguments.of("{\"update_type\":\"warp\",\"timestamp\":true}", Update.class),
                Arguments.of("{\"update_type\":\"warp\",\"timestamp\":{}}", Update.class),
                // update batches
                Arguments.of("{\"updates\":[{\"update_type\":\"bot_started\"},7,{\"update_type\":\"x\"}],"
                        + "\"marker\":9}", UpdateList.class),
                Arguments.of("{\"updates\":{},\"marker\":9}", UpdateList.class),
                Arguments.of("{\"marker\":\"9\"}", UpdateList.class),
                Arguments.of("{}", UpdateList.class),
                Arguments.of("[]", UpdateList.class));
    }

    static Stream<Object> models() {
        var keyboard = new InlineKeyboardAttachment.KeyboardPayload(List.of(List.of(
                new CallbackButton("Yes", "yes", null),
                new UnknownButton("teleport", "Go", "{}"))));
        return Stream.of(
                new NewMessageBody("<b>hi</b> & 'bye'", List.of(new InlineKeyboardAttachmentRequest(keyboard)),
                        null, true, TextFormat.HTML),
                new PinMessageBody("mid", false),
                new ChatPatch("title", null, new PhotoAttachmentRequestPayload(null, "https://x", null), null, true),
                new FileUploadedInfo(5L, "tok"),
                new VideoAttachmentDetails("tok", new VideoUrls("a", "b", null, null, null, "f", "g"),
                        null, 1, 2, null),
                new ActionRequestBody(SenderAction.TYPING_ON),
                new LocationAttachmentRequest(Double.NaN, Double.NEGATIVE_INFINITY),
                new LocationAttachmentRequest(Double.POSITIVE_INFINITY, 1.5),
                new CallbackButton("Yes", "yes", null),
                new MessageList(List.of(), null),
                List.of(new CallbackButton("Yes", "yes", null)),
                Map.of("k", TextFormat.MARKDOWN));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void fixturesDeserializeAndSerializeAlike(String path, Class<?> type) {
        assertSameOutcome(loadFixture(path), type);
    }

    @ParameterizedTest(name = "{0} as {1}")
    @MethodSource("edgeCases")
    void edgeCasesDeserializeAlike(String json, Class<?> type) {
        assertSameOutcome(json, type);
    }

    @ParameterizedTest(name = "{0} as {1}")
    @MethodSource("coercions")
    void coercionsReadLikeJackson(String json, Class<?> type) {
        assertThatCode(() -> JACKSON.deserialize(json, type))
                .as("Jackson reads this input; move it to edgeCases() if that changes")
                .doesNotThrowAnyException();
        assertSameOutcome(json, type);
    }

    @ParameterizedTest
    @MethodSource("models")
    void modelsSerializeAlike(Object model) {
        assertThatJson(GSON.serialize(model)).isEqualTo(JACKSON.serialize(model));
    }

    @ParameterizedTest
    @MethodSource("fixtures")
    void genericTypesDeserializeAlike(String path, Class<?> type) {
        if (type != Update.class) {
            return;
        }
        String json = "[" + loadFixture(path) + "]";
        var listType = new TypeReference<List<Update>>() { };
        assertThat(GSON.deserialize(json, listType)).isEqualTo(JACKSON.deserialize(json, listType));
    }

    private static Arguments fixture(String path, Class<?> type) {
        return Arguments.of(path, type);
    }

    private static void assertSameOutcome(String json, Class<?> type) {
        Object expected;
        try {
            expected = JACKSON.deserialize(json, type);
        } catch (IllegalArgumentException e) {
            assertThatThrownBy(() -> GSON.deserialize(json, type))
                    .as("Jackson rejected the document, so must Gson")
                    .isInstanceOf(IllegalArgumentException.class);
            return;
        }
        Object actual;
        try {
            actual = GSON.deserialize(json, type);
        } catch (IllegalArgumentException e) {
            fail("Jackson read " + expected + " but Gson failed", e);
            return;
        }
        assertThat(actual).isEqualTo(expected);
        if (expected != null) {
            assertThatJson(GSON.serialize(actual)).isEqualTo(JACKSON.serialize(expected));
        }
    }
}
