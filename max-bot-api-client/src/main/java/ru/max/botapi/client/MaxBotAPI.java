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

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import ru.max.botapi.client.queries.AddMembersQuery;
import ru.max.botapi.client.queries.AnswerOnCallbackQuery;
import ru.max.botapi.client.queries.DeleteAdminsQuery;
import ru.max.botapi.client.queries.DeleteChatQuery;
import ru.max.botapi.client.queries.DeleteMessageQuery;
import ru.max.botapi.client.queries.EditChatQuery;
import ru.max.botapi.client.queries.EditMessageQuery;
import ru.max.botapi.client.queries.EditMyInfoQuery;
import ru.max.botapi.client.queries.GetAdminsQuery;
import ru.max.botapi.client.queries.GetChatByLinkQuery;
import ru.max.botapi.client.queries.GetChatQuery;
import ru.max.botapi.client.queries.GetChatsQuery;
import ru.max.botapi.client.queries.GetMembersQuery;
import ru.max.botapi.client.queries.GetMembershipQuery;
import ru.max.botapi.client.queries.GetMessageByIdQuery;
import ru.max.botapi.client.queries.GetMessagesQuery;
import ru.max.botapi.client.queries.GetMyInfoQuery;
import ru.max.botapi.client.queries.GetPinnedMessageQuery;
import ru.max.botapi.client.queries.GetSubscriptionsQuery;
import ru.max.botapi.client.queries.GetUpdatesQuery;
import ru.max.botapi.client.queries.GetUploadUrlQuery;
import ru.max.botapi.client.queries.GetVideoDetailsQuery;
import ru.max.botapi.client.queries.LeaveChatQuery;
import ru.max.botapi.client.queries.PinMessageQuery;
import ru.max.botapi.client.queries.PostAdminsQuery;
import ru.max.botapi.client.queries.RemoveMemberQuery;
import ru.max.botapi.client.queries.SendActionQuery;
import ru.max.botapi.client.queries.SendMessageQuery;
import ru.max.botapi.client.queries.SubscribeQuery;
import ru.max.botapi.client.queries.UnpinMessageQuery;
import ru.max.botapi.client.queries.UnsubscribeQuery;
import ru.max.botapi.core.MaxSerializer;
import ru.max.botapi.model.ActionRequestBody;
import ru.max.botapi.model.BotPatch;
import ru.max.botapi.model.CallbackAnswer;
import ru.max.botapi.model.ChatAdminsList;
import ru.max.botapi.model.ChatPatch;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.PinMessageBody;
import ru.max.botapi.model.SubscriptionRequestBody;
import ru.max.botapi.model.UploadType;
import ru.max.botapi.model.UserIdsList;

/**
 * High-level facade for the MAX Bot API.
 *
 * <p>Provides factory methods for all 31 API endpoints as query builder objects.
 * Use {@link #create(String)} to create an instance with default configuration, or
 * {@link #create(String, MaxClientConfig)} for custom settings.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MaxBotAPI api = MaxBotAPI.create("my-access-token");
 * BotInfo info = api.getMyInfo().execute();
 * }</pre>
 *
 * <p>This class implements {@link AutoCloseable} to allow use in try-with-resources blocks,
 * which will shut down the underlying HTTP transport.</p>
 */
public class MaxBotAPI implements AutoCloseable {

    private static final String JACKSON_SERIALIZER_CLASS =
            "ru.max.botapi.jackson.JacksonMaxSerializer";

    private final MaxClient client;
    private final MaxTransportClient transport;
    private final MaxClientConfig config;

    /**
     * Creates a {@code MaxBotAPI} wrapping an existing {@link MaxClient}.
     *
     * <p>Use this constructor when you need full control over transport and serializer
     * configuration.</p>
     *
     * @param client the configured MAX client; must not be {@code null}
     */
    public MaxBotAPI(MaxClient client) {
        this(client, null, MaxClientConfig.defaults());
    }

    /**
     * Creates a {@code MaxBotAPI} with an internal client and transport (used by factory methods).
     *
     * @param client    the configured MAX client; must not be {@code null}
     * @param transport the transport to close when this instance is closed; may be {@code null}
     * @param config    the client configuration; must not be {@code null}
     */
    private MaxBotAPI(MaxClient client, MaxTransportClient transport, MaxClientConfig config) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.transport = transport;
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    /**
     * Returns the configuration used to create this instance.
     *
     * @return the client configuration; never {@code null}
     */
    public MaxClientConfig config() {
        return config;
    }

