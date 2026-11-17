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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import ru.max.botapi.longpolling.MaxLongPollingConsumer;
import ru.max.botapi.model.ActionRequestBody;
import ru.max.botapi.model.ChatMember;
import ru.max.botapi.model.ChatPermission;
import ru.max.botapi.model.GetPinnedMessageResult;
import ru.max.botapi.model.Message;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.SendMessageResult;
import ru.max.botapi.model.SenderAction;
import ru.max.botapi.model.SimpleQueryResult;
import ru.max.botapi.model.Subscription;
import ru.max.botapi.model.SubscriptionRequestBody;
import ru.max.botapi.model.UpdateType;

import static org.assertj.core.api.Assertions.assertThat;

import static ru.max.botapi.it.RefusalChecks.assertRefused;
import static ru.max.botapi.it.RefusalChecks.assertRefusedWithCode;
import static ru.max.botapi.it.RefusalChecks.probe;

/**
 * Checks what MAX answers when an operation is refused with HTTP 200 and {@code success:false}.
 *
 * <p>Schema 0.0.34 says that such a response now carries a machine-readable {@code code}, and
 * the changelog names nine methods. This class provokes a refusal from each of them except
 * {@code leaveChat}, whose success cannot be undone, and {@code addMembers} and
 * {@code removeMember}, which need the disposable chat and are checked in
 * {@link DestructiveLiveTest} before the bot leaves it. Every response is printed for diagnosis,
 * and every {@code success:false} result is checked against its raw body by
 * {@link RefusalChecks}.</p>
 *
 * <p>Scenarios whose answer is deterministic are checked for it — unpinning with nothing
 * pinned and unsubscribing a URL that was never subscribed give {@code not.found}, editing or
 * deleting a missing message gives {@code success:false} with some code, editing someone
 * else's message gives {@code access.denied} — and for them a 4xx instead is a failure. The
 * rest ({@code sendAction} to a missing chat, {@code subscribe} with a plain-HTTP URL, deleting
 * someone else's message, the steps without administrator rights) have no fixed answer, so only
 * the general check applies, and a 4xx there is printed rather than failed. A response the
 * model cannot read fails every scenario.</p>
 *
 * <p>Every scenario is chosen so that a refusal changes nothing and an unexpected success is
 * undone: fake subscription URLs ending in {@link IntegrationConfig#WEBHOOK_PATH}, nonexistent
 * chats and messages, and unpinning only when nothing is pinned.</p>
 */
@Order(12)
@DisplayName("Live: success=false responses carry a code")
class SuccessFalseLiveTest extends LiveTestBase {

    /** Same as in {@link ErrorHandlingLiveTest}: no chat has this id. */
    private static final long MISSING_CHAT_ID = 1L;

    /** Well-formed but never issued. */
    private static final String MISSING_MESSAGE_ID = "mid.00000000000000000000000000000000";

    /** Plain HTTP, which MAX is expected to refuse for a webhook. */
    private static final String SUBSCRIBE_URL =
            "http://example.invalid" + IntegrationConfig.WEBHOOK_PATH;

    /** Never subscribed, so there is nothing for MAX to remove. */
    private static final String UNSUBSCRIBE_URL =
            "https://example.invalid" + IntegrationConfig.WEBHOOK_PATH;

    private static final int POLL_TIMEOUT_SECONDS = 5;

    private static final int INCOMING_WAIT_SECONDS = 90;

    private String ownMessageId;

    /** The operator's message from the foreign-message step, until the bot has deleted it. */
    private String foreignMessageId;

    /** The bot's membership in the test chat before it was demoted, checked again at the end. */
    private ChatMember rightsBefore;

    /**
     * The interactive steps read updates over long polling, so nothing may be diverting them.
     */
    @BeforeAll
    void clearSubscriptions() {
        clearOwnWebhookSubscriptions();
    }

