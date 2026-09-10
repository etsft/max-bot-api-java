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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import ru.max.botapi.client.MaxApiException;
import ru.max.botapi.model.AddMembersResult;
import ru.max.botapi.model.ChatAdmin;
import ru.max.botapi.model.ChatAdminsList;
import ru.max.botapi.model.ChatMember;
import ru.max.botapi.model.ChatMembersList;
import ru.max.botapi.model.ChatPermission;
import ru.max.botapi.model.SimpleQueryResult;
import ru.max.botapi.model.UserIdsList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Operations that cannot be undone: membership changes, admin changes, leaving and deleting.
 *
 * <p>These run against {@code MAX_IT_DISPOSABLE_CHAT_ID} — a chat that is expected to be gone
 * afterwards — and only when {@code MAX_IT_DESTRUCTIVE=true}. Pointing the disposable chat at
 * the main test chat will destroy it; that is why it is a separate variable rather than a flag
 * on the existing one.</p>
 */
@Order(9)
@DisplayName("Live: destructive operations")
@SuppressWarnings("deprecation") // exercises the endpoints MAX no longer documents
class DestructiveLiveTest extends LiveTestBase {

    private long chatId;

    private ChatMember botMembership;

    /**
     * Skips every case unless the run explicitly opted into destruction.
     */
    @BeforeEach
    void requireOptIn() {
        Assumptions.assumeTrue(IntegrationConfig.destructive(),
                "destructive operations are disabled (set " + IntegrationConfig.DESTRUCTIVE
                        + "=true to run them)");
        chatId = IntegrationConfig.disposableChatId();
    }

    /**
     * Aborts when the bot is no longer in the disposable chat.
     *
     * <p>Called by the steps that operate on that chat rather than from {@code @BeforeEach}:
     * {@code deleteChat} works on a different chat and must not be held back by this one.</p>
     *
     * <p>A successful run consumes that chat: {@code leaveChat} is the last step and the bot
     * cannot come back on its own. Everything here then answers {@code chat.denied}, and
     * {@code leaveChat} answers success for a chat it is not in — a red run and a false green
     * that both describe the same spent fixture rather than a defect.</p>
     */
    private void requireDisposableChat() {
        try {
            botMembership = api().getMembership(chatId).execute();
        } catch (MaxApiException e) {
            Assumptions.abort(IntegrationConfig.DISPOSABLE_CHAT_ID + " (" + chatId + ") is not "
                    + "usable: " + e.getMessage() + ". Every destructive run ends with "
                    + "leaveChat, so the chat is spent: add the bot back as an administrator, "
                    + "or point the variable at a fresh chat.");
        }
    }

    /**
     * Aborts when the bot lacks the administrator right the step needs.
     *
     * <p>Membership alone is not enough — MAX answers {@code chat.denied} for a member without
     * the right — and the reply names no permission, so the check is made here where it can.</p>
     *
     * @param right the permission the step requires
     */
    private void requireRight(ChatPermission right) {
        List<ChatPermission> held = botMembership.permissions();
        Assumptions.assumeTrue(held != null && held.contains(right),
                () -> "the bot needs the " + right + " right in "
                        + IntegrationConfig.DISPOSABLE_CHAT_ID + " (" + chatId + "); it is "
                        + (botMembership.isAdmin() ? "an administrator holding " + held
                                : "not an administrator there"));
    }

