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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateList;

/**
 * Lenient Gson adapter factory for {@link UpdateList}.
 *
 * <p>Reads the {@code marker} first and then each element of {@code updates}
 * independently, so that an element the model cannot represent is dropped with a
 * warning instead of failing the whole batch. Recovering the marker matters for
 * long polling: a batch that fails to deserialize leaves the marker unadvanced,
 * and the server replays the same batch forever.</p>
 *
 * <p>Most malformed elements never reach this class — the {@link Update} adapter
 * already degrades them to {@code UnknownUpdate}. This is the backstop for
 * failures it cannot intercept, such as a non-object element.</p>
 */
final class UpdateListAdapterFactory implements TypeAdapterFactory {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateListAdapterFactory.class);

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        if (typeToken.getRawType() != UpdateList.class) {
            return null;
        }
        TypeAdapter<UpdateList> delegate = gson.getDelegateAdapter(this, TypeToken.get(UpdateList.class));
        return (TypeAdapter<T>) new UpdateListAdapter(delegate, gson.getAdapter(JsonElement.class),
                gson.getAdapter(Update.class));
    }

    private static final class UpdateListAdapter extends TypeAdapter<UpdateList> {

        private final TypeAdapter<UpdateList> delegate;
        private final TypeAdapter<JsonElement> elementAdapter;
        private final TypeAdapter<Update> updateAdapter;

        UpdateListAdapter(TypeAdapter<UpdateList> delegate, TypeAdapter<JsonElement> elementAdapter,
                TypeAdapter<Update> updateAdapter) {
            this.delegate = delegate;
            this.elementAdapter = elementAdapter;
            this.updateAdapter = updateAdapter;
        }

        @Override
        public void write(JsonWriter out, UpdateList value) throws IOException {
            delegate.write(out, value);
        }

        @Override
        public UpdateList read(JsonReader in) throws IOException {
            JsonElement node = elementAdapter.read(in);
            if (node.isJsonNull()) {
                return null;
            }
            JsonObject object = node.isJsonObject() ? node.getAsJsonObject() : new JsonObject();

            JsonElement markerNode = object.get("marker");
            Long marker = markerNode != null && markerNode.isJsonPrimitive()
                    && markerNode.getAsJsonPrimitive().isNumber() ? markerNode.getAsLong() : null;

            List<Update> updates = new ArrayList<>();
            JsonElement updatesNode = object.get("updates");
            if (updatesNode != null && updatesNode.isJsonArray()) {
                for (JsonElement element : updatesNode.getAsJsonArray()) {
                    readInto(updates, element);
                }
            } else if (updatesNode != null && !updatesNode.isJsonNull()) {
                LOG.warn("Expected 'updates' to be an array, got {}; treating the batch as empty",
                        LenientReads.truncate(updatesNode));
            }

            return new UpdateList(updates, marker);
        }

        private void readInto(List<Update> updates, JsonElement element) {
            try {
                Update update = updateAdapter.fromJsonTree(element);
                if (update != null) {
                    updates.add(update);
                } else {
                    LOG.warn("Skipping a null update");
                }
            } catch (RuntimeException e) {
                LOG.warn("Skipping an update that could not be deserialized. Raw JSON: {}",
                        LenientReads.truncate(element), e);
            }
        }
    }
}