    @Test
    @Order(1)
    @DisplayName("unpinMessage when nothing is pinned")
    void unpinWithoutPin() {
        long chatId = IntegrationConfig.chatId();
        requireNothingPinned(chatId);

        SimpleQueryResult result = probe("unpinMessage(" + chatId + ") with nothing pinned",
                () -> api().unpinMessage(chatId).execute());

        assertRefused(result, "unpinMessage with nothing pinned");
        assertThat(result.code()).as("unpinMessage with nothing pinned: code").isEqualTo("not.found");
    }

    @Test
    @Order(2)
    @DisplayName("sendAction to a missing chat")
    void sendActionToMissingChat() {
        probe("sendAction(TYPING_ON) to missing chat " + MISSING_CHAT_ID,
                () -> api().sendAction(new ActionRequestBody(SenderAction.TYPING_ON),
                        MISSING_CHAT_ID).execute());
    }

    @Test
    @Order(3)
    @DisplayName("editMessage with a missing mid")
    void editMissingMessage() {
        SimpleQueryResult result = probe("editMessage(" + MISSING_MESSAGE_ID + ")",
                () -> api().editMessage(NewMessageBody.text("Live suite: probe"),
                        MISSING_MESSAGE_ID).execute());

        assertRefusedWithCode(result, "editMessage with a missing mid");
    }

    @Test
    @Order(4)
    @DisplayName("deleteMessage with a missing mid")
    void deleteMissingMessage() {
        SimpleQueryResult result = probe("deleteMessage(" + MISSING_MESSAGE_ID + ")",
                () -> api().deleteMessage(MISSING_MESSAGE_ID).execute());

        assertRefusedWithCode(result, "deleteMessage with a missing mid");
    }

    @Test
    @Order(5)
    @DisplayName("editMessage and deleteMessage on someone else's message")
    void foreignMessage() {
        Assumptions.assumeTrue(IntegrationConfig.interactive(),
                () -> "interactive step skipped (set " + IntegrationConfig.INTERACTIVE
                        + "=true to run it)");
        long chatId = IntegrationConfig.chatId();
        String token = "probe-" + UUID.randomUUID().toString().substring(0, 8);

        Prompts.step(Prompts.groupChat(),
                "Send a text message containing " + token + " there, from your own account. "
                + "The bot will try to edit it and then delete it.");

        String mid = awaitMessage(chatId, token);
        foreignMessageId = mid;
        System.out.println("Received the operator's message " + mid);

        SimpleQueryResult edited = probe("editMessage(" + mid + ") of someone else's message",
                () -> api().editMessage(NewMessageBody.text("Live suite: edited by the bot"), mid)
                        .execute());
        // Observed on the live API. Deleting is not checked: an administrator bot may delete it.
        assertRefused(edited, "editMessage of someone else's message");
        assertThat(edited.code()).as("editMessage of someone else's message: code")
                .isEqualTo("access.denied");

        SimpleQueryResult deleted = probe("deleteMessage(" + mid + ") of someone else's message",
                () -> api().deleteMessage(mid).execute());
        if (deleted != null && deleted.success()) {
            foreignMessageId = null;
        }
    }

