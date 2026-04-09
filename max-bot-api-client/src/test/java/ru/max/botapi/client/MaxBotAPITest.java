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

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.max.botapi.jackson.JacksonMaxSerializer;
import ru.max.botapi.model.ActionRequestBody;
import ru.max.botapi.model.AddMemberFailure;
import ru.max.botapi.model.AddMembersResult;
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
import ru.max.botapi.model.Message;
import ru.max.botapi.model.MessageList;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.PinMessageBody;
import ru.max.botapi.model.SendMessageResult;
import ru.max.botapi.model.SenderAction;
import ru.max.botapi.model.SimpleQueryResult;
import ru.max.botapi.model.SubscriptionRequestBody;
import ru.max.botapi.model.UpdateList;
import ru.max.botapi.model.UploadEndpoint;
import ru.max.botapi.model.UploadType;
import ru.max.botapi.model.UserIdsList;
import ru.max.botapi.model.VideoAttachmentDetails;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * WireMock integration tests for {@link MaxBotAPI} — covers all 31 API methods
 * plus error scenarios.
 */
@WireMockTest
class MaxBotAPITest {

    private static final String TOKEN = "test-token";
    private static final String AUTH_HEADER = "Authorization";
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

    // ===== Bot Methods =====

    @Test
    void getMyInfo() {
        stubFor(get(urlPathEqualTo("/me"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "user_id": 12345,
                                  "name": "TestBot",
                                  "username": "test_bot",
                                  "is_bot": true,
                                  "last_activity_time": 1700000000000
                                }
                                """)));

        BotInfo info = api.getMyInfo().execute();

        assertThat(info).isNotNull();
        assertThat(info.userId()).isEqualTo(12345L);
        assertThat(info.name()).isEqualTo("TestBot");
    }

    @Test
    void editMyInfo() {
        stubFor(patch(urlPathEqualTo("/me"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "user_id": 12345,
                                  "name": "UpdatedBot",
                                  "username": "test_bot",
                                  "is_bot": true,
                                  "last_activity_time": 1700000001000
                                }
                                """)));

        BotPatch patch = new BotPatch("UpdatedBot", null, null, null);
        BotInfo info = api.editMyInfo(patch).execute();

