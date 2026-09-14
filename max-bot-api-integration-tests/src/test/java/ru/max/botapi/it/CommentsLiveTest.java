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

package ru.max.botapi.it;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import ru.max.botapi.client.MaxApiException;
import ru.max.botapi.longpolling.MaxLongPollingConsumer;
import ru.max.botapi.model.ChatMember;
import ru.max.botapi.model.ChatMembersList;
import ru.max.botapi.model.ChatPermission;
import ru.max.botapi.model.CommentList;
import ru.max.botapi.model.CommentMessage;
import ru.max.botapi.model.CommentRemovedUpdate;
import ru.max.botapi.model.NewCommentBody;
import ru.max.botapi.model.SendCommentResult;
import ru.max.botapi.model.SimpleQueryResult;
import ru.max.botapi.model.UnknownUpdate;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the channel-comment methods against the live API.
 *
 * <p>Needs {@code MAX_IT_CHANNEL_ID} and {@code MAX_IT_POST_ID}: a channel where the bot is an
 * administrator holding {@code read_all_messages}, {@code write}, {@code edit} and
 * {@code delete}, and a post in it whose comments are enabled. Without them every test here
 * reports a skip naming the variable.</p>
 *
 * <p>The test posts a comment, reads it back, edits it and deletes it again, so it leaves
 * nothing behind — but the comment is briefly visible to the channel's subscribers.</p>
 *
 * <p>The last step needs a person. MAX sends a bot no comment events for its own actions, so
 * the bot deleting its comment can never produce {@code comment_removed}; a channel
 * administrator deletes a second comment by hand while the step listens for the event.</p>
 */
@Order(10)
@DisplayName("Live: channel comments")
class CommentsLiveTest extends LiveTestBase {

    private static final int POLL_TIMEOUT_SECONDS = 5;

    private static final int REMOVAL_WAIT_SECONDS = 120;

    /** Every property the schema gives {@code comment_removed}. */
    private static final Set<String> COMMENT_REMOVED_FIELDS = Set.of(
            "update_type", "timestamp", "message_id", "chat_id", "user_id", "post_id");

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private String commentId;

    /** The comment a person is asked to delete; {@code null} once it is gone. */
    private String removableCommentId;

    @Test
    @Order(1)
    @DisplayName("the bot holds the rights the comment methods need")
    void botIsAdminWithCommentRights() {
        ChatMembersList admins = api().getAdmins(IntegrationConfig.channelId()).execute();

        ChatMember bot = admins.members().stream()
                .filter(ChatMember::isBot)
                .findFirst()
                .orElse(null);

        assertThat(bot)
                .withFailMessage("the bot is not an administrator of MAX_IT_CHANNEL_ID")
                .isNotNull();
        assertThat(bot.permissions())
                .withFailMessage("the bot holds no permissions in MAX_IT_CHANNEL_ID")
                .isNotNull();

        // MAX may answer with either the current or the former name of a renamed permission.
        List<ChatPermission> held = bot.permissions().stream()
                .map(ChatPermission::canonical)
                .toList();
        System.out.println("Bot rights in the channel: " + held);
        assertThat(held).contains(ChatPermission.READ_ALL_MESSAGES);
    }

    @Test
    @Order(2)
    @DisplayName("sendComment posts a comment on the configured post")
    void sendComment() {
        NewCommentBody body = new NewCommentBody(
                "Live integration test " + System.currentTimeMillis());

        SendCommentResult result;
        try {
            result = api().sendComment(body, IntegrationConfig.postId()).execute();
        } catch (MaxApiException e) {
            // Posting needs the channel's comment option switched on. That is a property of the
            // channel, not of this library, so it is reported as a skip like every other
            // unconfigured prerequisite in this suite.
            if (String.valueOf(e.errorMessage()).contains("comments_disabled")) {
                throw skipCommentsDisabled(e.errorMessage());
            }
            throw e;
        }

        CommentMessage comment = result.message();
        assertThat(comment.body().mid()).isNotBlank();
        assertThat(comment.recipient().postId())
                .withFailMessage("recipient.post_id was not mapped; raw response drift?")
                .isNotNull();

        commentId = comment.body().mid();
    }

    /**
     * Aborts the current step unless {@link #sendComment()} produced a comment to work on.
     */
    private void requireComment() {
        Assumptions.assumeTrue(commentId != null,
                "sendComment did not produce a comment; see its result for the reason");
    }

    /**
     * Builds the abort that reports a channel with its comment option switched off.
     *
     * @param reason the error message the API answered with
     * @return never returns; {@link Assumptions#abort} always throws
     */
    private static RuntimeException skipCommentsDisabled(String reason) {
        return Assumptions.abort("comments are disabled for the post in "
                + IntegrationConfig.CHANNEL_ID + ": " + reason
                + ". Switch comments on in the channel settings, or point "
                + IntegrationConfig.POST_ID + " at a post in a channel that has them.");
    }

    @Test
    @Order(3)
    @DisplayName("getComments lists the comment that was just posted")
    void getComments() {
        requireComment();

        CommentList comments = api().getComments(IntegrationConfig.postId())
                .count(100)
                .execute();

        assertThat(comments.messages())
                .extracting(comment -> comment.body().mid())
                .contains(commentId);
    }

    @Test
    @Order(4)
    @DisplayName("getCommentById returns the comment by its mid")
    void getCommentById() {
        requireComment();

        CommentMessage comment =
                api().getCommentById(IntegrationConfig.postId(), commentId).execute();

        assertThat(comment.body().mid()).isEqualTo(commentId);
        assertThat(comment.timestamp()).isPositive();
    }

