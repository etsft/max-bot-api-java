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

package ru.max.botapi.examples;

import java.util.Set;

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.longpolling.MaxLongPollingConsumer;
import ru.max.botapi.model.CommentCreatedUpdate;
import ru.max.botapi.model.CommentMessage;
import ru.max.botapi.model.MessageLinkType;
import ru.max.botapi.model.NewCommentBody;
import ru.max.botapi.model.NewMessageLink;
import ru.max.botapi.model.UpdateType;

/**
 * A bot that replies to new comments on channel posts.
 *
 * <p>The bot must be an administrator of the channel with the {@code READ_ALL_MESSAGES} and
 * {@code WRITE} permissions. It receives only {@code comment_created} updates and answers each
 * comment with a reply to it.</p>
 *
 * <p>The bot's own comments arrive as {@code comment_created} too, so it compares the sender
 * with its own user ID and skips them; without that check every reply would trigger another.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * export MAX_BOT_TOKEN="your-bot-token"
 * ./gradlew :max-bot-api-examples:run -PmainClass=ru.max.botapi.examples.CommentsBot
 * </pre>
 */
public final class CommentsBot {

    private CommentsBot() {
    }

    /**
     * Entry point for the comments bot.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        String token = System.getenv("MAX_BOT_TOKEN");
        if (token == null || token.isBlank()) {
            System.err.println("Set the MAX_BOT_TOKEN environment variable.");
            System.exit(1);
        }

        try (MaxBotAPI api = MaxBotAPI.create(token)) {
            long botUserId = api.getMyInfo().execute().userId();

            try (MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
                    .api(api)
                    .types(Set.of(UpdateType.COMMENT_CREATED))
                    .handler(update -> {
                        if (update instanceof CommentCreatedUpdate created && created.message() != null) {
                            replyTo(api, created.message(), botUserId);
                        }
                    })
                    .build()) {

                consumer.start();
                System.out.println("CommentsBot is running. Press Ctrl+C to stop.");

                Thread.currentThread().join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void replyTo(MaxBotAPI api, CommentMessage comment, long botUserId) {
        if (comment.sender() != null && comment.sender().userId() == botUserId) {
            return;
        }
        // The post the comment belongs to; the comment methods are addressed by it.
        String postId = comment.recipient().postId();
        if (postId == null) {
            return;
        }
        String author = comment.sender() == null || comment.sender().firstName() == null
                ? "there"
                : comment.sender().firstName();
        NewCommentBody reply = new NewCommentBody(
                "Thanks for your comment, " + author + "!",
                new NewMessageLink(MessageLinkType.REPLY, comment.body().mid()),
                null);
        try {
            api.sendComment(reply, postId).execute();
        } catch (Exception e) {
            System.err.println("Failed to reply to a comment: " + e.getMessage());
        }
    }
}