    @Test
    @Order(6)
    @DisplayName("sendAction, editMessage and unpinMessage without administrator rights")
    void withoutAdminRights() {
        Assumptions.assumeTrue(IntegrationConfig.interactive(),
                () -> "interactive step skipped (set " + IntegrationConfig.INTERACTIVE
                        + "=true to run it)");
        long chatId = IntegrationConfig.chatId();

        SendMessageResult sent = api()
                .sendMessage(NewMessageBody.text("Live suite: success=false probe"))
                .chatId(chatId)
                .execute();
        ownMessageId = sent.message().body().mid();
        boolean nothingPinned = nothingPinned(chatId);

        // What the operator has to give back. Without it a restore cannot be checked, so the
        // bot is not demoted at all.
        ChatMember before;
        try {
            before = api().getMembership(chatId).execute();
        } catch (RuntimeException e) {
            Assumptions.abort("could not record the bot's rights in " + chatId
                    + " before demoting it: " + e);
            return;
        }
        System.out.println("Rights before demotion: admin=" + before.isAdmin()
                + " permissions=" + before.permissions());
        Assumptions.assumeTrue(before.isAdmin(),
                () -> "the bot is not an administrator in " + chatId + ", so there are no "
                        + "rights to take away");
        rightsBefore = before;

        // Inside the try: if the prompt times out after the operator has already demoted the
        // bot, the finally block still asks for the rights back.
        Throwable primary = null;
        boolean demotionConfirmed = false;
        try {
            Prompts.step(Prompts.groupChat(),
                    "Temporarily take away the bot's administrator rights there: make it an "
                    + "ordinary member. Do NOT remove it from the chat.");
            demotionConfirmed = true;
            ChatMember demoted = probe("getMembership(" + chatId + ") after demotion",
                    () -> api().getMembership(chatId).execute());
            if (demoted != null && demoted.isAdmin()) {
                Assumptions.abort("the bot is still an administrator in " + chatId + ": it was "
                        + "not demoted, so probing without administrator rights would check "
                        + "nothing");
            }
            probe("sendAction(TYPING_ON) to " + chatId + " without admin rights",
                    () -> api().sendAction(new ActionRequestBody(SenderAction.TYPING_ON), chatId)
                            .execute());
            probe("editMessage(" + ownMessageId + ") of the bot's own message, no admin rights",
                    () -> api().editMessage(NewMessageBody.text("Live suite: edited"), ownMessageId)
                            .execute());
            if (nothingPinned) {
                probe("unpinMessage(" + chatId + ") without admin rights, nothing pinned",
                        () -> api().unpinMessage(chatId).execute());
            } else {
                System.out.println("unpinMessage skipped: a message is pinned in " + chatId
                        + ", and unpinning it would not be undone");
            }
        } catch (RuntimeException | Error e) {
            primary = e;
            throw e;
        } finally {
            // Asked only when the rights actually differ from the snapshot: after a demotion
            // that never happened, or a prompt that expired untouched, there is nothing to give back.
            AssertionError restore = rightsChanged(chatId, before)
                    ? restoreRights(chatId, before)
                    : null;
            if (restore == null && !demotionConfirmed && primary != null) {
                // The rights were intact when checked, but the operator may still act on the
                // expired prompt; @AfterAll checks once more.
                primary.addSuppressed(new AssertionError("If you took the bot's rights away in "
                        + chatId + " after the prompt expired, restore them: "
                        + describeRights(before)));
            }
            if (restore != null) {
                if (primary != null) {
                    primary.addSuppressed(restore);
                } else {
                    throw restore;
                }
            }
        }
    }

    @Test
    @Order(7)
    @DisplayName("subscribe and unsubscribe with fake URLs")
    void subscriptions() {
        List<String> before = subscribedUrls();
        System.out.println("Subscriptions before: " + before);
        // Someone else's subscription is not risked with a subscribe; unsubscribing a URL that
        // was never subscribed touches nothing, so that check always runs.
        String foreign = before.stream()
                .filter(url -> !url.endsWith(IntegrationConfig.WEBHOOK_PATH))
                .findFirst()
                .orElse(null);
        if (foreign != null) {
            System.out.println("subscribe probe skipped: " + foreign + " is subscribed to this "
                    + "bot's updates and does not belong to this suite");
        } else {
            try {
                probe("subscribe(" + SUBSCRIBE_URL + ")",
                        () -> api().subscribe(new SubscriptionRequestBody(SUBSCRIBE_URL,
                                List.of(UpdateType.MESSAGE_CREATED), null)).execute());
            } finally {
                // Plain cleanUp: MAX refuses the plain-HTTP URL, so there is normally nothing to
                // remove and the not.found answer is expected rather than worth a warning.
                cleanUp("unsubscribe " + SUBSCRIBE_URL,
                        () -> api().unsubscribe(SUBSCRIBE_URL).execute());
            }
        }
        SimpleQueryResult unsubscribed = probe("unsubscribe(" + UNSUBSCRIBE_URL
                        + "), never subscribed",
                () -> api().unsubscribe(UNSUBSCRIBE_URL).execute());

        List<String> after = subscribedUrls();
        List<String> added = new ArrayList<>(after);
        added.removeAll(before);
        List<String> removed = new ArrayList<>(before);
        removed.removeAll(after);
        System.out.println("Subscriptions after: " + after);
        assertThat(added).as("subscriptions added by the test").isEmpty();
        assertThat(removed).as("subscriptions removed by the test").isEmpty();

        assertRefused(unsubscribed, "unsubscribe of a URL never subscribed");
        assertThat(unsubscribed.code()).as("unsubscribe of a URL never subscribed: code")
                .isEqualTo("not.found");

        // Last, after every check ran: the step was incomplete, and should say so.
        if (foreign != null) {
            Assumptions.abort("subscribe probe skipped: " + foreign + " is subscribed to this "
                    + "bot's updates and does not belong to this suite");
        }
    }

