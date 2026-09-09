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

package ru.max.botapi.jackson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateList;

/**
 * Lenient Jackson deserializer for {@link UpdateList}.
 *
 * <p>Reads the {@code marker} first and then each element of {@code updates}
 * independently, so that an element the model cannot represent is dropped with a
 * warning instead of failing the whole batch. Recovering the marker matters for
 * long polling: a batch that fails to deserialize leaves the marker unadvanced,
 * and the server replays the same batch forever.</p>
 *
 * <p>Most malformed elements never reach this class — {@link UpdateDeserializer}
 * already degrades them to {@code UnknownUpdate}. This is the backstop for
 * failures it cannot intercept, such as a non-object element.</p>
 */
final class UpdateListDeserializer extends StdDeserializer<UpdateList> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(UpdateListDeserializer.class);

    UpdateListDeserializer() {
        super(UpdateList.class);
    }

    @Override
    public UpdateList deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        JsonNode markerNode = node.get("marker");
        Long marker = markerNode != null && markerNode.isNumber() ? markerNode.asLong() : null;

        List<Update> updates = new ArrayList<>();
        JsonNode updatesNode = node.get("updates");
        if (updatesNode != null && updatesNode.isArray()) {
            for (JsonNode element : updatesNode) {
                try {
                    updates.add(ctxt.readTreeAsValue(element, Update.class));
                } catch (IOException | RuntimeException e) {
                    LOG.warn("Skipping an update that could not be deserialized. Raw JSON: {}",
                            LenientReads.truncate(element), e);
                }
            }
        } else if (updatesNode != null && !updatesNode.isNull()) {
            LOG.warn("Expected 'updates' to be an array, got {}; treating the batch as empty",
                    updatesNode.getNodeType());
        }

        return new UpdateList(updates, marker);
    }
}