    /**
     * Creates a {@code MaxBotAPI} with default configuration.
     *
     * <p>Internally creates a {@link JdkHttpMaxTransportClient} and attempts to load
     * {@code ru.max.botapi.jackson.JacksonMaxSerializer} via reflection (requires
     * {@code max-bot-api-jackson} on the classpath).</p>
     *
     * @param accessToken the bot access token; must not be {@code null}
     * @return a new MaxBotAPI instance
     * @throws MaxClientException if the Jackson serializer cannot be instantiated
     */
    public static MaxBotAPI create(String accessToken) {
        return create(accessToken, MaxClientConfig.defaults());
    }

    /**
     * Creates a {@code MaxBotAPI} with the given configuration.
     *
     * <p>Internally creates a {@link JdkHttpMaxTransportClient} and attempts to load
     * {@code ru.max.botapi.jackson.JacksonMaxSerializer} via reflection (requires
     * {@code max-bot-api-jackson} on the classpath).</p>
     *
     * @param accessToken the bot access token; must not be {@code null}
     * @param config      the client configuration; must not be {@code null}
     * @return a new MaxBotAPI instance
     * @throws MaxClientException if the Jackson serializer cannot be instantiated
     */
    public static MaxBotAPI create(String accessToken, MaxClientConfig config) {
        Objects.requireNonNull(accessToken, "accessToken must not be null");
        Objects.requireNonNull(config, "config must not be null");
        MaxSerializer serializer = loadJacksonSerializer();
        JdkHttpMaxTransportClient transport = new JdkHttpMaxTransportClient(accessToken, config);
        MaxClient client = new MaxClient(transport, serializer, config);
        return new MaxBotAPI(client, transport, config);
    }

    /**
     * Loads the Jackson serializer via reflection to avoid a compile-time dependency on Jackson.
     *
     * @return the instantiated serializer
     * @throws MaxClientException if the class cannot be found or instantiated
     */
    private static MaxSerializer loadJacksonSerializer() {
        try {
            Class<?> clazz = Class.forName(JACKSON_SERIALIZER_CLASS);
            return (MaxSerializer) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new MaxClientException(
                    "Jackson serializer not found on classpath. "
                    + "Add 'max-bot-api-jackson' dependency or supply a custom MaxSerializer.", e);
        } catch (InvocationTargetException | InstantiationException
                | IllegalAccessException | NoSuchMethodException e) {
            throw new MaxClientException(
                    "Failed to instantiate Jackson serializer: " + JACKSON_SERIALIZER_CLASS, e);
        }
    }

    // ===== Bot Methods =====

    /**
     * Returns a query for {@code GET /me} that retrieves the bot's profile information.
     *
     * @return a {@link GetMyInfoQuery}
     */
    public GetMyInfoQuery getMyInfo() {
        return new GetMyInfoQuery(client);
    }

    /**
     * Returns a query for {@code PATCH /me} that updates the bot's profile information.
     *
     * @param botPatch the patch with fields to update; must not be {@code null}
     * @return an {@link EditMyInfoQuery}
     */
    public EditMyInfoQuery editMyInfo(BotPatch botPatch) {
        return new EditMyInfoQuery(client, botPatch);
    }

    // ===== Chat Methods =====

    /**
     * Returns a query for {@code GET /chats} that retrieves the list of chats.
     *
     * @return a {@link GetChatsQuery}
     */
    public GetChatsQuery getChats() {
        return new GetChatsQuery(client);
    }

    /**
     * Returns a query for {@code GET /chats/{chatId}} that retrieves a specific chat.
     *
     * @param chatId the chat identifier
     * @return a {@link GetChatQuery}
     */
    public GetChatQuery getChat(long chatId) {
        return new GetChatQuery(client, chatId);
    }

    /**
     * Returns a query for {@code GET /chats/{chatLink}} that retrieves a chat by its public link.
     *
     * @param chatLink the public chat link or username; must not be {@code null}
     * @return a {@link GetChatByLinkQuery}
     */
    public GetChatByLinkQuery getChatByLink(String chatLink) {
        return new GetChatByLinkQuery(client, chatLink);
    }

    /**
     * Returns a query for {@code PATCH /chats/{chatId}} that edits chat properties.
     *
     * @param chatPatch the patch with fields to update; must not be {@code null}
     * @param chatId    the chat identifier
     * @return an {@link EditChatQuery}
     */
    public EditChatQuery editChat(ChatPatch chatPatch, long chatId) {
        return new EditChatQuery(client, chatPatch, chatId);
    }

    /**
     * Returns a query for {@code DELETE /chats/{chatId}} that deletes a chat.
     *
     * @param chatId the chat identifier
     * @return a {@link DeleteChatQuery}
     */
    public DeleteChatQuery deleteChat(long chatId) {
        return new DeleteChatQuery(client, chatId);
    }