    private void requireNothingPinned(long chatId) {
        Assumptions.assumeTrue(nothingPinned(chatId),
                () -> "a message is pinned in " + chatId + "; unpinning it would not be undone");
    }

    private boolean nothingPinned(long chatId) {
        GetPinnedMessageResult pinned = api().getPinnedMessage(chatId).execute();
        return pinned.message() == null;
    }

    private List<String> subscribedUrls() {
        List<Subscription> subscriptions = api().getSubscriptions().execute().subscriptions();
        return subscriptions == null ? List.of()
                : subscriptions.stream().map(Subscription::url).toList();
    }

    /**
     * Asks the operator to give the bot its rights back and checks that it has them again.
     *
     * <p>Runs from a {@code finally} block, so it returns the failure instead of throwing it:
     * the caller attaches it to whatever already failed the step, or throws it otherwise. A
     * prompt that times out and rights that do not match the snapshot are both failures, and
     * the message lists what is missing so that the operator can restore it by hand.</p>
     *
     * @param chatId the chat the bot was demoted in
     * @param before the bot's membership before the demotion
     * @return the failure, or {@code null} if the rights are back
     */
    private AssertionError restoreRights(long chatId, ChatMember before) {
        String wanted = describeRights(before);
        try {
            Prompts.step(Prompts.groupChat(),
                    "Make the bot an administrator there again, with the rights it had before: "
                    + wanted);
        } catch (RuntimeException | AssertionError e) {
            return new AssertionError("The bot's rights in " + chatId + " were not confirmed "
                    + "restored (" + e.getMessage() + "). Restore them by hand: " + wanted, e);
        }

        ChatMember after;
        try {
            after = probe("getMembership(" + chatId + ") after restoring rights",
                    () -> api().getMembership(chatId).execute());
        } catch (RuntimeException e) {
            return new AssertionError("Could not read the bot's rights in " + chatId
                    + " after restoring them; check by hand that it has " + wanted, e);
        }
        if (after == null) {
            return new AssertionError("Could not read the bot's rights in " + chatId
                    + " after restoring them; check by hand that it has " + wanted);
        }
        String missing = missingRights(before, after);
        if (missing != null) {
            return new AssertionError("The bot's rights in " + chatId + " were not fully "
                    + "restored. Missing: " + missing + ". Restore them by hand; expected "
                    + wanted);
        }
        return null;
    }

    /**
     * Tells whether the bot's rights differ from the snapshot after the demotion step.
     *
     * <p>The operator may or may not have acted — the prompt may have expired, or the bot may
     * not have been demoted after all. If the rights still match, there is nothing to give back
     * and the step's own outcome stands; if they differ, or cannot be read, the operator is
     * asked to restore them.</p>
     *
     * @param chatId the chat
     * @param before the bot's membership before the demotion was asked for
     * @return {@code true} if the rights have to be restored
     */
    private boolean rightsChanged(long chatId, ChatMember before) {
        ChatMember now;
        try {
            now = probe("getMembership(" + chatId + ") after the demotion step",
                    () -> api().getMembership(chatId).execute());
        } catch (RuntimeException e) {
            // Runs from a finally block: never replace the failure that brought us here.
            System.out.println("Could not read the bot's rights in " + chatId + ": " + e);
            return true;
        }
        return now == null || missingRights(before, now) != null;
    }

