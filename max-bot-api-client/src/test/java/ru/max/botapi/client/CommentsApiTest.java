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
import ru.max.botapi.model.BotCommand;
import ru.max.botapi.model.BotCommandsPatch;
import ru.max.botapi.model.BotCommandsResult;
import ru.max.botapi.model.CommentList;
import ru.max.botapi.model.CommentMessage;
import ru.max.botapi.model.MessageLinkType;
import ru.max.botapi.model.NewCommentBody;
import ru.max.botapi.model.SendCommentResult;
import ru.max.botapi.model.SimpleQueryResult;
import ru.max.botapi.model.TextFormat;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * WireMock tests for the channel-comments methods and {@code PATCH /me/commands}.
 */
@WireMockTest
class CommentsApiTest {

    private static final String TOKEN = "test-token";
    private static final String AUTH_HEADER = "Authorization";
    private static final String CONTENT_JSON = "application/json";
    private static final String POST_ID = "mid.post123";

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

    @Test
    void getComments() {
        stubFor(get(urlPathEqualTo("/messages/" + POST_ID + "/comments"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .withQueryParam("count", equalTo("2"))
                .withQueryParam("after", equalTo("1700000000000"))
                .withQueryParam("before", equalTo("1700000009000"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "messages": [
                                    {
                                      "sender": {
                                        "user_id": 7,
                                        "first_name": "Ivan",
                                        "is_bot": false,
                                        "last_activity_time": 1700000000000
                                      },
                                      "recipient": {
                                        "chat_id": 42,
                                        "chat_type": "channel",
                                        "post_id": "mid.post123"
                                      },
                                      "timestamp": 1700000001000,
                                      "body": {
                                        "mid": "mid.c1",
                                        "seq": 1,
                                        "text": "Nice post"
                                      }
                                    },
                                    {
                                      "recipient": {
                                        "chat_id": 42,
                                        "chat_type": "channel",
                                        "post_id": "mid.post123"
                                      },
                                      "timestamp": 1700000002000,
                                      "body": {
                                        "mid": "mid.c2",
                                        "seq": 2,
                                        "text": "Posted as the channel"
                                      }
                                    }
                                  ]
                                }
                                """)));

        CommentList comments = api.getComments(POST_ID)
                .count(2)
                .after(1700000000000L)
                .before(1700000009000L)
                .execute();

        assertThat(comments.messages()).hasSize(2);
        CommentMessage first = comments.messages().getFirst();
        assertThat(first.body().text()).isEqualTo("Nice post");
        assertThat(first.recipient().postId()).isEqualTo(POST_ID);
        assertThat(first.sender()).isNotNull();
        // A comment posted as the channel carries no sender.
        assertThat(comments.messages().get(1).sender()).isNull();
    }

    @Test
    void getComments_byIds() {
        stubFor(get(urlPathEqualTo("/messages/" + POST_ID + "/comments"))
                .withQueryParam("comment_ids", equalTo("mid.c1,mid.c2"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"messages\": []}")));

        assertThat(api.getComments(POST_ID)
                .commentIds(List.of("mid.c1", "mid.c2"))
                .execute()
                .messages()).isEmpty();
    }

    @Test
    void getCommentById() {
        stubFor(get(urlPathEqualTo("/messages/" + POST_ID + "/comments/mid.c1"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "recipient": {
                                    "chat_id": 42,
                                    "chat_type": "channel",
                                    "post_id": "mid.post123"
                                  },
                                  "timestamp": 1700000001000,
                                  "link": {
                                    "type": "reply",
                                    "chat_id": 42,
                                    "message": {
                                      "mid": "mid.c0",
                                      "seq": 0,
                                      "text": "The comment replied to"
                                    }
                                  },
                                  "body": {
                                    "mid": "mid.c1",
                                    "seq": 1,
                                    "text": "A reply"
                                  }
                                }
                                """)));

        CommentMessage comment = api.getCommentById(POST_ID, "mid.c1").execute();

        assertThat(comment.body().mid()).isEqualTo("mid.c1");
        assertThat(comment.link()).isNotNull();
        assertThat(comment.link().type()).isEqualTo(MessageLinkType.REPLY);
        assertThat(comment.link().message().text()).isEqualTo("The comment replied to");
    }

    @Test
    void sendComment() {
        stubFor(post(urlPathEqualTo("/messages/" + POST_ID + "/comments"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .withRequestBody(equalToJson("""
                        {"text": "Thanks!", "format": "markdown"}
                        """))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {
                                  "message": {
                                    "recipient": {
                                      "chat_id": 42,
                                      "chat_type": "channel",
                                      "post_id": "mid.post123"
                                    },
                                    "timestamp": 1700000003000,
                                    "body": {"mid": "mid.c9", "seq": 9, "text": "Thanks!"}
                                  }
                                }
                                """)));

        SendCommentResult result = api
                .sendComment(new NewCommentBody("Thanks!", null, TextFormat.MARKDOWN), POST_ID)
                .execute();

        assertThat(result.message().body().mid()).isEqualTo("mid.c9");
    }

    @Test
    void editComment() {
        stubFor(put(urlPathEqualTo("/messages/" + POST_ID + "/comments"))
                .withQueryParam("comment_id", equalTo("mid.c9"))
                .withRequestBody(equalToJson("{\"text\": \"Fixed\"}"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        SimpleQueryResult result = api
                .editComment(new NewCommentBody("Fixed"), POST_ID, "mid.c9")
                .execute();

        assertThat(result.success()).isTrue();
    }

    @Test
    void deleteComment() {
        stubFor(delete(urlPathEqualTo("/messages/" + POST_ID + "/comments"))
                .withQueryParam("comment_id", equalTo("mid.c9"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"success\": true}")));

        assertThat(api.deleteComment(POST_ID, "mid.c9").execute().success()).isTrue();
    }

    @Test
    void editMyCommands() {
        stubFor(patch(urlPathEqualTo("/me/commands"))
                .withHeader(AUTH_HEADER, equalTo(TOKEN))
                .withRequestBody(equalToJson("""
                        {"commands": [{"name": "start", "description": "Start the bot"}]}
                        """))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("""
                                {"commands": [{"name": "start", "description": "Start the bot"}]}
                                """)));

        BotCommandsResult result = api.editMyCommands(new BotCommandsPatch(
                List.of(new BotCommand("start", "Start the bot")))).execute();

        assertThat(result.commands()).hasSize(1);
        assertThat(result.commands().getFirst().name()).isEqualTo("start");
    }

    @Test
    void editMyCommands_emptyListRemovesThemAll() {
        stubFor(patch(urlPathEqualTo("/me/commands"))
                .withRequestBody(equalToJson("{\"commands\": []}"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", CONTENT_JSON)
                        .withBody("{\"commands\": []}")));

        assertThat(api.editMyCommands(new BotCommandsPatch(List.of())).execute().commands())
                .isEmpty();
    }
}