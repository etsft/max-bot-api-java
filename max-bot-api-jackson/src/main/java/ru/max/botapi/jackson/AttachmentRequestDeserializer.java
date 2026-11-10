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

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import ru.max.botapi.model.AttachmentRequest;
import ru.max.botapi.model.ModelTypes;
import ru.max.botapi.model.UnknownAttachmentRequest;

/**
 * Custom Jackson deserializer for the {@link AttachmentRequest} sealed hierarchy.
 *
 * <p>Uses the {@code type} JSON field as a discriminator to resolve the concrete
 * attachment request type. Unknown types produce {@link UnknownAttachmentRequest}.</p>
 */
final class AttachmentRequestDeserializer extends StdDeserializer<AttachmentRequest> {

    AttachmentRequestDeserializer() {
        super(AttachmentRequest.class);
    }

    @Override
    public AttachmentRequest deserialize(JsonParser p, DeserializationContext ctxt) {
        JsonNode node = ctxt.readTree(p);
        String type = LenientReads.discriminator(node, "type");
        Class<? extends AttachmentRequest> requestType = ModelTypes.attachmentRequests().get(type);
        return requestType == null
                ? new UnknownAttachmentRequest(type, node.toString())
                : ctxt.readTreeAsValue(node, requestType);
    }
}
