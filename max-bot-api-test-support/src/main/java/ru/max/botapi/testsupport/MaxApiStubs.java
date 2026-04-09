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

package ru.max.botapi.testsupport;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

/**
 * WireMock stub factory for the MAX Bot API.
 *
 * <p>Provides static methods that register WireMock stubs for all 31 API endpoints.
 * Each stub verifies the {@code Authorization} header and returns the appropriate
 * JSON fixture response.
 *
 * <p>Usage example:
 * <pre>{@code
 * @WireMockTest
 * class MyApiTest {
 *     @Test
 *     void testGetMyInfo() {
 *         MaxApiStubs.stubGetMyInfo();
 *         // ... call API through MaxBotAPI and assert results
 *     }
 * }
 * }</pre>
 */
public final class MaxApiStubs {

    private static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN_PATTERN = ".+";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private MaxApiStubs() {
        // utility class
    }

    // ===== Bot Methods =====

    /**
     * Stubs {@code GET /me} — returns bot info fixture.
     *
     * @return the registered stub mapping
     */
    public static StubMapping stubGetMyInfo() {
        return stubFor(get(urlPathEqualTo("/me"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("bot/bot-info.json"))));
    }

    /**
     * Stubs {@code PATCH /me} — returns updated bot info fixture.
     *
     * @return the registered stub mapping
     */
    public static StubMapping stubEditMyInfo() {
        return stubFor(patch(urlPathEqualTo("/me"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("bot/bot-info.json"))));
    }

    // ===== Chat Methods =====

    /**
     * Stubs {@code GET /chats} — returns chat list fixture.
     *
     * @return the registered stub mapping
     */
    public static StubMapping stubGetChats() {
        return stubFor(get(urlPathEqualTo("/chats"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("chats/chat-list.json"))));
    }

    /**
     * Stubs {@code GET /chats/{chatId}} — returns single chat fixture.
     *
     * @param chatId the chat ID to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubGetChat(long chatId) {
        return stubFor(get(urlPathEqualTo("/chats/" + chatId))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("chats/chat.json"))));
    }

    /**
     * Stubs {@code GET /chats/{chatLink}} — returns single chat fixture by link.
     *
     * @param link the public chat link to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubGetChatByLink(String link) {
        return stubFor(get(urlPathEqualTo("/chats/" + link))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("chats/chat.json"))));
    }

    /**
     * Stubs {@code PATCH /chats/{chatId}} — returns updated chat fixture.
     *
     * @param chatId the chat ID to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubEditChat(long chatId) {
        return stubFor(patch(urlPathEqualTo("/chats/" + chatId))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("chats/chat.json"))));
    }

    /**
     * Stubs {@code DELETE /chats/{chatId}} — returns simple success result.
     *
     * @param chatId the chat ID to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubDeleteChat(long chatId) {
        return stubFor(delete(urlPathEqualTo("/chats/" + chatId))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("simple-result.json"))));
    }

    /**
     * Stubs {@code POST /chats/{chatId}/actions} — returns simple success result.
     *
     * @param chatId the chat ID to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubSendAction(long chatId) {
        return stubFor(post(urlPathEqualTo("/chats/" + chatId + "/actions"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("simple-result.json"))));
    }

    /**
     * Stubs {@code GET /chats/{chatId}/members} — returns members list fixture.
     *
     * @param chatId the chat ID to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubGetMembers(long chatId) {
        return stubFor(get(urlPathEqualTo("/chats/" + chatId + "/members"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("chats/chat-members-list.json"))));
    }

    /**
     * Stubs {@code POST /chats/{chatId}/members} — returns success result.
     *
     * @param chatId the chat ID to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubAddMembers(long chatId) {
        return stubFor(post(urlPathEqualTo("/chats/" + chatId + "/members"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture(
                                "chats/add-members-success.json"))));
    }

    /**
     * Stubs {@code POST /chats/{chatId}/members} — returns partial failure
     * (e.g. due to user privacy settings).
     *
     * @param chatId the chat ID to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubAddMembersFailure(long chatId) {
        return stubFor(post(urlPathEqualTo("/chats/" + chatId + "/members"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture(
                                "chats/add-members-partial-failure.json"))));
    }

    /**
     * Stubs {@code DELETE /chats/{chatId}/members} — returns simple success result.
     *
     * @param chatId the chat ID to match in the URL path
     * @param userId the user ID passed as query parameter
     * @return the registered stub mapping
     */
    public static StubMapping stubRemoveMember(long chatId, long userId) {
        return stubFor(delete(urlPathEqualTo("/chats/" + chatId + "/members"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("simple-result.json"))));
    }

