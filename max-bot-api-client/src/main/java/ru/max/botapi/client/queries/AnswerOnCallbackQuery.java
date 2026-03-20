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
import ru.max.botapi.model.CallbackAnswer;
import ru.max.botapi.model.SimpleQueryResult;

/**
 * Query for {@code POST /answers} — sends an answer to a callback query from an inline button.
 *
 * <p>{@code callbackId} is a required parameter: the MAX API will reject the request with HTTP 400
 * if it is absent. It is therefore a constructor argument, not a fluent setter.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * api.answerOnCallback(new CallbackAnswer(false, "Done!"), "callback-id-123").execute();
 * }</pre>
 */
public class AnswerOnCallbackQuery extends MaxQuery<SimpleQueryResult> {

    /**
     * Creates an AnswerOnCallbackQuery.
     *
     * @param client     the MAX client to execute this query
     * @param answer     the callback answer; must not be {@code null}
     * @param callbackId the ID of the callback to answer; must not be {@code null}
     */
    public AnswerOnCallbackQuery(MaxClient client, CallbackAnswer answer, String callbackId) {
        super(client, "/answers", HttpMethod.POST, SimpleQueryResult.class);
        this.body = Objects.requireNonNull(answer, "answer must not be null");
        queryParams.put("callback_id",
                Objects.requireNonNull(callbackId, "callbackId must not be null"));
    }
}
