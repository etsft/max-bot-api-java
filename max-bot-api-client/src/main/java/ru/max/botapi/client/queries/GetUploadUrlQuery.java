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
import ru.max.botapi.model.UploadEndpoint;
import ru.max.botapi.model.UploadType;

/**
 * Query for {@code POST /uploads} — requests an upload URL for a file of the given type.
 *
 * <p>The upload type is a required parameter that specifies the media category.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * UploadEndpoint endpoint = api.getUploadUrl(UploadType.IMAGE).execute();
 * String uploadUrl = endpoint.url();
 * }</pre>
 */
public class GetUploadUrlQuery extends MaxQuery<UploadEndpoint> {

    /**
     * Creates a GetUploadUrlQuery.
     *
     * @param client the MAX client to execute this query
     * @param type   the upload type (image, video, audio, or file); must not be {@code null}
     */
    public GetUploadUrlQuery(MaxClient client, UploadType type) {
        super(client, "/uploads", HttpMethod.POST, UploadEndpoint.class);
        Objects.requireNonNull(type, "type must not be null");
        // Use UploadType.value() to get the canonical API string ("image", "video", etc.)
        // rather than name().toLowerCase() so that the mapping survives future enum renames.
        queryParams.put("type", type.value());
    }
}