    /**
     * Stubs {@code GET /chats/{chatId}/members/me} — returns membership fixture.
     *
     * @param chatId the chat ID to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubGetMembership(long chatId) {
        return stubFor(get(urlPathEqualTo("/chats/" + chatId + "/members/me"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("membership.json"))));
    }

    /**
     * Stubs {@code DELETE /chats/{chatId}/members/me} — returns simple success result.
     *
     * @param chatId the chat ID to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubLeaveChat(long chatId) {
        return stubFor(delete(urlPathEqualTo("/chats/" + chatId + "/members/me"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("simple-result.json"))));
    }

    // ===== Admin Methods =====

    /**
     * Stubs {@code GET /chats/{chatId}/members/admins} — returns admin list fixture.
     *
     * @param chatId the chat ID to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubGetAdmins(long chatId) {
        return stubFor(get(urlPathEqualTo("/chats/" + chatId + "/members/admins"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("chats/chat-admins.json"))));
    }

    /**
     * Stubs {@code POST /chats/{chatId}/members/admins} — returns simple success result.
     *
     * @param chatId the chat ID to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubPostAdmins(long chatId) {
        return stubFor(post(urlPathEqualTo("/chats/" + chatId + "/members/admins"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("simple-result.json"))));
    }

    /**
     * Stubs {@code DELETE /chats/{chatId}/members/admins/{userId}} — returns simple success result.
     *
     * @param chatId the chat ID to match in the URL path
     * @param userId the user ID to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubDeleteAdmins(long chatId, long userId) {
        return stubFor(delete(urlPathEqualTo("/chats/" + chatId + "/members/admins/" + userId))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("simple-result.json"))));
    }

    // ===== Pin Methods =====

    /**
     * Stubs {@code GET /chats/{chatId}/pin} — returns pinned message fixture.
     *
     * @param chatId the chat ID to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubGetPinnedMessage(long chatId) {
        return stubFor(get(urlPathEqualTo("/chats/" + chatId + "/pin"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("pinned-message.json"))));
    }

    /**
     * Stubs {@code PUT /chats/{chatId}/pin} — returns simple success result.
     *
     * @param chatId the chat ID to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubPinMessage(long chatId) {
        return stubFor(put(urlPathEqualTo("/chats/" + chatId + "/pin"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("simple-result.json"))));
    }

    /**
     * Stubs {@code DELETE /chats/{chatId}/pin} — returns simple success result.
     *
     * @param chatId the chat ID to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubUnpinMessage(long chatId) {
        return stubFor(delete(urlPathEqualTo("/chats/" + chatId + "/pin"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("simple-result.json"))));
    }

    // ===== Message Methods =====

    /**
     * Stubs {@code GET /messages} — returns message list fixture.
     *
     * @return the registered stub mapping
     */
    public static StubMapping stubGetMessages() {
        return stubFor(get(urlPathEqualTo("/messages"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("messages/message-list.json"))));
    }

    /**
     * Stubs {@code GET /messages/{messageId}} — returns single message fixture.
     *
     * @param messageId the message ID to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubGetMessageById(String messageId) {
        return stubFor(get(urlPathEqualTo("/messages/" + messageId))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("messages/message-text.json"))));
    }

    /**
     * Stubs {@code POST /messages} — returns send message result fixture.
     *
     * @return the registered stub mapping
     */
    public static StubMapping stubSendMessage() {
        return stubFor(post(urlPathEqualTo("/messages"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("messages/send-message-result.json"))));
    }

