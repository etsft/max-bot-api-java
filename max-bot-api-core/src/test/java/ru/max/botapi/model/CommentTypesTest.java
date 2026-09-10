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

package ru.max.botapi.model;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests for the channel-comment records and the bot-commands patch introduced alongside them.
 */
class CommentTypesTest {

    private static final MessageRecipient CHANNEL =
            new MessageRecipient(42L, ChatType.CHANNEL, null, "mid.post123");

    @Test
    void commentMessageBody_copiesMarkup() {
        var markup = new java.util.ArrayList<MarkupElement>();
        markup.add(new MarkupElement("strong", 0, 4));
        var body = new CommentMessageBody("mid.c1", 1L, "Nice", markup);

        markup.clear();

        assertThat(body.markup()).hasSize(1);
        assertThat(body.text()).isEqualTo("Nice");
    }

    @Test
    void commentMessageBody_nullMarkup_staysNull() {
        var body = new CommentMessageBody("mid.c1", 1L, null, null);
        assertThat(body.markup()).isNull();
        assertThat(body.text()).isNull();
    }

    @Test
    void commentMessageBody_nullMid_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CommentMessageBody(null, 0L, null, null));
    }

    @Test
    void commentMessage_construction() {
        var body = new CommentMessageBody("mid.c1", 1L, "Hi", null);
        var comment = new CommentMessage(null, CHANNEL, 100L, null, body);

        assertThat(comment.sender()).isNull();
        assertThat(comment.recipient().postId()).isEqualTo("mid.post123");
        assertThat(comment.body().mid()).isEqualTo("mid.c1");
    }

    @Test
    void commentMessage_nulls_throw() {
        var body = new CommentMessageBody("mid.c1", 1L, null, null);
        assertThatNullPointerException()
                .isThrownBy(() -> new CommentMessage(null, null, 0L, null, body));
        assertThatNullPointerException()
                .isThrownBy(() -> new CommentMessage(null, CHANNEL, 0L, null, null));
    }

    @Test
    void commentLinkedMessage_construction() {
        var body = new CommentMessageBody("mid.c0", 0L, "Parent", null);
        var link = new CommentLinkedMessage(MessageLinkType.REPLY, null, 42L, body);

        assertThat(link.type()).isEqualTo(MessageLinkType.REPLY);
        assertThat(link.message().text()).isEqualTo("Parent");
    }

    @Test
    void commentLinkedMessage_nulls_throw() {
        var body = new CommentMessageBody("mid.c0", 0L, null, null);
        assertThatNullPointerException()
                .isThrownBy(() -> new CommentLinkedMessage(null, null, null, body));
        assertThatNullPointerException()
                .isThrownBy(() -> new CommentLinkedMessage(MessageLinkType.REPLY, null, null, null));
    }

    @Test
    void newCommentBody_textOnlyConvenienceConstructor() {
        var body = new NewCommentBody("Hello");
        assertThat(body.text()).isEqualTo("Hello");
        assertThat(body.link()).isNull();
        assertThat(body.format()).isNull();
    }

    @Test
    void newCommentBody_full() {
        var body = new NewCommentBody("Hello",
                new NewMessageLink(MessageLinkType.REPLY, "mid.c0"), TextFormat.HTML);
        assertThat(body.link().mid()).isEqualTo("mid.c0");
        assertThat(body.format()).isEqualTo(TextFormat.HTML);
    }

    @Test
    void commentList_copiesMessages() {
        var body = new CommentMessageBody("mid.c1", 1L, "Hi", null);
        var messages = new java.util.ArrayList<CommentMessage>();
        messages.add(new CommentMessage(null, CHANNEL, 100L, null, body));
        var list = new CommentList(messages);

        messages.clear();

        assertThat(list.messages()).hasSize(1);
    }

    @Test
    void commentList_nullMessages_throws() {
        assertThatNullPointerException().isThrownBy(() -> new CommentList(null));
    }

    @Test
    void sendCommentResult_nullMessage_throws() {
        assertThatNullPointerException().isThrownBy(() -> new SendCommentResult(null));
    }

    @Test
    void sendCommentResult_construction() {
        var body = new CommentMessageBody("mid.c1", 1L, "Hi", null);
        var comment = new CommentMessage(null, CHANNEL, 100L, null, body);
        assertThat(new SendCommentResult(comment).message()).isSameAs(comment);
    }

    @Test
    void botCommandsPatch_copiesCommands() {
        var commands = new java.util.ArrayList<BotCommand>();
        commands.add(new BotCommand("start", "Start"));
        var patch = new BotCommandsPatch(commands);

        commands.clear();

        assertThat(patch.commands()).hasSize(1);
    }

    @Test
    void botCommandsPatch_nullAndEmpty() {
        assertThat(new BotCommandsPatch(null).commands()).isNull();
        assertThat(new BotCommandsPatch(List.of()).commands()).isEmpty();
    }

    @Test
    void botCommandsResult_copiesCommands() {
        var commands = new java.util.ArrayList<BotCommand>();
        commands.add(new BotCommand("help", "Help"));
        var result = new BotCommandsResult(commands);

        commands.clear();

        assertThat(result.commands()).hasSize(1);
    }

    @Test
    void botCommandsResult_nullCommands_staysNull() {
        assertThat(new BotCommandsResult(null).commands()).isNull();
    }

    @Test
    void addMembersResult_carriesTheErrorMessage() {
        var result = new AddMembersResult(false, "some users could not be added",
                List.of(7L), List.of(new AddMemberFailure("privacy", List.of(7L))));

        assertThat(result.message()).isEqualTo("some users could not be added");
        assertThat(result.failedUserIds()).containsExactly(7L);
        assertThat(result.failedUserDetails()).hasSize(1);
    }

    @Test
    void addMembersResult_nullCollections_stayNull() {
        var result = new AddMembersResult(true, null, null, null);
        assertThat(result.failedUserIds()).isNull();
        assertThat(result.failedUserDetails()).isNull();
        assertThat(result.message()).isNull();
    }

    @Test
    void dialogUpdates_reportTheirType() {
        assertThat(new DialogClearedUpdate(1L, 42L, null).updateType())
                .isEqualTo("dialog_cleared");
        assertThat(new DialogMutedUpdate(1L, 42L, null).updateType())
                .isEqualTo("dialog_muted");
        assertThat(new DialogUnmutedUpdate(1L, 42L, null).updateType())
                .isEqualTo("dialog_unmuted");
        assertThat(new DialogRemovedUpdate(1L, 42L, null).updateType())
                .isEqualTo("dialog_removed");
    }

    @Test
    void commentUpdates_reportTheirType() {
        assertThat(new CommentCreatedUpdate(1L, null).updateType())
                .isEqualTo("comment_created");
        assertThat(new CommentEditedUpdate(1L, null).updateType())
                .isEqualTo("comment_edited");
        assertThat(new CommentRemovedUpdate(1L, null).updateType())
                .isEqualTo("comment_removed");
    }

    @Test
    void clipboardButton_construction() {
        var button = new ClipboardButton("Copy", "PROMO-1");
        assertThat(button.type()).isEqualTo("clipboard");
        assertThat(button.text()).isEqualTo("Copy");
        assertThat(button.payload()).isEqualTo("PROMO-1");
    }

    @Test
    void clipboardButton_nulls_throw() {
        assertThatNullPointerException().isThrownBy(() -> new ClipboardButton(null, "p"));
        assertThatNullPointerException().isThrownBy(() -> new ClipboardButton("t", null));
    }
}