    @Test
    @Order(1)
    @DisplayName("addMembers then removeMember")
    void membership() {
        requireDisposableChat();
        requireRight(ChatPermission.ADD_REMOVE_MEMBERS);
        long userId = IntegrationConfig.userId();

        AddMembersResult added = api()
                .addMembers(new UserIdsList(List.of(userId)), chatId)
                .execute();
        // A user who blocks invitations comes back as a reported failure rather than an
        // exception (add.participant.privacy). That is the user's setting, not a library
        // defect, and removeMember would only fail after it, so the case is skipped instead.
        Assumptions.assumeTrue(added.success(),
                () -> "addMembers was refused by MAX: " + added.failedUserDetails()
                        + " — user " + userId + " does not accept invitations from this bot");

        SimpleQueryResult removed = api().removeMember(chatId, userId)
                .block(false)
                .execute();

        assertThat(removed.success())
                .withFailMessage("removeMember failed: %s", removed.message())
                .isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("postAdmins then deleteAdmins")
    void admins() {
        requireDisposableChat();
        requireRight(ChatPermission.ADD_ADMINS);
        long userId = IntegrationConfig.userId();

        // Only a participant can be promoted, and the step before this one ends by removing
        // that very user — so membership is established here rather than assumed.
        boolean invited = ensureParticipant(userId);
        try {
            // READ_ALL_MESSAGES is not optional company for WRITE: the API rejects the write
            // right unless the member holds or is granted it in the same call.
            ChatAdmin admin = new ChatAdmin(userId,
                    List.of(ChatPermission.READ_ALL_MESSAGES, ChatPermission.WRITE));

            SimpleQueryResult promoted = api()
                    .postAdmins(new ChatAdminsList(List.of(admin)), chatId)
                    .execute();
            assertThat(promoted.success())
                    .withFailMessage("postAdmins failed: %s", promoted.message())
                    .isTrue();

            ChatMembersList admins = api().getAdmins(chatId).execute();
            assertThat(admins.members())
                    .extracting(ChatMember::userId)
                    .contains(userId);

            SimpleQueryResult demoted = api().deleteAdmins(chatId, userId).execute();
            assertThat(demoted.success())
                    .withFailMessage("deleteAdmins failed: %s", demoted.message())
                    .isTrue();
        } finally {
            if (invited) {
                cleanUp("remove member " + userId, () -> api()
                        .removeMember(chatId, userId).block(false).execute());
            }
        }
    }

    /**
     * Makes sure the user is in the chat, adding them if the previous step took them out.
     *
     * @param userId the user to promote afterwards
     * @return {@code true} if this call added them, so the caller has to take them back out
     */
    private boolean ensureParticipant(long userId) {
        ChatMembersList found = api().getMembers(chatId)
                .userIds(List.of(userId))
                .execute();
        if (found.members() != null && !found.members().isEmpty()) {
            return false;
        }

        AddMembersResult added = api()
                .addMembers(new UserIdsList(List.of(userId)), chatId)
                .execute();
        Assumptions.assumeTrue(added.success(),
                () -> "user " + userId + " has to be in the chat to be promoted, and addMembers "
                        + "was refused by MAX: " + added.failedUserDetails());
        return true;
    }

    @Test
    @Order(3)
    @DisplayName("deleteChat removes a separate deletable chat")
    void deleteChat() {
        // A separate chat on purpose: leaveChat below costs the bot its access to `chatId`,
        // after which it could no longer delete it. One chat cannot verify both endpoints.
        long deletableChatId = IntegrationConfig.deletableChatId();

        SimpleQueryResult result = api().deleteChat(deletableChatId).execute();

        // MAX answers HTTP 200 with success=false — a refusal, not an error — and the only
        // refusal that is about the environment rather than about this library is the one on
        // access rights. Checked twice with every right a bot can be granted: in a group chat
        // (read_all_messages, write, add_admins, add_remove_members, change_chat_info,
        // pin_message, edit_link, can_call) and in a channel, which adds edit and delete —
        // refused both times. What it lacks is ownership, and a bot cannot obtain that: the
        // Bot API has no method that creates a chat.
        if (!result.success() && String.valueOf(result.message()).contains("access rights")) {
            Assumptions.abort("deleteChat was refused for " + IntegrationConfig.DELETABLE_CHAT_ID
                    + " (" + deletableChatId + "): " + result.message()
                    + ". The endpoint is undocumented and needs chat ownership, which a bot "
                    + "cannot obtain: no admin right unlocks it, in a chat or in a channel.");
        }
        assertThat(result.success())
                .withFailMessage("deleteChat failed: %s", result.message())
                .isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("leaveChat removes the bot from the disposable chat")
    void leaveChat() {
        // Last: the bot loses access to `chatId` here, so nothing else can use it afterwards.
        requireDisposableChat();

        SimpleQueryResult result = api().leaveChat(chatId).execute();

        assertThat(result.success())
                .withFailMessage("leaveChat failed: %s", result.message())
                .isTrue();
    }
}
