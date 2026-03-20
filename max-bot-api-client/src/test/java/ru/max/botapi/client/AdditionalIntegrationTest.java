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

package ru.max.botapi.client;

import java.util.List;
import java.util.Set;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.max.botapi.jackson.JacksonMaxSerializer;
import ru.max.botapi.model.ActionRequestBody;
import ru.max.botapi.model.BotInfo;
import ru.max.botapi.model.BotPatch;
import ru.max.botapi.model.CallbackAnswer;
import ru.max.botapi.model.Chat;
import ru.max.botapi.model.ChatAdminsList;
import ru.max.botapi.model.ChatList;
import ru.max.botapi.model.ChatMember;
import ru.max.botapi.model.ChatMembersList;
import ru.max.botapi.model.ChatPatch;
import ru.max.botapi.model.GetPinnedMessageResult;
import ru.max.botapi.model.GetSubscriptionsResult;
import ru.max.botapi.model.Image;
import ru.max.botapi.model.Message;
import ru.max.botapi.model.MessageLinkType;
import ru.max.botapi.model.MessageList;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.NewMessageLink;
import ru.max.botapi.model.PinMessageBody;
import ru.max.botapi.model.SendMessageResult;
import ru.max.botapi.model.SenderAction;
import ru.max.botapi.model.SimpleQueryResult;
import ru.max.botapi.model.SubscriptionRequestBody;
import ru.max.botapi.model.TextFormat;
import ru.max.botapi.model.UpdateList;
import ru.max.botapi.model.UploadEndpoint;
import ru.max.botapi.model.UploadType;
import ru.max.botapi.model.UserIdsList;
import ru.max.botapi.model.VideoAttachmentDetails;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Additional WireMock integration tests for {@link MaxBotAPI} — covers
 * pagination, optional parameters, error scenarios, and edge cases
 * NOT covered in {@link MaxBotAPITest}.
 */
