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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the Button sealed hierarchy.
 */
class ButtonTest {

    @Test
    void callbackButton_construction() {
        var btn = new CallbackButton("Click", "data", ButtonIntent.POSITIVE);
        assertThat(btn.type()).isEqualTo("callback");
        assertThat(btn.text()).isEqualTo("Click");
        assertThat(btn.payload()).isEqualTo("data");
        assertThat(btn.intent()).isEqualTo(ButtonIntent.POSITIVE);
    }

    @Test
    void linkButton_construction() {
        var btn = new LinkButton("Open", "http://example.com");
        assertThat(btn.type()).isEqualTo("link");
        assertThat(btn.url()).isEqualTo("http://example.com");
    }

    @Test
    void requestContactButton_construction() {
        var btn = new RequestContactButton("Share Contact");
        assertThat(btn.type()).isEqualTo("request_contact");
        assertThat(btn.text()).isEqualTo("Share Contact");
    }

    @Test
    void requestGeoLocationButton_construction() {
        var btn = new RequestGeoLocationButton("Share Location", true);
        assertThat(btn.type()).isEqualTo("request_geo_location");
        assertThat(btn.quick()).isTrue();
    }

    @Test
    void chatButton_construction() {
        var btn = new ChatButton("Create Chat", "My Chat",
                "desc", "start_payload", "uuid-123");
        assertThat(btn.type()).isEqualTo("chat");
        assertThat(btn.chatTitle()).isEqualTo("My Chat");
    }

    @Test
    void openAppButton_construction() {
        var btn = new OpenAppButton("Open App", "http://app.com", "p");
        assertThat(btn.type()).isEqualTo("open_app");
        assertThat(btn.url()).isEqualTo("http://app.com");
    }

    @Test
    void messageButton_construction() {
        var btn = new MessageButton("Say Hi", "Hello bot!");
        assertThat(btn.type()).isEqualTo("message");
        assertThat(btn.message()).isEqualTo("Hello bot!");
    }

    @Test
    void unknownButton_construction() {
        var btn = new UnknownButton("future", "Future Btn", "{}");
        assertThat(btn.type()).isEqualTo("future");
        assertThat(btn.text()).isEqualTo("Future Btn");
    }

    @Test
    void exhaustiveSwitch_coversAllTypes() {
        Button[] all = {
                new CallbackButton("t", "p", null),
                new LinkButton("t", "u"),
                new RequestContactButton("t"),
                new RequestGeoLocationButton("t", null),
                new ChatButton("t", "ct", null, null, null),
                new OpenAppButton("t", "u", null),
                new MessageButton("t", "m"),
                new UnknownButton("x", "t", "{}")
        };
        for (Button btn : all) {
            String desc = switch (btn) {
                case CallbackButton b -> "callback";
                case LinkButton b -> "link";
                case RequestContactButton b -> "contact";
                case RequestGeoLocationButton b -> "geo";
                case ChatButton b -> "chat";
                case OpenAppButton b -> "app";
                case MessageButton b -> "message";
                case UnknownButton b -> "unknown";
            };
            assertThat(desc).isNotBlank();
        }
    }

    @Test
    void button_equality() {
        var b1 = new CallbackButton("OK", "accept", ButtonIntent.POSITIVE);
        var b2 = new CallbackButton("OK", "accept", ButtonIntent.POSITIVE);
        assertThat(b1).isEqualTo(b2);
        assertThat(b1.hashCode()).isEqualTo(b2.hashCode());
    }
}
