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

import java.util.List;

import ru.max.botapi.model.Attachment;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.ConstructedMessage;
import ru.max.botapi.model.InlineKeyboardAttachment;
import ru.max.botapi.model.LinkedMessage;
import ru.max.botapi.model.Message;
import ru.max.botapi.model.MessageBody;
import ru.max.botapi.model.MessageCallbackUpdate;
import ru.max.botapi.model.MessageConstructedUpdate;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.MessageEditedUpdate;
import ru.max.botapi.model.UnknownAttachment;
import ru.max.botapi.model.UnknownButton;
import ru.max.botapi.model.UnknownUpdate;
import ru.max.botapi.model.Update;
import ru.max.botapi.model.UpdateList;

/**
 * Assertions that the live response was fully mapped onto the library model.
 *
 * <p>The sealed hierarchies each carry a forward-compatibility fallback — {@code UnknownUpdate},
 * {@code UnknownAttachment}, {@code UnknownButton} — which keeps a bot running when the API
 * grows a type the library does not know. That is right in production and wrong here: this
 * suite exists to notice the drift, so a fallback anywhere in the returned graph is a failure,
 * reported together with the raw JSON that produced it.</p>
 */
public final class ModelAssertions {

    private ModelAssertions() {
    }

    /**
     * Asserts that every update in the batch was mapped onto a concrete type.
     *
     * @param updates the deserialized batch
     */
    public static void assertFullyMapped(UpdateList updates) {
        assertFullyMapped(updates.updates());
    }

    /**
     * Asserts that every update in the list was mapped onto a concrete type.
     *
     * @param updates the deserialized updates
     */
    public static void assertFullyMapped(List<Update> updates) {
        updates.forEach(ModelAssertions::assertFullyMapped);
    }

    /**
     * Asserts that the update and any message it carries were mapped onto concrete types.
     *
     * @param update the deserialized update
     */
    public static void assertFullyMapped(Update update) {
        switch (update) {
            case UnknownUpdate unknown -> throw drift(
                    "update_type '" + unknown.updateType() + "' was not mapped to a concrete type",
                    unknown.rawJson());
            case MessageCreatedUpdate u -> assertFullyMapped(u.message());
            case MessageEditedUpdate u -> assertFullyMapped(u.message());
            case MessageConstructedUpdate u -> assertFullyMapped(u.message());
            case MessageCallbackUpdate u -> {
                if (u.message() != null) {
                    assertFullyMapped(u.message());
                }
            }
            default -> {
                // Updates without a nested message have nothing further to walk.
            }
        }
    }

    /**
     * Asserts that the message, its attachments and any linked message were fully mapped.
     *
     * @param message the deserialized message
     */
    public static void assertFullyMapped(Message message) {
        assertFullyMapped(message.body());
        LinkedMessage link = message.link();
        if (link != null) {
            assertFullyMapped(link.message());
        }
    }

    /**
     * Asserts that a constructed message, its attachments and any linked message were mapped.
     *
     * @param message the deserialized constructed message
     */
    public static void assertFullyMapped(ConstructedMessage message) {
        assertFullyMapped(message.body());
        LinkedMessage link = message.link();
        if (link != null) {
            assertFullyMapped(link.message());
        }
    }

    /**
     * Asserts that every attachment of the body was mapped onto a concrete type.
     *
     * @param body the deserialized message body
     */
    public static void assertFullyMapped(MessageBody body) {
        List<Attachment> attachments = body.attachments();
        if (attachments == null) {
            return;
        }
        attachments.forEach(ModelAssertions::assertFullyMapped);
    }

    /**
     * Asserts that the attachment, and the buttons of an inline keyboard, were fully mapped.
     *
     * @param attachment the deserialized attachment
     */
    public static void assertFullyMapped(Attachment attachment) {
        if (attachment instanceof UnknownAttachment unknown) {
            throw drift("attachment type '" + unknown.type() + "' was not mapped to a concrete type",
                    unknown.rawJson());
        }
        if (attachment instanceof InlineKeyboardAttachment keyboard) {
            for (List<Button> row : keyboard.payload().buttons()) {
                row.forEach(ModelAssertions::assertFullyMapped);
            }
        }
    }

    /**
     * Asserts that the button was mapped onto a concrete type.
     *
     * @param button the deserialized button
     */
    public static void assertFullyMapped(Button button) {
        if (button instanceof UnknownButton unknown) {
            throw drift("button type '" + unknown.type() + "' was not mapped to a concrete type",
                    unknown.rawJson());
        }
    }

    private static AssertionError drift(String what, String rawNodeJson) {
        return new AssertionError(String.format(
                "The live API returned something this library cannot represent: %s.%n"
                        + "This means the model has drifted from the API — fix the model, "
                        + "do not relax this assertion.%n"
                        + "Offending node:%n%s%n"
                        + "Full response body:%n%s",
                what, rawNodeJson, RecordingSerializer.lastJson()));
    }
}