@WireMockTest
class AdditionalIntegrationTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN = "test-token";
    private static final String CONTENT_JSON = "application/json";

    private MaxBotAPI api;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        MaxClientConfig config = MaxClientConfig.builder()
                .baseUrl(wmInfo.getHttpBaseUrl())
                .enableRateLimiting(false)
                .maxRetries(0)
                .build();
        JacksonMaxSerializer serializer = new JacksonMaxSerializer();
        JdkHttpMaxTransportClient transport = new JdkHttpMaxTransportClient(TOKEN, config);
        MaxClient client = new MaxClient(transport, serializer, config);
        api = new MaxBotAPI(client);
    }

    // ===== Pagination Tests =====

    @Test
    void getChats_withCountAndMarker() {
        stubFor(get(urlPathEqualTo("/chats"))
                .withQueryParam("count", equalTo("5"))
                .withQueryParam("marker", equalTo("100"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "chats": [
                                    {
                                      "chat_id": 50003,
                                      "type": "chat",
                                      "status": "active",
                                      "title": "Page 2 Chat",
                                      "last_event_time": 1700000300000,
                                      "participants_count": 3,
                                      "is_public": false
                                    }
                                  ],
                                  "marker": 50003
                                }
                                """)));

        ChatList list = api.getChats().count(5).marker(100L).execute();

        assertThat(list.chats()).hasSize(1);
        assertThat(list.chats().getFirst().title()).isEqualTo("Page 2 Chat");
        assertThat(list.marker()).isEqualTo(50003L);
    }

    @Test
    void getMembers_withPagination() {
        stubFor(get(urlPathEqualTo("/chats/123/members"))
                .withQueryParam("count", equalTo("10"))
                .withQueryParam("marker", equalTo("500"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "members": [
                                    {
                                      "user_id": 99010,
                                      "name": "Paginated User",
                                      "is_bot": false,
                                      "last_activity_time": 1700000100000,
                                      "last_access_time": 1700000100000,
                                      "is_owner": false,
                                      "is_admin": false,
                                      "join_time": 1699000000000
                                    }
                                  ],
                                  "marker": 99010
                                }
                                """)));

        ChatMembersList list = api.getMembers(123L).count(10).marker(500L).execute();

        assertThat(list.members()).hasSize(1);
        assertThat(list.members().getFirst().name()).isEqualTo("Paginated User");
        assertThat(list.marker()).isEqualTo(99010L);
    }

    @Test
    void getMessages_withFromAndTo() {
        stubFor(get(urlPathEqualTo("/messages"))
                .withQueryParam("chat_id", equalTo("123"))
                .withQueryParam("from", equalTo("1700000000000"))
                .withQueryParam("to", equalTo("1700001000000"))
                .withQueryParam("count", equalTo("50"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "messages": [
                                    {
                                      "sender": {
                                        "user_id": 99001,
                                        "name": "John",
                                        "is_bot": false,
                                        "last_activity_time": 1700000100000
                                      },
                                      "recipient": {"chat_id": 123, "chat_type": "chat"},
                                      "timestamp": 1700000500000,
                                      "body": {"mid": "msg_ts_001", "seq": 1, "text": "Filtered msg"}
                                    }
                                  ],
                                  "marker": 1700000500000
                                }
                                """)));

        MessageList list = api.getMessages()
                .chatId(123L)
                .from(1700000000000L)
                .to(1700001000000L)
                .count(50)
                .execute();

        assertThat(list.messages()).hasSize(1);
        assertThat(list.messages().getFirst().body().text()).isEqualTo("Filtered msg");
        assertThat(list.marker()).isEqualTo(1700000500000L);
    }

    // ===== Optional Parameter Tests =====

    @Test
    void sendMessage_withAllOptionalParams() {
        stubFor(post(urlPathEqualTo("/messages"))
                .withQueryParam("chat_id", equalTo("123"))
                .withQueryParam("disable_link_preview", equalTo("true"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "message": {
                                    "sender": {
                                      "user_id": 12345,
                                      "name": "TestBot",
                                      "is_bot": true,
                                      "last_activity_time": 1700000000000
                                    },
                                    "recipient": {"chat_id": 123, "chat_type": "chat"},
                                    "timestamp": 1700001000000,
                                    "body": {"mid": "msg_opt_001", "seq": 1, "text": "Full opts"}
                                  }
                                }
                                """)));

        NewMessageBody body = new NewMessageBody("Full opts", null, null, true, TextFormat.MARKDOWN);
        SendMessageResult result = api.sendMessage(body)
                .chatId(123L)
                .disableLinkPreview(true)
                .execute();

        assertThat(result.message().body().text()).isEqualTo("Full opts");
    }

    @Test
    void sendMessage_withLink() {
        stubFor(post(urlPathEqualTo("/messages"))
                .withQueryParam("chat_id", equalTo("123"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "message": {
                                    "sender": {
                                      "user_id": 12345,
                                      "name": "TestBot",
                                      "is_bot": true,
                                      "last_activity_time": 1700000000000
                                    },
                                    "recipient": {"chat_id": 123, "chat_type": "chat"},
                                    "timestamp": 1700001000000,
                                    "body": {"mid": "msg_link_001", "seq": 1, "text": "Reply"}
                                  }
                                }
                                """)));

        var link = new NewMessageLink(MessageLinkType.REPLY, "original_mid");
        NewMessageBody body = new NewMessageBody("Reply", null, link, null, null);
        SendMessageResult result = api.sendMessage(body).chatId(123L).execute();

        assertThat(result.message().body().mid()).isEqualTo("msg_link_001");
    }

    // ===== Error Scenarios =====

    @Test
    void sendMessage_returns400() {
        stubFor(post(urlPathEqualTo("/messages"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"code": "bad_request", "message": "Invalid request body"}
                                """)));

        NewMessageBody body = new NewMessageBody("test", null, null, null, null);
        assertThatThrownBy(() -> api.sendMessage(body).chatId(123L).execute())
                .isInstanceOf(MaxApiException.class)
                .satisfies(ex -> {
                    MaxApiException apiEx = (MaxApiException) ex;
                    assertThat(apiEx.statusCode()).isEqualTo(400);
                    assertThat(apiEx.errorCode()).isEqualTo("bad_request");
                });
    }

    // maxRetries(0) in setUp(): verifies that when retries are disabled,
    // 503 propagates as MaxApiException. Retry-on-503 behaviour is covered
    // in RetryPolicyTest.
    @Test
    void getMyInfo_returns503ServiceUnavailable() {
        stubFor(get(urlPathEqualTo("/me"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"code": "service_unavailable", "message": "Service unavailable"}
                                """)));

        assertThatThrownBy(() -> api.getMyInfo().execute())
                .isInstanceOf(MaxApiException.class)
                .satisfies(ex -> {
                    MaxApiException apiEx = (MaxApiException) ex;
                    assertThat(apiEx.statusCode()).isEqualTo(503);
                });
    }

    @Test
    void editChat_returns404() {
        stubFor(patch(urlPathEqualTo("/chats/99999"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"code": "not_found", "message": "Chat not found"}
                                """)));

        ChatPatch patch = new ChatPatch("Title", null, null, null);
        assertThatThrownBy(() -> api.editChat(patch, 99999L).execute())
                .isInstanceOf(MaxApiException.class)
                .satisfies(ex -> {
                    MaxApiException apiEx = (MaxApiException) ex;
                    assertThat(apiEx.statusCode()).isEqualTo(404);
                    assertThat(apiEx.errorCode()).isEqualTo("not_found");
                });
    }

    @Test
    void deleteMessage_returns400() {
        stubFor(delete(urlPathEqualTo("/messages"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"code": "bad_request", "message": "Invalid message ID"}
                                """)));

        assertThatThrownBy(() -> api.deleteMessage("invalid_mid").execute())
                .isInstanceOf(MaxApiException.class)
                .satisfies(ex -> {
                    MaxApiException apiEx = (MaxApiException) ex;
                    assertThat(apiEx.statusCode()).isEqualTo(400);
                });
    }

    @Test
    void addMembers_returns429() {
        stubFor(post(urlPathEqualTo("/chats/123/members"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withHeader("Retry-After", "10")
                        .withBody("""
                                {"code": "rate_limit", "message": "Too many requests"}
                                """)));

        UserIdsList ids = new UserIdsList(List.of(111L));
        assertThatThrownBy(() -> api.addMembers(ids, 123L).execute())
                .isInstanceOf(MaxRateLimitException.class)
                .satisfies(ex -> {
                    MaxRateLimitException rle = (MaxRateLimitException) ex;
                    assertThat(rle.statusCode()).isEqualTo(429);
                });
    }

    // ===== Empty List Responses =====

    @Test
    void getChats_returnsEmptyList() {
        stubFor(get(urlPathEqualTo("/chats"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"chats": [], "marker": null}
                                """)));

        ChatList list = api.getChats().execute();

        assertThat(list.chats()).isEmpty();
        assertThat(list.marker()).isNull();
    }

    @Test
    void getMembers_returnsEmptyList() {
        stubFor(get(urlPathEqualTo("/chats/123/members"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"members": [], "marker": null}
                                """)));

        ChatMembersList list = api.getMembers(123L).execute();

        assertThat(list.members()).isEmpty();
        assertThat(list.marker()).isNull();
    }

    @Test
    void getMessages_returnsEmptyList() {
        stubFor(get(urlPathEqualTo("/messages"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"messages": [], "marker": null}
                                """)));

        MessageList list = api.getMessages().chatId(123L).execute();

        assertThat(list.messages()).isEmpty();
        assertThat(list.marker()).isNull();
    }

    @Test
    void getUpdates_returnsEmptyList() {
        stubFor(get(urlPathEqualTo("/updates"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"updates": [], "marker": null}
                                """)));

        UpdateList list = api.getUpdates().execute();

        assertThat(list.updates()).isEmpty();
        assertThat(list.marker()).isNull();
    }

    // ===== Subscription Tests =====

    @Test
    void subscribe_withAllFields() {
        stubFor(post(urlPathEqualTo("/subscriptions"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"success": true}
                                """)));

        SubscriptionRequestBody body = new SubscriptionRequestBody(
                "https://example.com/webhook",
                List.of("message_created", "bot_started", "message_callback"),
                "webhook_secret_123");
        SimpleQueryResult result = api.subscribe(body).execute();

        assertThat(result.success()).isTrue();
    }

    @Test
    void getSubscriptions_returnsMultiple() {
        stubFor(get(urlPathEqualTo("/subscriptions"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "subscriptions": [
                                    {
                                      "url": "https://example.com/wh1",
                                      "update_types": ["message_created"]
                                    },
                                    {
                                      "url": "https://example.com/wh2",
                                      "update_types": null
                                    }
                                  ]
                                }
                                """)));

        GetSubscriptionsResult result = api.getSubscriptions().execute();

        assertThat(result.subscriptions()).hasSize(2);
        assertThat(result.subscriptions().get(0).url()).isEqualTo("https://example.com/wh1");
        assertThat(result.subscriptions().get(0).updateTypes())
                .containsExactly("message_created");
        assertThat(result.subscriptions().get(1).url()).isEqualTo("https://example.com/wh2");
        assertThat(result.subscriptions().get(1).updateTypes()).isNull();
    }

    // ===== Video Details =====

    @Test
    void getVideoDetails_returnsAllFields() {
        stubFor(get(urlPathEqualTo("/videos/video_token_xyz"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "url": "https://cdn.example.com/vid.mp4",
                                  "token": "video_token_xyz",
                                  "thumbnail": "https://cdn.example.com/thumb.jpg",
                                  "width": 3840,
                                  "height": 2160,
                                  "duration": 300
                                }
                                """)));

        VideoAttachmentDetails details = api.getVideoAttachmentDetails("video_token_xyz").execute();

        assertThat(details.url()).isEqualTo("https://cdn.example.com/vid.mp4");
        assertThat(details.token()).isEqualTo("video_token_xyz");
        assertThat(details.thumbnail()).isEqualTo("https://cdn.example.com/thumb.jpg");
        assertThat(details.width()).isEqualTo(3840);
        assertThat(details.height()).isEqualTo(2160);
        assertThat(details.duration()).isEqualTo(300);
    }

    @Test
    void getVideoDetails_minimalFields() {
        stubFor(get(urlPathEqualTo("/videos/vtok_min"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "url": "https://cdn.example.com/vid.mp4",
                                  "token": "vtok_min"
                                }
                                """)));

        VideoAttachmentDetails details = api.getVideoAttachmentDetails("vtok_min").execute();

        assertThat(details.url()).isEqualTo("https://cdn.example.com/vid.mp4");
        assertThat(details.thumbnail()).isNull();
        assertThat(details.width()).isNull();
        assertThat(details.height()).isNull();
        assertThat(details.duration()).isNull();
    }

    // ===== Edit Chat =====

    @Test
    void editChat_withAllFields() {
        stubFor(patch(urlPathEqualTo("/chats/456"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "chat_id": 456,
                                  "type": "chat",
                                  "status": "active",
                                  "title": "Updated Chat Title",
                                  "last_event_time": 1700001000000,
                                  "participants_count": 10,
                                  "is_public": true,
                                  "description": "Updated description"
                                }
                                """)));

        ChatPatch chatPatch = new ChatPatch("Updated Chat Title",
                new Image("http://icon.png"), "pin_msg_001", true);
        Chat chat = api.editChat(chatPatch, 456L).execute();

        assertThat(chat.chatId()).isEqualTo(456L);
        assertThat(chat.title()).isEqualTo("Updated Chat Title");
        assertThat(chat.isPublic()).isTrue();
    }

    // ===== Post Admins =====

    @Test
    void postAdmins_sendsUserIdsInBody() {
        stubFor(post(urlPathEqualTo("/chats/789/members/admins"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .withRequestBody(matchingJsonPath("$.user_ids"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"success": true}
                                """)));

        ChatAdminsList adminsList = new ChatAdminsList(List.of(111L, 222L, 333L));
        SimpleQueryResult result = api.postAdmins(adminsList, 789L).execute();

        assertThat(result.success()).isTrue();
    }

    // ===== Unsubscribe with query param =====

    @Test
    void unsubscribe_sendsUrlQueryParam() {
        stubFor(delete(urlPathEqualTo("/subscriptions"))
                .withQueryParam("url", equalTo("https://example.com/webhook"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"success": true}
                                """)));

        SimpleQueryResult result = api.unsubscribe("https://example.com/webhook").execute();

        assertThat(result.success()).isTrue();
    }

    // ===== Upload URL variations =====

    @Test
    void getUploadUrl_imageType() {
        stubFor(post(urlPathEqualTo("/uploads"))
                .withQueryParam("type", equalTo("image"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "url": "https://upload.example.com/img",
                                  "token": "upload_img_tok"
                                }
                                """)));

        UploadEndpoint endpoint = api.getUploadUrl(UploadType.IMAGE).execute();

        assertThat(endpoint.url()).isEqualTo("https://upload.example.com/img");
        assertThat(endpoint.token()).isEqualTo("upload_img_tok");
    }

    @Test
    void getUploadUrl_videoType() {
        stubFor(post(urlPathEqualTo("/uploads"))
                .withQueryParam("type", equalTo("video"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "url": "https://upload.example.com/video_upload",
                                  "token": "upload_vid_tok"
                                }
                                """)));

        UploadEndpoint endpoint = api.getUploadUrl(UploadType.VIDEO).execute();

        assertThat(endpoint.url()).isEqualTo("https://upload.example.com/video_upload");
        assertThat(endpoint.token()).isEqualTo("upload_vid_tok");
    }

    @Test
    void getUploadUrl_audioType() {
        stubFor(post(urlPathEqualTo("/uploads"))
                .withQueryParam("type", equalTo("audio"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "url": "https://upload.example.com/audio_upload",
                                  "token": null
                                }
                                """)));

        UploadEndpoint endpoint = api.getUploadUrl(UploadType.AUDIO).execute();

        assertThat(endpoint.url()).isEqualTo("https://upload.example.com/audio_upload");
        assertThat(endpoint.token()).isNull();
    }

    @Test
    void getUploadUrl_fileType() {
        stubFor(post(urlPathEqualTo("/uploads"))
                .withQueryParam("type", equalTo("file"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "url": "https://upload.example.com/file_upload",
                                  "token": null
                                }
                                """)));

        UploadEndpoint endpoint = api.getUploadUrl(UploadType.FILE).execute();

        assertThat(endpoint.url()).isEqualTo("https://upload.example.com/file_upload");
    }

    // ===== Answer Callback with message body =====

    @Test
    void answerOnCallback_withMessageBody() {
        stubFor(post(urlPathEqualTo("/answers"))
                .withQueryParam("callback_id", equalTo("cb_full"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .withRequestBody(matchingJsonPath("$.message"))
                .withRequestBody(matchingJsonPath("$.notification"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"success": true}
                                """)));

        var msgBody = new NewMessageBody("Callback response", null, null, true, null);
        CallbackAnswer answer = new CallbackAnswer(msgBody, "Done!");
        SimpleQueryResult result = api.answerOnCallback(answer, "cb_full").execute();

        assertThat(result.success()).isTrue();
    }

    // ===== Pin Message with notify =====

    @Test
    void pinMessage_withNotify() {
        stubFor(put(urlPathEqualTo("/chats/123/pin"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .withRequestBody(
                        matchingJsonPath("$.message_id",
                                equalTo("msg_pin_001")))
                .withRequestBody(
                        matchingJsonPath("$.notify", equalTo("true")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"success": true}
                                """)));

        PinMessageBody body = new PinMessageBody("msg_pin_001", true);
        SimpleQueryResult result = api.pinMessage(body, 123L).execute();

        assertThat(result.success()).isTrue();
    }

    // ===== Get Pinned Message when no message is pinned =====

    @Test
    void getPinnedMessage_returnsNullMessage() {
        stubFor(get(urlPathEqualTo("/chats/123/pin"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"message": null}
                                """)));

        GetPinnedMessageResult result = api.getPinnedMessage(123L).execute();

        assertThat(result.message()).isNull();
    }

    // ===== Send Action with different actions =====

    @Test
    void sendAction_sendingPhoto() {
        stubFor(post(urlPathEqualTo("/chats/123/actions"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"success": true}
                                """)));

        ActionRequestBody body = new ActionRequestBody(SenderAction.SENDING_PHOTO);
        SimpleQueryResult result = api.sendAction(body, 123L).execute();

        assertThat(result.success()).isTrue();
    }

    @Test
    void sendAction_markSeen() {
        stubFor(post(urlPathEqualTo("/chats/456/actions"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"success": true}
                                """)));

        ActionRequestBody body = new ActionRequestBody(SenderAction.MARK_SEEN);
        SimpleQueryResult result = api.sendAction(body, 456L).execute();

        assertThat(result.success()).isTrue();
    }

    // ===== Edit My Info with commands =====

    @Test
    void editMyInfo_withDescriptionAndCommands() {
        stubFor(patch(urlPathEqualTo("/me"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "user_id": 12345,
                                  "name": "TestBot",
                                  "username": "test_bot",
                                  "is_bot": true,
                                  "last_activity_time": 1700000000000,
                                  "description": "Updated description",
                                  "commands": [
                                    {"name": "start", "description": "Start"},
                                    {"name": "help", "description": "Help"}
                                  ]
                                }
                                """)));

        var cmd1 = new ru.max.botapi.model.BotCommand("start", "Start");
        var cmd2 = new ru.max.botapi.model.BotCommand("help", "Help");
        BotPatch patch = new BotPatch(null, "Updated description", List.of(cmd1, cmd2), null);
        BotInfo info = api.editMyInfo(patch).execute();

        assertThat(info.description()).isEqualTo("Updated description");
        assertThat(info.commands()).hasSize(2);
    }

    // ===== Get Updates with types filter =====

    @Test
    void getUpdates_withTypesFilter() {
        stubFor(get(urlPathEqualTo("/updates"))
                .withQueryParam("types", matching(".*message_created.*"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "updates": [
                                    {
                                      "update_type": "message_created",
                                      "timestamp": 1700001000000,
                                      "message": {
                                        "sender": {
                                          "user_id": 99001,
                                          "name": "John",
                                          "is_bot": false,
                                          "last_activity_time": 1700000100000
                                        },
                                        "recipient": {"chat_id": 50001, "chat_type": "chat"},
                                        "timestamp": 1700001000000,
                                        "body": {"mid": "filtered_001", "seq": 1, "text": "Filtered!"}
                                      }
                                    }
                                  ],
                                  "marker": 55555
                                }
                                """)));

        UpdateList list = api.getUpdates()
                .types(Set.of("message_created"))
                .execute();

        assertThat(list.updates()).hasSize(1);
        assertThat(list.marker()).isEqualTo(55555L);
    }

    // ===== Delete Admins =====

    @Test
    void deleteAdmins_withSpecificUserId() {
        stubFor(delete(urlPathEqualTo("/chats/100/members/admins/200"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"success": true}
                                """)));

        SimpleQueryResult result = api.deleteAdmins(100L, 200L).execute();

        assertThat(result.success()).isTrue();
    }

    // ===== Edit Message with new text =====

    @Test
    void editMessage_withNewText() {
        stubFor(put(urlPathEqualTo("/messages"))
                .withQueryParam("message_id", equalTo("msg_edit_001"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"success": true}
                                """)));

        NewMessageBody body = new NewMessageBody("Edited content", null, null, null, TextFormat.HTML);
        SimpleQueryResult result = api.editMessage(body, "msg_edit_001").execute();

        assertThat(result.success()).isTrue();
    }

    // ===== SimpleQueryResult failure =====

    @Test
    void deleteChat_returnsFailure() {
        stubFor(delete(urlPathEqualTo("/chats/999"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"success": false, "message": "Cannot delete chat"}
                                """)));

        SimpleQueryResult result = api.deleteChat(999L).execute();

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Cannot delete chat");
    }

    // ===== Multiple updates in single response =====

    @Test
    void getUpdates_multipleUpdateTypes() {
        stubFor(get(urlPathEqualTo("/updates"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "updates": [
                                    {
                                      "update_type": "message_created",
                                      "timestamp": 1700001000000,
                                      "message": {
                                        "sender": {
                                          "user_id": 99001, "name": "John",
                                          "is_bot": false, "last_activity_time": 100
                                        },
                                        "recipient": {"chat_id": 50001, "chat_type": "chat"},
                                        "timestamp": 1700001000000,
                                        "body": {"mid": "m1", "seq": 1, "text": "Hello"}
                                      }
                                    },
                                    {
                                      "update_type": "bot_started",
                                      "timestamp": 1700002000000,
                                      "chat_id": 60001,
                                      "user": {
                                        "user_id": 99002, "name": "Jane",
                                        "is_bot": false, "last_activity_time": 200
                                      }
                                    },
                                    {
                                      "update_type": "message_removed",
                                      "timestamp": 1700003000000,
                                      "message_id": "m2",
                                      "chat_id": 50001,
                                      "user_id": 99001
                                    }
                                  ],
                                  "marker": 99999
                                }
                                """)));

        UpdateList list = api.getUpdates().execute();

        assertThat(list.updates()).hasSize(3);
        assertThat(list.updates().get(0).updateType()).isEqualTo("message_created");
        assertThat(list.updates().get(1).updateType()).isEqualTo("bot_started");
        assertThat(list.updates().get(2).updateType()).isEqualTo("message_removed");
        assertThat(list.marker()).isEqualTo(99999L);
    }

    // ===== Chat with dialog type and dialogWithUser =====

    @Test
    void getChat_dialogWithUser() {
        stubFor(get(urlPathEqualTo("/chats/777"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "chat_id": 777,
                                  "type": "dialog",
                                  "status": "active",
                                  "last_event_time": 1700000200000,
                                  "participants_count": 2,
                                  "is_public": false,
                                  "dialog_with_user": {
                                    "user_id": 99001,
                                    "name": "Alice",
                                    "username": "alice",
                                    "is_bot": false,
                                    "last_activity_time": 1700000100000,
                                    "description": "A user",
                                    "avatar_url": "http://avatar.jpg",
                                    "full_avatar_url": "http://full_avatar.jpg"
                                  }
                                }
                                """)));

        Chat chat = api.getChat(777L).execute();

        assertThat(chat.chatId()).isEqualTo(777L);
        assertThat(chat.type()).isEqualTo(ru.max.botapi.model.ChatType.DIALOG);
        assertThat(chat.dialogWithUser()).isNotNull();
        assertThat(chat.dialogWithUser().name()).isEqualTo("Alice");
        assertThat(chat.dialogWithUser().avatarUrl()).isEqualTo("http://avatar.jpg");
    }

    // ===== Leave chat that was already left =====

    @Test
    void leaveChat_returnsFailure() {
        stubFor(delete(urlPathEqualTo("/chats/123/members/me"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"success": false, "message": "Already left the chat"}
                                """)));

        SimpleQueryResult result = api.leaveChat(123L).execute();

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Already left the chat");
    }
}