    /**
     * Stubs {@code PUT /messages} — returns simple success result.
     *
     * @return the registered stub mapping
     */
    public static StubMapping stubEditMessage() {
        return stubFor(put(urlPathEqualTo("/messages"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("simple-result.json"))));
    }

    /**
     * Stubs {@code DELETE /messages} — returns simple success result.
     *
     * @return the registered stub mapping
     */
    public static StubMapping stubDeleteMessage() {
        return stubFor(delete(urlPathEqualTo("/messages"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("simple-result.json"))));
    }

    // ===== Callback =====

    /**
     * Stubs {@code POST /answers} — returns simple success result.
     *
     * @return the registered stub mapping
     */
    public static StubMapping stubAnswerOnCallback() {
        return stubFor(post(urlPathEqualTo("/answers"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("simple-result.json"))));
    }

    // ===== Subscriptions =====

    /**
     * Stubs {@code GET /subscriptions} — returns subscriptions list fixture.
     *
     * @return the registered stub mapping
     */
    public static StubMapping stubGetSubscriptions() {
        return stubFor(get(urlPathEqualTo("/subscriptions"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture(
                                "subscriptions/subscriptions-list.json"))));
    }

    /**
     * Stubs {@code POST /subscriptions} — returns simple success result.
     *
     * @return the registered stub mapping
     */
    public static StubMapping stubSubscribe() {
        return stubFor(post(urlPathEqualTo("/subscriptions"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("simple-result.json"))));
    }

    /**
     * Stubs {@code DELETE /subscriptions} — returns simple success result.
     *
     * @return the registered stub mapping
     */
    public static StubMapping stubUnsubscribe() {
        return stubFor(delete(urlPathEqualTo("/subscriptions"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("simple-result.json"))));
    }

    // ===== Updates =====

    /**
     * Stubs {@code GET /updates} — returns update list fixture.
     *
     * @return the registered stub mapping
     */
    public static StubMapping stubGetUpdates() {
        return stubFor(get(urlPathEqualTo("/updates"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("updates/update-list.json"))));
    }

    // ===== Uploads =====

    /**
     * Stubs {@code POST /uploads} — returns upload endpoint fixture.
     *
     * @return the registered stub mapping
     */
    public static StubMapping stubGetUploadUrl() {
        return stubFor(post(urlPathEqualTo("/uploads"))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("uploads/upload-endpoint.json"))));
    }

    // ===== Video =====

    /**
     * Stubs {@code GET /videos/{videoToken}} — returns video attachment details fixture.
     *
     * @param videoToken the video token to match in the URL path
     * @return the registered stub mapping
     */
    public static StubMapping stubGetVideoDetails(String videoToken) {
        return stubFor(get(urlPathEqualTo("/videos/" + videoToken))
                .withHeader(AUTH_HEADER, matching(TOKEN_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture("video-details.json"))));
    }

    // ===== Error Scenarios =====

    /**
     * Stubs any request to return the specified HTTP error status with the corresponding
     * error fixture.
     *
     * @param status HTTP status code (400, 401, 404, 429, or 503)
     * @return the registered stub mapping
     * @throws IllegalArgumentException if no fixture exists for the given status
     */
    public static StubMapping stubError(int status) {
        String fixturePath = switch (status) {
            case 400 -> "errors/error-400.json";
            case 401 -> "errors/error-401.json";
            case 404 -> "errors/error-404.json";
            case 429 -> "errors/error-429.json";
            case 503 -> "errors/error-503.json";
            default -> throw new IllegalArgumentException(
                    "No error fixture for status: " + status);
        };
        return stubFor(any(urlPathMatching("/.*"))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(FixtureLoader.loadFixture(fixturePath))));
    }

    // ===== Internal helper =====

    private static com.github.tomakehurst.wiremock.matching.StringValuePattern matching(
            String regex) {
        return com.github.tomakehurst.wiremock.client.WireMock.matching(regex);
    }
}