        assertThat(info).isNotNull();
        assertThat(info.name()).isEqualTo("UpdatedBot");
    }

    // ===== Chat Methods =====

    @Test
    void getChats() {
        stubFor(get(urlPathEqualTo("/chats"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "chats": [
                                    {
                                      "chat_id": 50001,
                                      "type": "chat",
                                      "status": "active",
                                      "title": "Test Chat",
                                      "last_event_time": 1700000200000,
                                      "participants_count": 3,
                                      "owner_id": 99001,
                                      "is_public": false
                                    }
                                  ],
                                  "marker": null
                                }
                                """)));

        ChatList list = api.getChats().count(10).execute();

        assertThat(list).isNotNull();
        assertThat(list.chats()).hasSize(1);
        assertThat(list.chats().get(0).chatId()).isEqualTo(50001L);
    }

    @Test
    void getChat() {
        stubFor(get(urlPathEqualTo("/chats/123"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "chat_id": 123,
                                  "type": "chat",
                                  "status": "active",
                                  "title": "My Chat",
                                  "last_event_time": 1700000200000,
                                  "participants_count": 2,
                                  "owner_id": 99001,
                                  "is_public": false
                                }
                                """)));

        Chat chat = api.getChat(123L).execute();

        assertThat(chat).isNotNull();
        assertThat(chat.chatId()).isEqualTo(123L);
    }

    @Test
    void getChatByLink() {
        stubFor(get(urlPathEqualTo("/chats/test-link"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "chat_id": 55555,
                                  "type": "channel",
                                  "status": "active",
                                  "title": "Public Channel",
                                  "last_event_time": 1700000300000,
                                  "participants_count": 100,
                                  "owner_id": 99001,
                                  "is_public": true
                                }
                                """)));

        Chat chat = api.getChatByLink("test-link").execute();

        assertThat(chat).isNotNull();
        assertThat(chat.chatId()).isEqualTo(55555L);
    }

    @Test
    void editChat() {
        stubFor(patch(urlPathEqualTo("/chats/123"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "chat_id": 123,
                                  "type": "chat",
                                  "status": "active",
                                  "title": "New Title",
                                  "last_event_time": 1700000400000,
                                  "participants_count": 2,
                                  "owner_id": 99001,
                                  "is_public": false
                                }
                                """)));

        ChatPatch chatPatch = new ChatPatch("New Title", null, null, null);
        Chat chat = api.editChat(chatPatch, 123L).execute();

        assertThat(chat).isNotNull();
        assertThat(chat.title()).isEqualTo("New Title");
    }

    @Test
    void deleteChat() {
        stubFor(delete(urlPathEqualTo("/chats/123"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        SimpleQueryResult result = api.deleteChat(123L).execute();

        assertThat(result.success()).isTrue();
    }

    @Test
    void sendAction() {
        stubFor(post(urlPathEqualTo("/chats/123/actions"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        ActionRequestBody body = new ActionRequestBody(SenderAction.TYPING_ON);
        SimpleQueryResult result = api.sendAction(body, 123L).execute();

        assertThat(result.success()).isTrue();
    }

    // ===== Chat Members =====

    @Test
    void getMembers() {
        stubFor(get(urlPathEqualTo("/chats/123/members"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "members": [
                                    {
                                      "user_id": 99001,
                                      "name": "Alice",
                                      "is_bot": false,
                                      "last_activity_time": 1700000100000,
                                      "last_access_time": 1700000100000,
                                      "is_owner": true,
                                      "is_admin": true,
                                      "join_time": 1699000000000
                                    }
                                  ],
                                  "marker": null
                                }
                                """)));

        ChatMembersList list = api.getMembers(123L).count(50).execute();

        assertThat(list).isNotNull();
        assertThat(list.members()).hasSize(1);
        assertThat(list.members().get(0).userId()).isEqualTo(99001L);
    }

    @Test
    void addMembers_success() {
        stubFor(post(urlPathEqualTo("/chats/123/members"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        UserIdsList ids = new UserIdsList(List.of(111L, 222L));
        AddMembersResult result = api.addMembers(ids, 123L).execute();

        assertThat(result.success()).isTrue();
        assertThat(result.failedUserIds()).isNull();
        assertThat(result.failedUserDetails()).isNull();
    }

    @Test
    void addMembers_partialFailure_privacySettings() {
        String body = "{\"success\":false,"
                + "\"failed_user_ids\":[217690268],"
                + "\"failed_user_details\":[{"
                + "\"error_code\":\"add.participant.privacy\","
                + "\"user_ids\":[217690268]}]}";
        stubFor(post(urlPathEqualTo("/chats/123/members"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody(body)));

        UserIdsList ids = new UserIdsList(List.of(217690268L));
        AddMembersResult result = api.addMembers(ids, 123L).execute();

        assertThat(result.success()).isFalse();
        assertThat(result.failedUserIds()).containsExactly(217690268L);
        assertThat(result.failedUserDetails()).hasSize(1);
        AddMemberFailure failure = result.failedUserDetails().get(0);
        assertThat(failure.errorCode()).isEqualTo("add.participant.privacy");
        assertThat(failure.userIds()).containsExactly(217690268L);
    }

    @Test
    void removeMember() {
        stubFor(delete(urlPathEqualTo("/chats/123/members"))
                .withQueryParam("user_id", equalTo("456"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        SimpleQueryResult result = api.removeMember(123L, 456L).execute();

        assertThat(result.success()).isTrue();
    }

    @Test
    void getMembership() {
        stubFor(get(urlPathEqualTo("/chats/123/members/me"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "user_id": 12345,
                                  "name": "TestBot",
                                  "is_bot": true,
                                  "last_activity_time": 1700000000000,
                                  "last_access_time": 1700000000000,
                                  "is_owner": false,
                                  "is_admin": false,
                                  "join_time": 1699000000000
                                }
                                """)));

        ChatMember member = api.getMembership(123L).execute();

        assertThat(member).isNotNull();
        assertThat(member.userId()).isEqualTo(12345L);
    }

    @Test
    void leaveChat() {
        stubFor(delete(urlPathEqualTo("/chats/123/members/me"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        SimpleQueryResult result = api.leaveChat(123L).execute();

        assertThat(result.success()).isTrue();
    }

    @Test
    void getAdmins() {
        stubFor(get(urlPathEqualTo("/chats/123/members/admins"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "members": [
                                    {
                                      "user_id": 99001,
                                      "name": "Admin User",
                                      "is_bot": false,
                                      "last_activity_time": 1700000100000,
                                      "last_access_time": 1700000100000,
                                      "is_owner": false,
                                      "is_admin": true,
                                      "join_time": 1699000000000
                                    }
                                  ],
                                  "marker": null
                                }
                                """)));

        ChatMembersList admins = api.getAdmins(123L).execute();

        assertThat(admins).isNotNull();
        assertThat(admins.members()).hasSize(1);
        assertThat(admins.members().get(0).isAdmin()).isTrue();
    }

    @Test
    void postAdmins() {
        stubFor(post(urlPathEqualTo("/chats/123/members/admins"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        ChatAdminsList adminsList = new ChatAdminsList(List.of(99001L));
        SimpleQueryResult result = api.postAdmins(adminsList, 123L).execute();

        assertThat(result.success()).isTrue();
    }

    @Test
    void deleteAdmins() {
        stubFor(delete(urlPathEqualTo("/chats/123/members/admins/456"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        SimpleQueryResult result = api.deleteAdmins(123L, 456L).execute();

        assertThat(result.success()).isTrue();
    }

    // ===== Pin =====

    @Test
    void getPinnedMessage() {
        stubFor(get(urlPathEqualTo("/chats/123/pin"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "message": {
                                    "sender": {
                                      "user_id": 99001,
                                      "name": "John Doe",
                                      "is_bot": false,
                                      "last_activity_time": 1700000100000
                                    },
                                    "recipient": {"chat_id": 123, "chat_type": "chat"},
                                    "timestamp": 1700000500000,
                                    "body": {"mid": "pinned_msg_001", "seq": 5, "text": "Pinned!"}
                                  }
                                }
                                """)));

        GetPinnedMessageResult result = api.getPinnedMessage(123L).execute();

        assertThat(result).isNotNull();
        assertThat(result.message()).isNotNull();
        assertThat(result.message().body().mid()).isEqualTo("pinned_msg_001");
    }

    @Test
    void pinMessage() {
        stubFor(put(urlPathEqualTo("/chats/123/pin"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        PinMessageBody body = new PinMessageBody("pinned_msg_001", null);
        SimpleQueryResult result = api.pinMessage(body, 123L).execute();

        assertThat(result.success()).isTrue();
    }

    @Test
    void unpinMessage() {
        stubFor(delete(urlPathEqualTo("/chats/123/pin"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        SimpleQueryResult result = api.unpinMessage(123L).execute();

        assertThat(result.success()).isTrue();
    }

    // ===== Messages =====

    @Test
    void getMessages() {
        stubFor(get(urlPathEqualTo("/messages"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "messages": [
                                    {
                                      "sender": {
                                        "user_id": 99001,
                                        "name": "John Doe",
                                        "is_bot": false,
                                        "last_activity_time": 1700000100000
                                      },
                                      "recipient": {"chat_id": 123, "chat_type": "chat"},
                                      "timestamp": 1700000500000,
                                      "body": {"mid": "msg_001", "seq": 1, "text": "Hello!"}
                                    }
                                  ],
                                  "marker": null
                                }
                                """)));

        MessageList list = api.getMessages().chatId(123L).execute();

        assertThat(list).isNotNull();
        assertThat(list.messages()).hasSize(1);
        assertThat(list.messages().get(0).body().mid()).isEqualTo("msg_001");
    }

    @Test
    void getMessageById() {
        stubFor(get(urlPathEqualTo("/messages/mid1"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "sender": {
                                    "user_id": 99001,
                                    "name": "John Doe",
                                    "is_bot": false,
                                    "last_activity_time": 1700000100000
                                  },
                                  "recipient": {"chat_id": 50001, "chat_type": "chat"},
                                  "timestamp": 1700000500000,
                                  "body": {"mid": "mid1", "seq": 42, "text": "Test message"}
                                }
                                """)));

        Message message = api.getMessageById("mid1").execute();

        assertThat(message).isNotNull();
        assertThat(message.body().mid()).isEqualTo("mid1");
    }

    @Test
    void sendMessage() {
        stubFor(post(urlPathEqualTo("/messages"))
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
                                    "body": {"mid": "sent_msg_001", "seq": 10, "text": "Hello!"}
                                  }
                                }
                                """)));

        NewMessageBody body = new NewMessageBody("Hello!", null, null, null, null);
        SendMessageResult result = api.sendMessage(body).chatId(123L).execute();

        assertThat(result).isNotNull();
        assertThat(result.message()).isNotNull();
        assertThat(result.message().body().mid()).isEqualTo("sent_msg_001");
    }

    @Test
    void editMessage() {
        stubFor(put(urlPathEqualTo("/messages"))
                .withQueryParam("message_id", equalTo("mid1"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        NewMessageBody body = new NewMessageBody("Updated text", null, null, null, null);
        SimpleQueryResult result = api.editMessage(body, "mid1").execute();

        assertThat(result.success()).isTrue();
    }

    @Test
    void deleteMessage() {
        stubFor(delete(urlPathEqualTo("/messages"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        SimpleQueryResult result = api.deleteMessage("mid1").execute();

        assertThat(result.success()).isTrue();
    }

    // ===== Callback =====

    @Test
    void answerOnCallback() {
        stubFor(post(urlPathEqualTo("/answers"))
                .withQueryParam("callback_id", equalTo("cb1"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        CallbackAnswer answer = new CallbackAnswer(null, "Action completed!");
        SimpleQueryResult result = api.answerOnCallback(answer, "cb1").execute();

        assertThat(result.success()).isTrue();
    }

    // ===== Subscriptions =====

    @Test
    void getSubscriptions() {
        stubFor(get(urlPathEqualTo("/subscriptions"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "subscriptions": [
                                    {
                                      "url": "https://example.com/webhook",
                                      "update_types": null
                                    }
                                  ]
                                }
                                """)));

        GetSubscriptionsResult result = api.getSubscriptions().execute();

        assertThat(result).isNotNull();
        assertThat(result.subscriptions()).hasSize(1);
        assertThat(result.subscriptions().get(0).url()).isEqualTo("https://example.com/webhook");
    }

    @Test
    void subscribe() {
        stubFor(post(urlPathEqualTo("/subscriptions"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        SubscriptionRequestBody body =
                new SubscriptionRequestBody("https://example.com/webhook", null, null);
        SimpleQueryResult result = api.subscribe(body).execute();

        assertThat(result.success()).isTrue();
    }

    @Test
    void unsubscribe() {
        stubFor(delete(urlPathEqualTo("/subscriptions"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        SimpleQueryResult result = api.unsubscribe("https://example.com/webhook").execute();

        assertThat(result.success()).isTrue();
    }

    // ===== Updates =====

    @Test
    void getUpdates() {
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
                                          "user_id": 99001,
                                          "name": "John Doe",
                                          "is_bot": false,
                                          "last_activity_time": 1700000100000
                                        },
                                        "recipient": {"chat_id": 50001, "chat_type": "chat"},
                                        "timestamp": 1700001000000,
                                        "body": {"mid": "msg_upd_001", "seq": 1, "text": "Hi!"}
                                      }
                                    }
                                  ],
                                  "marker": 12345
                                }
                                """)));

        UpdateList list = api.getUpdates().limit(10).timeout(30).execute();

        assertThat(list).isNotNull();
        assertThat(list.updates()).hasSize(1);
        assertThat(list.marker()).isEqualTo(12345L);
    }

    // ===== Uploads =====

    @Test
    void getUploadUrl() {
        stubFor(post(urlPathEqualTo("/uploads"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "url": "https://upload.example.com/files/abc123",
                                  "token": null
                                }
                                """)));

        UploadEndpoint endpoint = api.getUploadUrl(UploadType.IMAGE).execute();

        assertThat(endpoint).isNotNull();
        assertThat(endpoint.url()).isEqualTo("https://upload.example.com/files/abc123");
    }

    // ===== Video =====

    @Test
    void getVideoAttachmentDetails() {
        stubFor(get(urlPathEqualTo("/videos/vtoken"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "url": "https://cdn.example.com/video/abc.mp4",
                                  "token": "vtoken",
                                  "thumbnail": {
                                    "url": "https://cdn.example.com/thumb/abc.jpg"
                                  },
                                  "width": 1280,
                                  "height": 720,
                                  "duration": 60
                                }
                                """)));

        VideoAttachmentDetails details = api.getVideoAttachmentDetails("vtoken").execute();

        assertThat(details).isNotNull();
        assertThat(details.token()).isEqualTo("vtoken");
        assertThat(details.thumbnail().url()).isEqualTo(
                "https://cdn.example.com/thumb/abc.jpg");
        assertThat(details.width()).isEqualTo(1280);
    }

    // ===== Error Scenarios =====

    @Test
    void returns401Unauthorized() {
        stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"code\": \"unauthorized\", \"message\": \"Unauthorized\"}")));

        assertThatThrownBy(() -> api.getMyInfo().execute())
                .isInstanceOf(MaxApiException.class)
                .satisfies(ex -> {
                    MaxApiException apiEx = (MaxApiException) ex;
                    assertThat(apiEx.statusCode()).isEqualTo(401);
                });
    }

    @Test
    void returns404NotFound() {
        stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"code\": \"not_found\", \"message\": \"Not found\"}")));

        assertThatThrownBy(() -> api.getChat(99999L).execute())
                .isInstanceOf(MaxApiException.class)
                .satisfies(ex -> {
                    MaxApiException apiEx = (MaxApiException) ex;
                    assertThat(apiEx.statusCode()).isEqualTo(404);
                });
    }

    @Test
    void returns429TooManyRequests() {
        stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withHeader("Retry-After", "5")
                        .withBody(
                                "{\"code\": \"too_many_requests\", \"message\": \"Too many requests\"}")));

        assertThatThrownBy(() -> api.getMyInfo().execute())
                .isInstanceOf(MaxRateLimitException.class)
                .satisfies(ex -> {
                    MaxRateLimitException rle = (MaxRateLimitException) ex;
                    assertThat(rle.statusCode()).isEqualTo(429);
                    assertThat(rle.retryAfter()).isNotNull();
                });
    }

    @Test
    void returns405MethodNotAllowed() {
        stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                        .withStatus(405)
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody(
                                "{\"code\": \"method_not_allowed\", \"message\": \"Method not allowed\"}")));

        assertThatThrownBy(() -> api.getMyInfo().execute())
                .isInstanceOf(MaxMethodNotAllowedException.class)
                .satisfies(ex -> {
                    MaxApiException apiEx = (MaxApiException) ex;
                    assertThat(apiEx.statusCode()).isEqualTo(405);
                });
    }

    @Test
    void authorizationHeaderIsSent() {
        stubFor(get(urlPathEqualTo("/me"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "user_id": 12345,
                                  "name": "TestBot",
                                  "username": "test_bot",
                                  "is_bot": true,
                                  "last_activity_time": 1700000000000
                                }
                                """)));

        BotInfo info = api.getMyInfo().execute();

        assertThat(info).isNotNull();
        // WireMock would have rejected the request if the Authorization header was missing
    }

    // ===== Additional Coverage Tests =====

    @Test
    void clientMethodReturnsUnderlyingClient() {
        assertThat(api.client()).isNotNull();
    }

    @Test
    void closeDoesNotThrow() {
        // The api here is created with an externally-managed client (no transport stored),
        // so close() is a no-op that should succeed
        api.close();
    }

    @Test
    void getMessagesWithAllParams() {
        stubFor(get(urlPathEqualTo("/messages"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "messages": [],
                                  "marker": null
                                }
                                """)));

        MessageList list = api.getMessages()
                .userId(99001L)
                .from(1700000000000L)
                .to(1700001000000L)
                .count(20)
                .execute();

        assertThat(list).isNotNull();
    }

    @Test
    void getMessagesWithMessageIds() {
        stubFor(get(urlPathEqualTo("/messages"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "messages": [],
                                  "marker": null
                                }
                                """)));

        MessageList list = api.getMessages()
                .messageIds(List.of("mid1", "mid2"))
                .execute();

        assertThat(list).isNotNull();
    }

    @Test
    void getMembersWithUserIdsAndMarker() {
        stubFor(get(urlPathEqualTo("/chats/123/members"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "members": [],
                                  "marker": null
                                }
                                """)));

        ChatMembersList list = api.getMembers(123L)
                .userIds(List.of(99001L, 99002L))
                .marker(5000L)
                .execute();

        assertThat(list).isNotNull();
    }

    @Test
    void getChatsWithMarker() {
        stubFor(get(urlPathEqualTo("/chats"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "chats": [],
                                  "marker": null
                                }
                                """)));

        ChatList list = api.getChats().marker(1000L).execute();

        assertThat(list).isNotNull();
    }

    @Test
    void getUpdatesWithMarker() {
        stubFor(get(urlPathEqualTo("/updates"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "updates": [],
                                  "marker": null
                                }
                                """)));

        UpdateList list = api.getUpdates().marker(9000L).execute();

        assertThat(list).isNotNull();
    }

    @Test
    void sendMessageToUser() {
        stubFor(post(urlPathEqualTo("/messages"))
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
                                    "recipient": {"chat_id": 0, "chat_type": "dialog"},
                                    "timestamp": 1700001000000,
                                    "body": {"mid": "dm_001", "seq": 1, "text": "Hi!"}
                                  }
                                }
                                """)));

        NewMessageBody body = new NewMessageBody("Hi!", null, null, null, null);
        SendMessageResult result = api.sendMessage(body).userId(99001L)
                .disableLinkPreview(true).execute();

        assertThat(result).isNotNull();
    }

    @Test
    void removeMemberWithBlock() {
        stubFor(delete(urlPathEqualTo("/chats/123/members"))
                .withQueryParam("user_id", equalTo("456"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        SimpleQueryResult result = api.removeMember(123L, 456L).block(true).execute();

        assertThat(result.success()).isTrue();
    }

    @Test
    void getUploadUrlSendsTypeQueryParam() {
        stubFor(post(urlPathEqualTo("/uploads"))
                .withQueryParam("type", equalTo("image"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "url": "https://upload.example.com/files/abc123",
                                  "token": null
                                }
                                """)));

        UploadEndpoint endpoint = api.getUploadUrl(UploadType.IMAGE).execute();

        assertThat(endpoint).isNotNull();
        assertThat(endpoint.url()).isEqualTo("https://upload.example.com/files/abc123");
    }

    @Test
    void deleteMessageSendsMessageIdQueryParam() {
        stubFor(delete(urlPathEqualTo("/messages"))
                .withQueryParam("message_id", equalTo("mid1"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        SimpleQueryResult result = api.deleteMessage("mid1").execute();

        assertThat(result.success()).isTrue();
    }

    @Test
    void getChatByLinkEncodesSpacesAsPercent20InPath() {
        // Spaces must be encoded as %20 (RFC 3986 path encoding), not + (form encoding).
        stubFor(get(urlPathEqualTo("/chats/my%20chat"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "chat_id": 55555,
                                  "type": "channel",
                                  "status": "active",
                                  "title": "My Chat",
                                  "last_event_time": 1700000300000,
                                  "participants_count": 1,
                                  "owner_id": 99001,
                                  "is_public": true
                                }
                                """)));

        Chat chat = api.getChatByLink("my chat").execute();

        assertThat(chat).isNotNull();
        assertThat(chat.chatId()).isEqualTo(55555L);
    }

    @Test
    void getChatByLinkEncodesUnicodeCharactersInPath() {
        // Unicode characters must be percent-encoded in path segments.
        // "тест" in UTF-8 is %D1%82%D0%B5%D1%81%D1%82
        stubFor(get(urlPathEqualTo("/chats/%D1%82%D0%B5%D1%81%D1%82"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "chat_id": 55556,
                                  "type": "channel",
                                  "status": "active",
                                  "title": "Unicode Chat",
                                  "last_event_time": 1700000400000,
                                  "participants_count": 5,
                                  "owner_id": 99002,
                                  "is_public": true
                                }
                                """)));

        Chat chat = api.getChatByLink("\u0442\u0435\u0441\u0442").execute();

        assertThat(chat).isNotNull();
        assertThat(chat.chatId()).isEqualTo(55556L);
    }
}