    @Test
    @Order(5)
    @DisplayName("editComment rewrites the comment text")
    void editComment() {
        requireComment();

        SimpleQueryResult result = api()
                .editComment(new NewCommentBody("Live integration test (edited)"),
                        IntegrationConfig.postId(), commentId)
                .execute();

        assertThat(result.success()).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("deleteComment removes the comment again")
    void deleteComment() {
        requireComment();

        SimpleQueryResult result =
                api().deleteComment(IntegrationConfig.postId(), commentId).execute();

        assertThat(result.success()).isTrue();
    }

    @Test
    @Order(7)
    @DisplayName("a comment a person deletes arrives as comment_removed with its identifiers")
    void commentRemovedByAPerson() {
        Assumptions.assumeTrue(IntegrationConfig.interactive(),
                () -> "interactive step skipped (set " + IntegrationConfig.INTERACTIVE
                        + "=true to run it): a person deletes a comment the bot posted, "
                        + "so that comment_removed can be observed");
        // Long polling only sees what is not being diverted to a webhook.
        clearOwnWebhookSubscriptions();

        String text = "Live suite: delete this comment " + System.currentTimeMillis();
        String target = sendCommentOrSkip(text);
        removableCommentId = target;
        long botId = api().getMyInfo().execute().userId();

        AtomicReference<Update> event = new AtomicReference<>();
        AtomicReference<String> batchJson = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
                .api(api())
                .pollTimeout(POLL_TIMEOUT_SECONDS)
                .types(Set.of(UpdateType.COMMENT_REMOVED))
                .handler(update -> {
                    // A payload that has drifted from the model arrives as UnknownUpdate; take
                    // that too, so the step fails with its JSON instead of timing out.
                    boolean ours = update instanceof CommentRemovedUpdate removed
                            ? target.equals(removed.messageId())
                            : update instanceof UnknownUpdate unknown
                                    && "comment_removed".equals(unknown.updateType());
                    if (ours && event.compareAndSet(null, update)) {
                        // The handler runs on the thread that deserialized this batch.
                        batchJson.set(RecordingSerializer.lastJson());
                        latch.countDown();
                    }
                })
                .onError(e -> System.out.println("Long polling error: " + e))
                .build();

        consumer.start();
        try {
            Prompts.step("the test CHANNEL - " + IntegrationConfig.CHANNEL_ID + " = "
                            + IntegrationConfig.channelId(),
                    "Open the comments of post " + IntegrationConfig.postId()
                    + " and delete the bot's comment \"" + text + "\" yourself, as a channel "
                    + "administrator. MAX does not tell a bot about its own deletions.");
            awaitRemoval(latch, target);
        } finally {
            consumer.stop();
        }

        Update update = event.get();
        ModelAssertions.assertFullyMapped(update);
        CommentRemovedUpdate removed = (CommentRemovedUpdate) update;
        removableCommentId = null;
        System.out.println("comment_removed arrived as " + removed);

        assertThat(removed.messageId()).isEqualTo(target);
        assertThat(removed.chatId()).isEqualTo(IntegrationConfig.channelId());
        assertThat(removed.postId()).isEqualTo(IntegrationConfig.postId());
        assertThat(removed.userId())
                .as("user_id names the person who deleted the comment, not the bot")
                .isPositive()
                .isNotEqualTo(botId);
        assertOnlySchemaFields(batchJson.get(), target);
    }

    private String sendCommentOrSkip(String text) {
        try {
            return api().sendComment(new NewCommentBody(text), IntegrationConfig.postId())
                    .execute().message().body().mid();
        } catch (MaxApiException e) {
            if (String.valueOf(e.errorMessage()).contains("comments_disabled")) {
                throw skipCommentsDisabled(e.errorMessage());
            }
            throw e;
        }
    }

    private static void awaitRemoval(CountDownLatch latch, String target) {
        try {
            if (!latch.await(REMOVAL_WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new AssertionError("No comment_removed for comment " + target
                        + " arrived within " + REMOVAL_WAIT_SECONDS + "s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for comment_removed", e);
        }
    }

    /**
     * Asserts that the event carries no property beyond those the schema lists. The model
     * ignores unknown properties, so without this a field MAX adds would go unnoticed.
     */
    private static void assertOnlySchemaFields(String batch, String target) {
        JsonNode root;
        try {
            root = MAPPER.readTree(batch);
        } catch (JacksonException e) {
            throw new AssertionError("the batch comment_removed arrived in is not JSON:\n" + batch, e);
        }
        for (JsonNode node : root.path("updates")) {
            if ("comment_removed".equals(node.path("update_type").asString())
                    && target.equals(node.path("message_id").asString())) {
                Set<String> extra = new TreeSet<>();
                node.propertyNames().forEach(name -> {
                    if (!COMMENT_REMOVED_FIELDS.contains(name)) {
                        extra.add(name);
                    }
                });
                assertThat(extra)
                        .withFailMessage("comment_removed carries %s, which "
                                + "CommentRemovedUpdate does not model:%n%s", extra, node)
                        .isEmpty();
                return;
            }
        }
        throw new AssertionError("comment_removed for " + target
                + " is missing from the batch it arrived in:\n" + batch);
    }

    /**
     * Deletes the comment a person was asked to remove, if they did not.
     */
    @AfterAll
    void removeLeftoverComment() {
        String leftover = removableCommentId;
        if (leftover != null) {
            cleanUp("delete comment " + leftover,
                    () -> api().deleteComment(IntegrationConfig.postId(), leftover).execute());
        }
    }
}