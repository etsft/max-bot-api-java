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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import ru.max.botapi.client.MaxApiException;
import ru.max.botapi.client.MaxRateLimitException;
import ru.max.botapi.model.AddMembersResult;
import ru.max.botapi.model.SimpleQueryResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs calls that MAX is expected to refuse and checks the refusal against the raw response.
 *
 * <p>Since schema 0.0.34 a response of HTTP 200 with {@code success:false} carries a
 * machine-readable {@code code}. {@link #probe} prints the HTTP status, the raw body and what
 * the library made of it, and for every {@code success:false} result checks that
 * {@code code()} is exactly the {@code code} in the raw body, or {@code null} when the body has
 * none. A body field outside {@link #FAILURE_FIELDS} is reported as a warning, not a failure:
 * it means the model may be missing something, not that the library misread what it models.</p>
 *
 * <p>Relies on {@link RecordingTransport} being installed, as {@link LiveApi} does.</p>
 */
final class RefusalChecks {

    /** Every field a {@code success:false} body may carry, per schema 0.0.34. */
    static final Set<String> FAILURE_FIELDS =
            Set.of("success", "message", "code", "failed_user_ids", "failed_user_details");

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final int HTTP_UNAUTHORIZED = 401;

    private static final int HTTP_SERVER_ERROR = 500;

    private RefusalChecks() {
    }

    /**
     * Runs one call, prints everything known about its response, and checks a
     * {@code success:false} result against the raw body.
     *
     * <p>A {@link MaxApiException} with a 4xx status other than 401 and 429 is printed rather
     * than thrown, and {@code null} returned:
     * whether a refusal comes as a 4xx or as {@code success:false} is part of what is observed.
     * A scenario that expects {@code success:false} fails on that {@code null} through
     * {@link #assertRefused}. A 401, a 429, a 5xx and any other exception — a response the model
     * cannot read, a transport failure — are thrown: they mean the model or the environment is
     * broken.</p>
     *
     * @param scenario what is being probed, for the output
     * @param call     the API call
     * @param <T>      the result type
     * @return the result, or {@code null} if MAX answered with a 4xx other than 401 and 429
     */
    static <T> T probe(String scenario, Supplier<T> call) {
        RecordingTransport.reset();
        System.out.println();
        System.out.println("--- " + scenario);
        try {
            T result = call.get();
            printWire();
            System.out.println("    result: " + describe(result));
            assertMatchesRawBody(result, scenario);
            return result;
        } catch (MaxApiException e) {
            printWire();
            System.out.println("    MaxApiException: statusCode=" + e.statusCode()
                    + " errorCode=" + e.errorCode() + " errorMessage=" + e.errorMessage());
            if (e instanceof MaxRateLimitException
                    || e.statusCode() == HTTP_UNAUTHORIZED || e.statusCode() >= HTTP_SERVER_ERROR) {
                // A bad token, a rate limit or a server failure says nothing about how MAX
                // refuses an operation, and returning null would let a scenario without a fixed
                // answer pass.
                throw e;
            }
            return null;
        } catch (RuntimeException e) {
            // A body the model cannot read, or a transport failure, is not an answer from MAX:
            // printed for context, then thrown, so the test fails rather than passes on it.
            printWire();
            throw e;
        }
    }

    /**
     * Asserts that a call came back as HTTP 200 with {@code success:false}.
     *
     * @param result   the result, {@code null} if the call threw
     * @param scenario what was probed, for the failure message
     */
    static void assertRefused(SimpleQueryResult result, String scenario) {
        assertThat(result)
                .as("%s threw instead of answering success=false; see the output above", scenario)
                .isNotNull();
        assertThat(result.success()).as("%s: success", scenario).isFalse();
    }

    /**
     * Asserts that a call was refused with {@code success:false} and some {@code code}.
     *
     * @param result   the result, {@code null} if the call threw
     * @param scenario what was probed, for the failure message
     */
    static void assertRefusedWithCode(SimpleQueryResult result, String scenario) {
        assertRefused(result, scenario);
        assertThat(result.code()).as("%s: code", scenario).isNotNull();
    }

    /**
     * Checks a {@code success:false} result against the raw body it was read from.
     *
     * @param result   what the library returned
     * @param scenario what was probed, for the failure message
     */
    private static void assertMatchesRawBody(Object result, String scenario) {
        String code;
        String message;
        AddMembersResult members = null;
        if (result instanceof SimpleQueryResult r && !r.success()) {
            code = r.code();
            message = r.message();
        } else if (result instanceof AddMembersResult r && !r.success()) {
            code = r.code();
            message = r.message();
            members = r;
        } else {
            return;
        }
        String raw = RecordingTransport.lastBody();
        JsonNode root;
        try {
            root = MAPPER.readTree(raw);
        } catch (JacksonException e) {
            throw new AssertionError(scenario + ": the body is not JSON: " + raw, e);
        }
        assertThat(root.isObject()).as("%s: body %s", scenario, raw).isTrue();

        Set<String> unknown = new TreeSet<>(root.propertyNames());
        unknown.removeAll(FAILURE_FIELDS);
        if (!unknown.isEmpty()) {
            System.out.println("    WARNING: success=false carries " + unknown
                    + ", which the model does not know");
        }

        assertThat(code).as("%s: code() against body %s", scenario, raw)
                .isEqualTo(text(root.get("code")));
        assertThat(message).as("%s: message() against body %s", scenario, raw)
                .isEqualTo(text(root.get("message")));
        if (members != null) {
            assertThat(members.failedUserIds())
                    .as("%s: failedUserIds() against body %s", scenario, raw)
                    .isEqualTo(longs(root.get("failed_user_ids")));
            List<String> details = members.failedUserDetails() == null ? null
                    : members.failedUserDetails().stream()
                            .map(f -> f.errorCode() + " " + f.userIds())
                            .toList();
            assertThat(details).as("%s: failedUserDetails() against body %s", scenario, raw)
                    .isEqualTo(details(root.get("failed_user_details")));
        }
    }

    /**
     * Reads a string field the way the model does: absent or JSON {@code null} is {@code null}.
     *
     * @param node the field, or {@code null} if absent
     * @return its text, or {@code null}
     */
    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asString();
    }

    /**
     * Reads an array of ids the way the model does: absent or {@code null} is {@code null},
     * and {@code null} elements are dropped.
     *
     * @param node the array, or {@code null} if absent
     * @return the ids, or {@code null}
     */
    private static List<Long> longs(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        List<Long> ids = new ArrayList<>();
        for (JsonNode element : node) {
            if (!element.isNull()) {
                ids.add(element.asLong());
            }
        }
        return ids;
    }

    /**
     * Renders {@code failed_user_details} as {@code "<error_code> <user_ids>"} per entry, the
     * same way {@link #assertMatchesRawBody} renders the model's entries.
     *
     * @param node the array, or {@code null} if absent
     * @return one string per entry, or {@code null}
     */
    private static List<String> details(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        List<String> details = new ArrayList<>();
        for (JsonNode element : node) {
            if (!element.isNull()) {
                details.add(text(element.get("error_code")) + " " + longs(element.get("user_ids")));
            }
        }
        return details;
    }

    private static void printWire() {
        System.out.println("    HTTP " + RecordingTransport.lastStatus());
        System.out.println("    body: " + RecordingTransport.lastBody());
    }

    private static String describe(Object result) {
        if (result instanceof AddMembersResult r) {
            return "success=" + r.success() + " code=" + r.code() + " message=" + r.message()
                    + " failedUserIds=" + r.failedUserIds()
                    + " failedUserDetails=" + r.failedUserDetails();
        }
        if (result instanceof SimpleQueryResult r) {
            return "success=" + r.success() + " code=" + r.code() + " message=" + r.message();
        }
        return String.valueOf(result);
    }
}
