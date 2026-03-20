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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import ru.max.botapi.client.HttpMethod;
import ru.max.botapi.client.MaxClient;
import ru.max.botapi.client.MaxQuery;
import ru.max.botapi.model.Chat;

/**
 * Query for {@code GET /chats/{chatLink}} — retrieves information about a chat by its public link.
 *
 * <p>The {@code chatLink} is percent-encoded as a path segment before being appended to the
 * path. This uses {@link java.net.URLEncoder} with a {@code +} → {@code %20} replacement so
 * that spaces are encoded as {@code %20} (correct for path segments per RFC 3986) rather than
 * {@code +} (which is correct only for {@code application/x-www-form-urlencoded} query strings).
 * This ensures links containing Unicode characters or special characters (spaces, {@code #},
 * etc.) do not produce a malformed URI when the transport layer calls {@code URI.create()}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Chat chat = api.getChatByLink("my-public-chat").execute();
 * }</pre>
 */
public class GetChatByLinkQuery extends MaxQuery<Chat> {

    /**
     * Creates a GetChatByLinkQuery.
     *
     * @param client   the MAX client to execute this query
     * @param chatLink the public chat link or username; must not be {@code null}
     */
    public GetChatByLinkQuery(MaxClient client, String chatLink) {
        super(client,
                "/chats/" + encodePathSegment(
                        Objects.requireNonNull(chatLink, "chatLink must not be null")),
                HttpMethod.GET, Chat.class);
    }

    /**
     * Percent-encodes a string for use as a URL path segment.
     *
     * <p>{@link URLEncoder#encode(String, java.nio.charset.Charset)} produces
     * {@code application/x-www-form-urlencoded} encoding where spaces become {@code +}.
     * RFC 3986 path segments require spaces to be {@code %20}, so we replace {@code +}
     * after encoding.</p>
     *
     * @param segment the raw path segment value
     * @return the percent-encoded segment safe for inclusion in a URL path
     */
    private static String encodePathSegment(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
