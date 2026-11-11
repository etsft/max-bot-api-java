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

package ru.max.botapi.core;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.max.botapi.model.BotStartedUpdate;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.UnknownUpdate;
import ru.max.botapi.model.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateRouterTest {

    private static final UnknownUpdate UNKNOWN = new UnknownUpdate("warp", 1L, "{}");

    @Test
    void dispatchesEachUpdateToTheHandlerForItsType() {
        List<String> seen = new ArrayList<>();
        UpdateRouter router = UpdateRouter.builder()
                .on(UnknownUpdate.class, u -> seen.add("unknown:" + u.updateType()))
                .on(BotStartedUpdate.class, u -> seen.add("started"))
                .build();

        router.onUpdate(UNKNOWN);

        assertThat(seen).containsExactly("unknown:warp");
    }

    @Test
    void unroutedUpdatesGoToTheFallback() {
        List<Update> fallback = new ArrayList<>();
        UpdateRouter router = UpdateRouter.builder()
                .on(MessageCreatedUpdate.class, u -> { })
                .otherwise(fallback::add)
                .build();

        router.onUpdate(UNKNOWN);

        assertThat(fallback).containsExactly(UNKNOWN);
    }

    @Test
    void unroutedUpdatesAreIgnoredByDefault() {
        UpdateRouter.builder().build().onUpdate(UNKNOWN);
    }

    @Test
    void rejectsTheSealedInterfaceAndDuplicateRoutes() {
        assertThatThrownBy(() -> UpdateRouter.builder().on(Update.class, u -> { }))
                .isInstanceOf(IllegalArgumentException.class);
        UpdateRouter.Builder builder = UpdateRouter.builder().on(UnknownUpdate.class, u -> { });
        assertThatThrownBy(() -> builder.on(UnknownUpdate.class, u -> { }))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
