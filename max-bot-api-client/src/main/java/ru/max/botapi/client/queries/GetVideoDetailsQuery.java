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

package ru.max.botapi.client.queries;

import java.util.Objects;

import ru.max.botapi.client.HttpMethod;
import ru.max.botapi.client.MaxClient;
import ru.max.botapi.client.MaxQuery;
import ru.max.botapi.model.VideoAttachmentDetails;

/**
 * Query for {@code GET /videos/{videoToken}} — retrieves details of a video attachment
 * by its token.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * VideoAttachmentDetails details = api.getVideoAttachmentDetails("video-token-abc").execute();
 * }</pre>
 */
public class GetVideoDetailsQuery extends MaxQuery<VideoAttachmentDetails> {

    /**
     * Creates a GetVideoDetailsQuery.
     *
     * @param client     the MAX client to execute this query
     * @param videoToken the video token; must not be {@code null}
     */
    public GetVideoDetailsQuery(MaxClient client, String videoToken) {
        super(client,
                "/videos/" + Objects.requireNonNull(videoToken, "videoToken must not be null"),
                HttpMethod.GET, VideoAttachmentDetails.class);
    }
}
