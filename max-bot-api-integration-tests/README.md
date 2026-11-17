# max-bot-api-integration-tests

A hand-run suite that exercises every `MaxBotAPI` endpoint against the **real** MAX Bot API
with a **real** bot token.

It exists because the rest of the repository is tested against WireMock stubs, which cannot
notice that the live API has changed shape. This suite can: whenever a response falls back to
`UnknownUpdate`, `UnknownAttachment` or `UnknownButton`, the test fails and prints the raw JSON
that caused it. Treat such a failure as a model bug to fix, not an assertion to relax.

> **This suite has real side effects.** It posts, edits, pins and deletes messages, changes the
> bot profile and the chat title, comments on a channel post, sends one direct message to
> `MAX_IT_USER_ID`, and can subscribe a webhook. Everything it creates it deletes again, but
> the direct message and the comment are briefly visible to a real person. Point it at a chat
> you own and do not mind disturbing.

## Running

```bash
set -a; source .env.local; set +a
./gradlew :max-bot-api-integration-tests:liveTest --console=plain
```

`--console=plain` matters: without it Gradle buffers the output and the interactive prompts are
not visible. The task is never up-to-date, so it re-runs every time.

Prompts name the chat by its title as well as by its id. The titles of the configured chats
are looked up once, when the first class starts, and printed once in a `Live suite chats:` line; a
chat that cannot be looked up is shown by id only.

Input typed before a prompt appears — say, an Enter pressed after the previous prompt had
already timed out — is discarded, so it cannot confirm the next prompt unread.

Each interactive prompt waits 30 seconds for Enter. Raise it for a run when that is not enough
to reach the phone:

```bash
./gradlew :max-bot-api-integration-tests:liveTest --console=plain -Pit.promptTimeout=90
```

To run a single class rather than the whole package, name it with `-Pit.class=<SimpleName>`
(a class in `ru.max.botapi.it`). Without it every class runs, including `DestructiveLiveTest`
when `MAX_IT_DESTRUCTIVE=true`:

```bash
./gradlew :max-bot-api-integration-tests:liveTest --console=plain -Pit.class=SuccessFalseLiveTest
```

It is not part of `./gradlew build` and never runs in CI — the module's `test` task is disabled
and `liveTest` has to be asked for by name. The sources are still compiled and checkstyled by
the normal build, so they cannot rot.

## Configuration

| Variable | Required | Meaning |
|---|---|---|
| `MAX_BOT_TOKEN` | yes | Bot access token. Without it every class is skipped. |
| `MAX_IT_CHAT_ID` | for most tests | Main test chat. The bot must be an administrator. |
| `MAX_IT_USER_ID` | for membership and direct-message tests | A real user id to add, remove and promote, and to receive one direct message. That account must have started the bot, otherwise the direct-message step reports a skip. |
| `MAX_IT_CHAT_LINK` | no | Public chat link or username for `getChatByLink`: `@my_chat`, `my_chat` or `https://max.ru/my_chat`. An invite link (`https://max.ru/join/…`) points at a private chat, which this endpoint cannot resolve, so the test is skipped when one is configured. |
| `MAX_IT_BASE_URL` | no | Overrides the API base URL. |
| `MAX_IT_INTERACTIVE` | no | `true` enables the steps that need a human. |
| `MAX_IT_PROMPT_TIMEOUT_SECONDS` | no | How long a prompt waits for Enter, default 30. Also settable per run as `-Pit.promptTimeout=<seconds>`, which wins over the variable. |
| `MAX_IT_CHANNEL_ID` | for comment tests | Channel where the bot is an administrator with `read_all_messages`, `write`, `edit` and `delete`. |
| `MAX_IT_POST_ID` | for comment tests | A post (`mid`) in that channel whose comments are enabled. The comment tests post, edit and delete one comment on it. |
| `MAX_IT_VIDEO_PATH` | no | A real video file; without it the video tests are skipped. |
| `MAX_IT_AUDIO_PATH` | no | A real audio file; without it the audio test is skipped. |
| `MAX_IT_WEBHOOK_URL` | no | Public HTTPS URL proxied to this machine, path preserved. Without a path of its own, `/max-bot/live-test` is appended and subscribed; the proxy must forward that path unchanged to `MAX_IT_WEBHOOK_PORT`. |
| `MAX_IT_WEBHOOK_PORT` | no | Local port to bind, default 18080. |
| `MAX_IT_WEBHOOK_SECRET` | no | Value expected in `X-Max-Bot-Api-Secret`. |
| `MAX_IT_DESTRUCTIVE` | no | `true` enables the irreversible operations below. |
| `MAX_IT_DISPOSABLE_CHAT_ID` | for destructive tests | Chat used for membership, admin and `leaveChat`. |
| `MAX_IT_DELETABLE_CHAT_ID` | for destructive tests | A second chat for `deleteChat`. See the note below: MAX refuses the call for a bot, so this step reports a skip. |

