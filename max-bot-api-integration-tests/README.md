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

Each interactive prompt waits 30 seconds for Enter. Raise it for a run when that is not enough
to reach the phone:

```bash
./gradlew :max-bot-api-integration-tests:liveTest --console=plain -Pit.promptTimeout=90
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
call and prints whatever MAX answers, so the day that changes, the suite will notice.

## Ordering and cleanup

Classes run in a fixed order, ending with the webhook subscription — a subscription changes how
MAX delivers updates for the whole bot, and whether long polling keeps working alongside one is
not documented, so everything else finishes first. The subscription is removed in teardown even
when the test fails.

Each class restores what it changed: the bot profile, the chat title, posted messages, the pin,
the subscription. Cleanup failures are printed and swallowed so they never mask the real error.

An interrupted run can still leave a subscription behind, and while one exists MAX delivers
updates to it instead of to long polling — which looks like a broken consumer, not like a
leftover. The long-polling classes therefore remove any subscription whose URL ends with
`/max-bot/live-test` before they start, and print a warning for any other subscription they
find.