    /**
     * Describes what the bot held before and lacks now.
     *
     * @param before the snapshot
     * @param after  the current membership
     * @return e.g. {@code administrator status, permissions [PIN_MESSAGE]}, or {@code null} if
     *         nothing is missing
     */
    private static String missingRights(ChatMember before, ChatMember after) {
        Set<ChatPermission> missing = new TreeSet<>(permissionsOf(before));
        missing.removeAll(permissionsOf(after));
        boolean adminLost = before.isAdmin() && !after.isAdmin();
        if (!adminLost && missing.isEmpty()) {
            return null;
        }
        return (adminLost ? "administrator status, " : "") + "permissions " + missing;
    }

    private static String describeRights(ChatMember member) {
        return "admin=" + member.isAdmin() + " permissions=" + member.permissions();
    }

    private static List<ChatPermission> permissionsOf(ChatMember member) {
        return member.permissions() == null ? List.of() : member.permissions();
    }

    /**
     * Waits for the operator's message carrying the given token in the given chat.
     *
     * @param chatId the chat to watch
     * @param token  text the message must contain
     * @return the message id
     */
    private String awaitMessage(long chatId, String token) {
        AtomicReference<String> holder = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        MaxLongPollingConsumer consumer = MaxLongPollingConsumer.builder()
                .api(api())
                .pollTimeout(POLL_TIMEOUT_SECONDS)
                .handler(update -> {
                    if (update instanceof MessageCreatedUpdate created
                            && matches(created.message(), chatId, token)) {
                        holder.set(created.message().body().mid());
                        latch.countDown();
                    }
                })
                .onError(e -> System.out.println("Long polling error: " + e))
                .build();

        consumer.start();
        try {
            if (!latch.await(INCOMING_WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new AssertionError("No message containing " + token + " arrived in "
                        + chatId + " within " + INCOMING_WAIT_SECONDS + "s. If you send it "
                        + "later, delete it from the chat by hand: the bot no longer waits for it.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a message", e);
        } finally {
            consumer.stop();
        }
        return holder.get();
    }

    private static boolean matches(Message message, long chatId, String token) {
        Long recipientChat = message.recipient().chatId();
        return recipientChat != null && recipientChat == chatId
                && message.body() != null
                && message.body().text() != null
                && message.body().text().contains(token);
    }

    /**
     * Checks once more that the bot has the rights it had before the demotion step.
     *
     * <p>The operator may act on a demotion prompt after it has expired, when the step no longer
     * asks for the rights back. This cannot fail anything any more, so a difference is printed
     * as loudly as possible, with what has to be restored.</p>
     */
    @AfterAll
    void checkRightsStillIntact() {
        if (rightsBefore == null || live == null) {
            return;
        }
        long chatId = IntegrationConfig.chatId();
        ChatMember now;
        try {
            now = api().getMembership(chatId).execute();
        } catch (RuntimeException e) {
            System.out.println("WARNING: could not check the bot's rights in " + chatId
                    + " at the end (" + e + "). Make sure it has " + describeRights(rightsBefore));
            return;
        }
        String missing = missingRights(rightsBefore, now);
        if (missing != null) {
            System.out.println();
            System.out.println("!!! WARNING: the bot is missing rights in " + chatId + ": " + missing
                    + ". Restore them by hand: " + describeRights(rightsBefore) + " !!!");
            System.out.println();
        }
    }

    /**
     * Deletes the bot's own message from the demotion step and, if the foreign-message step
     * failed before deleting it, the operator's message.
     */
    @AfterAll
    void removeMessages() {
        if (ownMessageId != null) {
            cleanUpResult("delete probe message", () -> api().deleteMessage(ownMessageId).execute());
        }
        if (foreignMessageId != null) {
            String mid = foreignMessageId;
            cleanUpResult("delete the operator's message", () -> api().deleteMessage(mid).execute());
        }
    }
}