Anything unset is reported as a skip naming the variable, so a partial run is a normal outcome
rather than a failure. Put the values in `.env.local`; `.env*` is git-ignored.

Images and plain files for the upload tests are generated into `build/it-fixtures/`. Video and
audio are not generated on purpose — a synthesised container would likely be rejected by the
API for reasons unrelated to this library, so those come from real files or not at all.

## What needs a human

With `MAX_IT_INTERACTIVE=true` the suite pauses and prints what to do:

- **Updates** — send any message to the bot, so a real `message_created` can be verified.
- **Callbacks** — press the inline button the bot posts. `answerOnCallback` cannot be reached
  any other way: it needs a `callback_id`, which only exists after a real press.
- **Webhook** — send a message so MAX delivers it to the subscribed URL. The suite prints the
  exact URL it subscribed; MAX posts to precisely that, so the proxy has to reach the local
  server on the same path.
- **Comments** — delete the comment the bot posts under `MAX_IT_POST_ID`, as a channel
  administrator. MAX sends a bot no events for its own actions, so this is the only way to
  observe `comment_removed`. The step checks every identifier in the event against the comment
  it posted, and fails if the event carries a field the schema does not list. A comment left
  undeleted is removed in teardown.

## Keyboard buttons

`KeyboardButtonsLiveTest` covers the buttons whose shape is known only from the schema:
`message` and `open_app`. It posts one message per button to `MAX_IT_CHAT_ID`, reads each back
with `getMessageById`, prints the button as MAX stored it, and deletes the messages afterwards.

The `open_app` buttons name the test bot itself, so nothing extra has to be configured: one by
`web_app` alone, one by `web_app` together with `contact_id`. There is no case for `contact_id`
on its own. The schema lists it as an alternative, but MAX refuses such a button with
`Field 'webApp' cannot be null`, so the library no longer builds one. Any refusal fails the test
with MAX's own error message.

## Refusals with `success: false`

`SuccessFalseLiveTest` provokes a refusal from the methods that can answer HTTP 200 with
`success: false` and checks each one against the raw response: `code()` must be exactly the
`code` in the body (or `null` when it has none), and a body field beyond `success`, `message`,
`code`, `failed_user_ids` and `failed_user_details` is printed as a warning. The deterministic
cases are checked for their answer as well — unpinning with nothing pinned and unsubscribing a
URL that was never subscribed give `not.found`; editing or deleting a missing message gives some
code; editing someone else's message gives `access.denied` — and for them a 4xx instead is a
failure. The other cases (`sendAction` to a missing chat, `subscribe` with a plain-HTTP URL,
deleting someone else's message, which an administrator bot is allowed to do, the bot
temporarily demoted) have no fixed answer: only the general check applies, and a 4xx there is
printed rather than failed. A 401, a 429, a 5xx, a response the model cannot read, or a
transport failure fails the scenario in either case: none of them is an answer about the
operation. Every response is printed for diagnosis. If the bot has a webhook subscription that
does not belong to this suite, only the `subscribe` probe is skipped; the `unsubscribe` check
and the comparison of subscriptions before and after always run, and the step is then reported
as skipped. `leaveChat` is not probed, since its success cannot be undone; `addMembers` and
`removeMember` of a nonexistent user are checked the same way in
`DestructiveLiveTest`, before it leaves the disposable chat.

## Destructive operations

