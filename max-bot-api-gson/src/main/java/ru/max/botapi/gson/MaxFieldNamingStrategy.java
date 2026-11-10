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

package ru.max.botapi.gson;

import java.lang.reflect.Field;
import java.util.Map;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;

import ru.max.botapi.model.ChatPatch;
import ru.max.botapi.model.FileUploadedInfo;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.PinMessageBody;
import ru.max.botapi.model.VideoUrls;

/**
 * Maps Java field names to {@code snake_case} JSON names, with the exceptions the MAX API needs.
 *
 * <p>The core model records carry no serialization annotations, so the exceptions that the
 * Jackson adapter expresses as mix-ins live here:</p>
 * <ul>
 *   <li>{@code notifyRecipients} is {@code "notify"} on the wire — the Java name avoids
 *       clashing with {@link Object#notify()}</li>
 *   <li>{@link FileUploadedInfo#fileId()} is {@code "fileId"} verbatim — the upload endpoint
 *       is the one camelCase response in the API</li>
 *   <li>{@link VideoUrls} renditions are {@code "mp4_1080"} and so on — snake_case only
 *       inserts underscores before capitals, and checkstyle rejects underscores in member names</li>
 * </ul>
 */
final class MaxFieldNamingStrategy implements FieldNamingStrategy {

    private static final Map<Class<?>, Map<String, String>> OVERRIDES = Map.of(
            NewMessageBody.class, Map.of("notifyRecipients", "notify"),
            PinMessageBody.class, Map.of("notifyRecipients", "notify"),
            ChatPatch.class, Map.of("notifyRecipients", "notify"),
            FileUploadedInfo.class, Map.of("fileId", "fileId"),
            VideoUrls.class, Map.of(
                    "mp41080", "mp4_1080",
                    "mp4720", "mp4_720",
                    "mp4480", "mp4_480",
                    "mp4360", "mp4_360",
                    "mp4240", "mp4_240",
                    "mp4144", "mp4_144"));

    @Override
    public String translateName(Field field) {
        Map<String, String> overrides = OVERRIDES.get(field.getDeclaringClass());
        if (overrides != null) {
            String name = overrides.get(field.getName());
            if (name != null) {
                return name;
            }
        }
        return FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES.translateName(field);
    }
}
