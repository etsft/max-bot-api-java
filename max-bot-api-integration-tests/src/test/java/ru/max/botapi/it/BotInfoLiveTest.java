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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import ru.max.botapi.model.BotCommand;
import ru.max.botapi.model.BotInfo;
import ru.max.botapi.model.BotPatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code GET /me} and {@code PATCH /me} against the live API.
 *
 * <p>The profile edit is a real change to the bot everyone sees, so the original values are
 * captured before the first edit and written back in teardown.</p>
 */
@Order(1)
@DisplayName("Live: bot profile")
@SuppressWarnings("deprecation") // exercises the endpoints MAX no longer documents
class BotInfoLiveTest extends LiveTestBase {

    private BotInfo original;

    @Test
    @Order(1)
    @DisplayName("getMyInfo returns the bot")
    void getMyInfo() {
        BotInfo info = api().getMyInfo().execute();

        assertThat(info.userId()).isPositive();
        assertThat(info.name()).isNotBlank();
        assertThat(info.isBot()).isTrue();

        original = info;
        System.out.println("Bot: " + info.name() + " (id " + info.userId() + ")");
    }

    @Test
    @Order(2)
    @DisplayName("editMyInfo updates the description and commands")
    void editMyInfo() {
        String description = "Live integration test run " + System.currentTimeMillis();
        List<BotCommand> commands = List.of(new BotCommand("ping", "Health check"));

        BotInfo updated = api()
                .editMyInfo(new BotPatch(null, description, commands, null))
                .execute();

        assertThat(updated.description()).isEqualTo(description);
        assertThat(updated.commands()).isNotNull();
        assertThat(updated.commands()).extracting(BotCommand::name).contains("ping");
    }

    /**
     * Restores whatever the profile looked like before the run.
     */
    @AfterAll
    void restoreProfile() {
        if (original == null) {
            return;
        }
        cleanUp("restore bot profile", () -> api()
                .editMyInfo(new BotPatch(original.name(), original.description(),
                        original.commands(), null))
                .execute());
    }
}
