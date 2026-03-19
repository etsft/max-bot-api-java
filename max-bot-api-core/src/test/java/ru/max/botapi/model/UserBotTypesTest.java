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
 * Tests for User, UserWithPhoto, BotInfo, BotCommand, BotPatch records.
 */
class UserBotTypesTest {

    @Test
    void user_construction() {
        var user = new User(1L, "Alice", "@alice", false, 1000L);
        assertThat(user.userId()).isEqualTo(1L);
        assertThat(user.name()).isEqualTo("Alice");
        assertThat(user.username()).isEqualTo("@alice");
        assertThat(user.isBot()).isFalse();
        assertThat(user.lastActivityTime()).isEqualTo(1000L);
    }

    @Test
    void user_nullableUsername() {
        var user = new User(2L, "Bob", null, true, 2000L);
        assertThat(user.username()).isNull();
    }

    @Test
    void user_nullName_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new User(1L, null, null, false, 0L));
    }

    @Test
    void user_equalityAndHashCode() {
        var u1 = new User(1L, "Alice", null, false, 100L);
        var u2 = new User(1L, "Alice", null, false, 100L);
        assertThat(u1).isEqualTo(u2);
        assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
    }

    @Test
    void user_notEqual_differentFields() {
        var u1 = new User(1L, "Alice", null, false, 100L);
        var u2 = new User(2L, "Alice", null, false, 100L);
        assertThat(u1).isNotEqualTo(u2);
    }

    @Test
    void userWithPhoto_construction() {
        var user = new UserWithPhoto(1L, "Alice", "@alice", false, 1000L,
                "desc", "http://avatar", "http://full");
        assertThat(user.description()).isEqualTo("desc");
        assertThat(user.avatarUrl()).isEqualTo("http://avatar");
        assertThat(user.fullAvatarUrl()).isEqualTo("http://full");
    }

    @Test
    void botCommand_construction() {
        var cmd = new BotCommand("help", "Show help");
        assertThat(cmd.name()).isEqualTo("help");
        assertThat(cmd.description()).isEqualTo("Show help");
    }

    @Test
    void botCommand_nulls_throw() {
        assertThatNullPointerException()
                .isThrownBy(() -> new BotCommand(null, "desc"));
        assertThatNullPointerException()
                .isThrownBy(() -> new BotCommand("name", null));
    }

    @Test
    void botInfo_construction() {
        var cmd = new BotCommand("start", "Start bot");
        var bot = new BotInfo(1L, "MyBot", "@mybot", true, 1000L,
                "A bot", "http://avatar", "http://full", List.of(cmd));
        assertThat(bot.commands()).hasSize(1);
        assertThat(bot.isBot()).isTrue();
    }

    @Test
    void botPatch_allNulls() {
        var patch = new BotPatch(null, null, null, null);
        assertThat(patch.name()).isNull();
        assertThat(patch.description()).isNull();
        assertThat(patch.commands()).isNull();
        assertThat(patch.photo()).isNull();
    }

    @Test
    void image_construction() {
        var img = new Image("http://example.com/img.png");
        assertThat(img.url()).isEqualTo("http://example.com/img.png");
    }

    @Test
    void image_nullUrl_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Image(null));
    }
}
