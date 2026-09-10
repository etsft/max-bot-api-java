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
 * Query for {@code GET /chats/{chatLink}} — retrieves information about a chat by its public
 * link, or the dialog with a user by their username.
 *
 * <p>The API routes on a single path segment, so only the link itself may be sent: a full URL
 * makes MAX answer {@code method.not.found} because the encoded slashes do not match the route.
 * The argument is therefore normalized first — {@code "@my_chat"}, {@code "my_chat"},
 * {@code "max.ru/my_chat"} and {@code "https://max.ru/my_chat"} all address the same chat — and
 * only then percent-encoded as a path segment.</p>
 *
 * <p>Encoding uses {@link java.net.URLEncoder} with a {@code +} → {@code %20} replacement so
 * that spaces are encoded as {@code %20} (correct for path segments per RFC 3986) rather than
 * {@code +} (which is correct only for {@code application/x-www-form-urlencoded} query strings).
 * This ensures links containing Unicode characters or special characters (spaces, {@code #},
 * etc.) do not produce a malformed URI when the transport layer calls {@code URI.create()}.</p>
 *
 * <p>An invite link ({@code https://max.ru/join/...}) is not a public link and cannot be
 * resolved by this endpoint at all; it is rejected here rather than sent and 404'd.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Chat chat = api.getChatByLink("@my-public-chat").execute();
 * }</pre>
 *
 * @deprecated {@code GET /chats/{chatLink}} is not part of the MAX API documentation and may
 *             stop working without notice.
 */
@Deprecated
public class GetChatByLinkQuery extends MaxQuery<Chat> {

    private static final String INVITE_PREFIX = "join/";

    /**
     * Creates a GetChatByLinkQuery.
     *
     * @param client   the MAX client to execute this query
     * @param chatLink the public chat link or username; must not be {@code null}. Accepted as
     *                 {@code "@name"}, {@code "name"} or a {@code https://max.ru/name} URL
     * @throws IllegalArgumentException if the link is blank, is an invite link, or still holds
     *                                  a path after the host has been stripped
     */
    public GetChatByLinkQuery(MaxClient client, String chatLink) {
        super(client,
                "/chats/" + encodePathSegment(normalize(
                        Objects.requireNonNull(chatLink, "chatLink must not be null"))),
                HttpMethod.GET, Chat.class);
    }

    /**
     * Reduces an accepted link form to the single path segment the API routes on.
     *
     * @param chatLink the raw link as given by the caller
     * @return the bare link, {@code @} kept if it was there
     */
    private static String normalize(String chatLink) {
        String link = chatLink.trim();
        int schemeEnd = link.indexOf("://");
        if (schemeEnd >= 0) {
            link = link.substring(schemeEnd + "://".length());
        }
        int firstSlash = link.indexOf('/');
        if (firstSlash >= 0 && link.lastIndexOf('.', firstSlash) >= 0) {
            // "max.ru/name" and the like: everything up to the first slash is a host.
            link = link.substring(firstSlash + 1);
        }
        while (link.endsWith("/")) {
            link = link.substring(0, link.length() - 1);
        }
        if (link.isBlank()) {
            throw new IllegalArgumentException("chatLink must not be blank: " + chatLink);
        }
        if (link.startsWith(INVITE_PREFIX)) {
            throw new IllegalArgumentException(
                    "chatLink is an invite link (" + chatLink + "); GET /chats/{chatLink} "
                            + "resolves public links and usernames only, such as \"@my_chat\"");
        }
        if (link.indexOf('/') >= 0) {
            throw new IllegalArgumentException(
                    "chatLink must be a single path segment, got: " + chatLink);
        }
        return link;
    }

    /**
     * Percent-encodes a string for use as a URL path segment.
     *
     * <p>{@link URLEncoder#encode(String, java.nio.charset.Charset)} produces
     * {@code application/x-www-form-urlencoded} encoding where spaces become {@code +}.
     * RFC 3986 path segments require spaces to be {@code %20}, so we replace {@code +}
     * after encoding.</p>
     *
     * <p>A leading {@code @} is kept verbatim: it is a legal path character (RFC 3986
     * {@code pchar}) and MAX matches the link as the user wrote it.</p>
     *
     * @param segment the raw path segment value
     * @return the percent-encoded segment safe for inclusion in a URL path
     */
    private static String encodePathSegment(String segment) {
        if (segment.startsWith("@")) {
            return "@" + encodePathSegment(segment.substring(1));
        }
        return URLEncoder.encode(segment, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