    /**
     * Returns a query for {@code POST /chats/{chatId}/actions} that sends a typing or other
     * action indicator.
     *
     * @param body   the action request body; must not be {@code null}
     * @param chatId the chat identifier
     * @return a {@link SendActionQuery}
     */
    public SendActionQuery sendAction(ActionRequestBody body, long chatId) {
        return new SendActionQuery(client, body, chatId);
    }

    // ===== Chat Members =====

    /**
     * Returns a query for {@code GET /chats/{chatId}/members} that retrieves the chat member list.
     *
     * @param chatId the chat identifier
     * @return a {@link GetMembersQuery}
     */
    public GetMembersQuery getMembers(long chatId) {
        return new GetMembersQuery(client, chatId);
    }

    /**
     * Returns a query for {@code POST /chats/{chatId}/members} that adds members to a chat.
     *
     * @param userIds the list of user IDs to add; must not be {@code null}
     * @param chatId  the chat identifier
     * @return an {@link AddMembersQuery}
     */
    public AddMembersQuery addMembers(UserIdsList userIds, long chatId) {
        return new AddMembersQuery(client, userIds, chatId);
    }

    /**
     * Returns a query for {@code DELETE /chats/{chatId}/members} that removes a member from
     * a chat.
     *
     * <p>Both {@code chatId} and {@code userId} are required by the MAX API. Use the returned
     * query's {@link RemoveMemberQuery#block(boolean)} to optionally block the user.</p>
     *
     * @param chatId the chat identifier
     * @param userId the user identifier of the member to remove
     * @return a {@link RemoveMemberQuery}
     */
    public RemoveMemberQuery removeMember(long chatId, long userId) {
        return new RemoveMemberQuery(client, chatId, userId);
    }

    /**
     * Returns a query for {@code GET /chats/{chatId}/members/me} that retrieves the bot's
     * own membership info.
     *
     * @param chatId the chat identifier
     * @return a {@link GetMembershipQuery}
     */
    public GetMembershipQuery getMembership(long chatId) {
        return new GetMembershipQuery(client, chatId);
    }

    /**
     * Returns a query for {@code DELETE /chats/{chatId}/members/me} that removes the bot
     * from a chat.
     *
     * @param chatId the chat identifier
     * @return a {@link LeaveChatQuery}
     */
    public LeaveChatQuery leaveChat(long chatId) {
        return new LeaveChatQuery(client, chatId);
    }

    /**
     * Returns a query for {@code GET /chats/{chatId}/members/admins} that retrieves the admin list.
     *
     * @param chatId the chat identifier
     * @return a {@link GetAdminsQuery}
     */
    public GetAdminsQuery getAdmins(long chatId) {
        return new GetAdminsQuery(client, chatId);
    }

    /**
     * Returns a query for {@code POST /chats/{chatId}/members/admins} that promotes members
     * to admin.
     *
     * @param adminsList the list of admins to promote; must not be {@code null}
     * @param chatId     the chat identifier
     * @return a {@link PostAdminsQuery}
     */
    public PostAdminsQuery postAdmins(ChatAdminsList adminsList, long chatId) {
        return new PostAdminsQuery(client, adminsList, chatId);
    }

    /**
     * Returns a query for {@code DELETE /chats/{chatId}/members/admins/{userId}} that demotes
     * an admin.
     *
     * @param chatId the chat identifier
     * @param userId the user identifier of the admin to demote
     * @return a {@link DeleteAdminsQuery}
     */
    public DeleteAdminsQuery deleteAdmins(long chatId, long userId) {
        return new DeleteAdminsQuery(client, chatId, userId);
    }

    // ===== Pin =====

    /**
     * Returns a query for {@code GET /chats/{chatId}/pin} that retrieves the pinned message.
     *
     * @param chatId the chat identifier
     * @return a {@link GetPinnedMessageQuery}
     */
    public GetPinnedMessageQuery getPinnedMessage(long chatId) {
        return new GetPinnedMessageQuery(client, chatId);
    }

    /**
     * Returns a query for {@code PUT /chats/{chatId}/pin} that pins a message in a chat.
     *
     * @param body   the pin message body; must not be {@code null}
     * @param chatId the chat identifier
     * @return a {@link PinMessageQuery}
     */
    public PinMessageQuery pinMessage(PinMessageBody body, long chatId) {
        return new PinMessageQuery(client, body, chatId);
    }

    /**
     * Returns a query for {@code DELETE /chats/{chatId}/pin} that unpins the current message.
     *
     * @param chatId the chat identifier
     * @return an {@link UnpinMessageQuery}
     */
    public UnpinMessageQuery unpinMessage(long chatId) {
        return new UnpinMessageQuery(client, chatId);
    }

    // ===== Messages =====

    /**
     * Returns a query for {@code GET /messages} that retrieves messages with optional filters.
     *
     * @return a {@link GetMessagesQuery}
     */
    public GetMessagesQuery getMessages() {
        return new GetMessagesQuery(client);
    }