`MAX_IT_DESTRUCTIVE=true` enables `addMembers`/`removeMember`, `postAdmins`/`deleteAdmins`,
`deleteChat` and `leaveChat`. Two throwaway chats are needed rather than one: after `leaveChat`
the bot has no access left to delete anything, so a single chat cannot verify both endpoints.

The disposable chat is consumed by a successful run — `leaveChat` removes the bot and it cannot
return on its own — so a repeat run needs a fresh chat, with the bot added as an administrator
holding `add_remove_members` and `add_admins`. Conditions outside the suite's control are
reported as skips rather than failures, each naming what to change: a bot that is no longer in
the disposable chat or lacks a right there, and a user whose privacy settings refuse invitations
from bots (`add.participant.privacy`).

`deleteChat` is one of those skips in practice. `DELETE /chats/{chatId}` is not in the MAX
documentation, and it answers `success=false, "Insufficient access rights to perform this
action"` for a bot that holds every right it can be granted — verified in a group chat and in a
channel, where `edit` and `delete` are also available. The call appears to require ownership,
which a bot cannot have: the Bot API has no method that creates a chat. The step still makes the
call and prints whatever MAX answers, so the day that changes, the suite will notice. The step
is skipped only when the refusal is recognised as one for lack of rights. MAX answers it with
`{"success":false,"message":"Insufficient access rights to perform this action","code":"chat.denied"}`,
so the rule is: `code` is `chat.denied` and the message does not mention "Invalid chatId". The
same code also comes with "Invalid chatId: 0" — a wrong id, whether from the configuration or
from a broken `DeleteChatQuery` — and that must fail, not skip. Before calling `deleteChat` the
step also checks that `MAX_IT_DELETABLE_CHAT_ID` is not 0 and reads the chat with `getChat`; a
403 or 404 there fails the step with an explanation, any other error as it is. A response
without a code is recognised by "access rights" in the message, as before. Any other
`success=false` fails the step with its code and message.

## Ordering and cleanup

Classes run in the fixed order of their `@Order` annotations:

0. `ModelAssertionsSelfTest`
1. `BotInfoLiveTest`
2. `ChatLiveTest`
3. `MessageLifecycleLiveTest`
4. `AttachmentLiveTest`
5. `UpdatesLiveTest`
6. `CallbackLiveTest`
7. `ErrorHandlingLiveTest`
8. `SubscriptionWebhookLiveTest`
9. `DestructiveLiveTest`
10. `CommentsLiveTest`
11. `KeyboardButtonsLiveTest`
12. `SuccessFalseLiveTest`

A webhook subscription changes how MAX delivers updates for the whole bot, and whether long
polling keeps working alongside one is not documented, so the long-polling classes `Updates` and
`Callback` run before `SubscriptionWebhookLiveTest`. It removes its subscription in teardown even
when the test fails, and the classes after it that read updates over long polling
(`CommentsLiveTest`, `SuccessFalseLiveTest`) first clear any leftover subscription of this suite.
`DestructiveLiveTest` ends with `leaveChat`, so no later class uses the disposable chat.

Each class restores what it changed: the bot profile, the chat title, posted messages, the pin,
the subscription, and the bot's administrator rights in `MAX_IT_CHAT_ID`, which
`SuccessFalseLiveTest` asks the operator to take away for one step. Its rights are recorded
before the demotion and compared after the operator restores them; a restore prompt that times
out, or rights that do not match, fail that step with the missing rights listed, so that they
can be given back by hand. If the demotion prompt itself timed out and the rights are still
intact, nothing is asked, but the failure reminds you to restore them if you demote the bot
afterwards; the class checks the rights once more when it finishes and prints a loud warning
listing anything missing. Cleanup failures are printed and swallowed so they never mask the real
error, and a cleanup that MAX refuses with `success=false` is printed as a warning with its code
and message.

An interrupted run can still leave a subscription behind, and while one exists MAX delivers
updates to it instead of to long polling — which looks like a broken consumer, not like a
leftover. The long-polling classes therefore remove any subscription whose URL ends with
`/max-bot/live-test` before they start, and print a warning for any other subscription they
find.
