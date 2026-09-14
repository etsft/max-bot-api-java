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

import tools.jackson.databind.module.SimpleModule;

import ru.max.botapi.model.Attachment;
import ru.max.botapi.model.AttachmentRequest;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.ChatPatch;
import ru.max.botapi.model.FileUploadedInfo;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.PinMessageBody;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateList;
import ru.max.botapi.model.VideoUrls;

/**
 * Custom Jackson module that registers deserializers for sealed type hierarchies
 * and serializers that include type discriminator fields.
 *
 * <p>Registered types:</p>
 * <ul>
 *   <li>{@link Update} — discriminated by {@code update_type}</li>
 *   <li>{@link UpdateList} — deserialized element by element, skipping malformed updates</li>
 *   <li>{@link Attachment} — discriminated by {@code type}</li>
 *   <li>{@link AttachmentRequest} — discriminated by {@code type}</li>
 *   <li>{@link Button} — discriminated by {@code type}</li>
 * </ul>
 */
public class MaxBotApiModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the module and registers all custom serializers and deserializers.
     */
    @SuppressWarnings("this-escape") // Safe: SimpleModule constructor is fully initialized
    public MaxBotApiModule() {
        super("MaxBotApiModule");

        addDeserializer(Update.class, new UpdateDeserializer());
        addDeserializer(UpdateList.class, new UpdateListDeserializer());
        addDeserializer(Attachment.class, new AttachmentDeserializer());
        addDeserializer(AttachmentRequest.class, new AttachmentRequestDeserializer());
        addDeserializer(Button.class, new ButtonDeserializer());

        addSerializer(new SealedTypeSerializer<>(Attachment.class, Attachment::type));
        addSerializer(new SealedTypeSerializer<>(AttachmentRequest.class, AttachmentRequest::type));
        addSerializer(new SealedTypeSerializer<>(Button.class, Button::type));
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);

        context.setMixIn(NewMessageBody.class, NotifyFieldMixIn.class);
        context.setMixIn(PinMessageBody.class, NotifyFieldMixIn.class);
        context.setMixIn(ChatPatch.class, NotifyFieldMixIn.class);
        context.setMixIn(FileUploadedInfo.class, FileUploadedInfoMixIn.class);
        context.setMixIn(VideoUrls.class, VideoUrlsMixIn.class);
    }
}