    /**
     * Returns a query for {@code GET /messages/{messageId}} that retrieves a specific message.
     *
     * @param messageId the message identifier; must not be {@code null}
     * @return a {@link GetMessageByIdQuery}
     */
    public GetMessageByIdQuery getMessageById(String messageId) {
        return new GetMessageByIdQuery(client, messageId);
    }

    /**
     * Returns a query for {@code POST /messages} that sends a message.
     *
     * <p>Use the returned query's {@code .chatId(long)} or {@code .userId(long)} to specify
     * the recipient.</p>
     *
     * @param body the message body; must not be {@code null}
     * @return a {@link SendMessageQuery}
     */
    public SendMessageQuery sendMessage(NewMessageBody body) {
        return new SendMessageQuery(client, body);
    }

    /**
     * Returns a query for {@code PUT /messages} that edits an existing message.
     *
     * <p>{@code messageId} is required: there is no edit operation without specifying the
     * target message.</p>
     *
     * @param body      the updated message body; must not be {@code null}
     * @param messageId the ID of the message to edit; must not be {@code null}
     * @return an {@link EditMessageQuery}
     */
    public EditMessageQuery editMessage(NewMessageBody body, String messageId) {
        return new EditMessageQuery(client, body, messageId);
    }

    /**
     * Returns a query for {@code DELETE /messages} that deletes a message.
     *
     * @param messageId the ID of the message to delete; must not be {@code null}
     * @return a {@link DeleteMessageQuery}
     */
    public DeleteMessageQuery deleteMessage(String messageId) {
        return new DeleteMessageQuery(client, messageId);
    }

    // ===== Callback =====

    /**
     * Returns a query for {@code POST /answers} that answers a callback from an inline button.
     *
     * <p>{@code callbackId} is required by the MAX API. It identifies the callback interaction
     * to acknowledge.</p>
     *
     * @param answer     the callback answer; must not be {@code null}
     * @param callbackId the ID of the callback to answer; must not be {@code null}
     * @return an {@link AnswerOnCallbackQuery}
     */
    public AnswerOnCallbackQuery answerOnCallback(CallbackAnswer answer, String callbackId) {
        return new AnswerOnCallbackQuery(client, answer, callbackId);
    }

    // ===== Subscriptions =====

    /**
     * Returns a query for {@code GET /subscriptions} that lists active webhook subscriptions.
     *
     * @return a {@link GetSubscriptionsQuery}
     */
    public GetSubscriptionsQuery getSubscriptions() {
        return new GetSubscriptionsQuery(client);
    }

    /**
     * Returns a query for {@code POST /subscriptions} that registers a webhook URL.
     *
     * @param body the subscription request body; must not be {@code null}
     * @return a {@link SubscribeQuery}
     */
    public SubscribeQuery subscribe(SubscriptionRequestBody body) {
        return new SubscribeQuery(client, body);
    }

    /**
     * Returns a query for {@code DELETE /subscriptions} that removes a webhook subscription.
     *
     * @param url the webhook URL to unsubscribe; must not be {@code null}
     * @return an {@link UnsubscribeQuery}
     */
    public UnsubscribeQuery unsubscribe(String url) {
        return new UnsubscribeQuery(client, url);
    }

    // ===== Updates (Long Polling) =====

    /**
     * Returns a query for {@code GET /updates} that long-polls for incoming updates.
     *
     * @return a {@link GetUpdatesQuery}
     */
    public GetUpdatesQuery getUpdates() {
        return new GetUpdatesQuery(client);
    }

    // ===== Uploads =====

    /**
     * Returns a query for {@code POST /uploads} that requests an upload URL for a file.
     *
     * @param type the upload type (image, video, audio, or file); must not be {@code null}
     * @return a {@link GetUploadUrlQuery}
     */
    public GetUploadUrlQuery getUploadUrl(UploadType type) {
        return new GetUploadUrlQuery(client, type);
    }

    // ===== Video =====

    /**
     * Returns a query for {@code GET /videos/{videoToken}} that retrieves video attachment details.
     *
     * @param videoToken the video token; must not be {@code null}
     * @return a {@link GetVideoDetailsQuery}
     */
    public GetVideoDetailsQuery getVideoAttachmentDetails(String videoToken) {
        return new GetVideoDetailsQuery(client, videoToken);
    }

    /**
     * Returns the underlying {@link MaxClient} for advanced use cases.
     *
     * @return the MAX client
     */
    public MaxClient client() {
        return client;
    }

    /**
     * Closes the underlying HTTP transport, releasing all resources.
     *
     * <p>After calling this method, any queries obtained from this instance will fail.</p>
     */
    @Override
    public void close() {
        if (transport != null) {
            transport.close();
        }
    }
}
