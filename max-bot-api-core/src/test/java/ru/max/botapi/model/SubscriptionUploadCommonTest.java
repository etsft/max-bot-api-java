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

/**
 * Tests for Subscription, Upload, and Common response types.
 */
class SubscriptionUploadCommonTest {

    @Test
    void subscription_construction() {
        var sub = new Subscription("http://hook", List.of(UpdateType.MESSAGE_CREATED));
        assertThat(sub.url()).isEqualTo("http://hook");
        assertThat(sub.updateTypes()).containsExactly(UpdateType.MESSAGE_CREATED);
    }

    @Test
    void subscriptionRequestBody_construction() {
        var body = new SubscriptionRequestBody("http://hook", null, "secret");
        assertThat(body.url()).isEqualTo("http://hook");
        assertThat(body.secret()).isEqualTo("secret");
    }

    @Test
    void getSubscriptionsResult_construction() {
        var sub = new Subscription("http://hook", null);
        var result = new GetSubscriptionsResult(List.of(sub));
        assertThat(result.subscriptions()).hasSize(1);
    }

    @Test
    void uploadEndpoint_construction() {
        var ep = new UploadEndpoint("http://upload", "pre-tok");
        assertThat(ep.url()).isEqualTo("http://upload");
        assertThat(ep.token()).isEqualTo("pre-tok");
    }

    @Test
    void fileUploadedInfo_construction() {
        var info = new FileUploadedInfo(3343344796L, "file-tok");
        assertThat(info.fileId()).isEqualTo(3343344796L);
        assertThat(info.token()).isEqualTo("file-tok");
        assertThat((UploadedInfo) info).isInstanceOf(UploadedInfo.class);
    }

    @Test
    void imageUploadedInfo_construction() {
        var photos = java.util.Map.of(
                "key1", new PhotoAttachmentRequestPayload.TokenRef("img-tok-1"));
        var info = new ImageUploadedInfo(photos);
        assertThat(info.photos()).hasSize(1);
        assertThat(info.photos().get("key1").token()).isEqualTo("img-tok-1");
    }

    @Test
    void mediaUploadedInfo_construction() {
        var info = new MediaUploadedInfo("video-tok", 1);
        assertThat(info.token()).isEqualTo("video-tok");
        assertThat(info.retval()).isEqualTo(1);
    }

    @Test
    void uploadedInfo_isSealedAndExhaustive() {
        UploadedInfo file = new FileUploadedInfo(1L, "t");
        String description = switch (file) {
            case FileUploadedInfo f -> "file:" + f.fileId();
            case ImageUploadedInfo i -> "image:" + i.photos().size();
            case MediaUploadedInfo m -> "media:" + m.retval();
        };
        assertThat(description).isEqualTo("file:1");
    }

    @Test
    void videoAttachmentDetails_construction() {
        var details = new VideoAttachmentDetails(
                "http://vid", "vtok", new VideoThumbnail("http://thumb"), 1920, 1080, 120);
        assertThat(details.url()).isEqualTo("http://vid");
        assertThat(details.thumbnail().url()).isEqualTo("http://thumb");
        assertThat(details.duration()).isEqualTo(120);
    }

    @Test
    void simpleQueryResult_success() {
        var result = new SimpleQueryResult(true, "ok");
        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("ok");
    }

    @Test
    void simpleQueryResult_failure() {
        var result = new SimpleQueryResult(false, null);
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isNull();
    }

    @Test
    void callbackAnswer_construction() {
        var answer = new CallbackAnswer(null, "Notification text");
        assertThat(answer.message()).isNull();
        assertThat(answer.notification()).isEqualTo("Notification text");
    }

    @Test
    void updateList_construction() {
        var list = new UpdateList(List.of(), 42L);
        assertThat(list.updates()).isEmpty();
        assertThat(list.marker()).isEqualTo(42L);
    }

    @Test
    void getPinnedMessageResult_construction() {
        var result = new GetPinnedMessageResult(null);
        assertThat(result.message()).isNull();
    }

    @Test
    void uploadType_values() {
        assertThat(UploadType.values()).hasSize(4);
    }
}
