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

package ru.max.botapi.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import ru.max.botapi.model.Update;

/**
 * An {@link UpdateHandler} that dispatches each update to the handler registered for its type.
 *
 * <p>Instead of one handler with a hand-written {@code switch}, register a handler per update
 * type; the handler receives the update already cast:</p>
 * <pre>{@code
 * UpdateHandler handler = UpdateRouter.builder()
 *         .on(MessageCreatedUpdate.class, u -> reply(u.message()))
 *         .on(MessageCallbackUpdate.class, u -> answer(u.callback()))
 *         .otherwise(u -> log.debug("Ignored {}", u.updateType()))
 *         .build();
 * }</pre>
 *
 * <p>An update with no registered handler goes to the {@link Builder#otherwise(UpdateHandler)
 * fallback}, which ignores it by default. Updates of a type this library does not know arrive
 * as {@link ru.max.botapi.model.UnknownUpdate} and can be routed like any other type.</p>
 *
 * <p>The router is immutable and thread-safe if the registered handlers are.</p>
 */
public final class UpdateRouter implements UpdateHandler {

    private final Map<Class<? extends Update>, UpdateHandler> routes;
    private final UpdateHandler fallback;

    private UpdateRouter(Builder builder) {
        this.routes = Map.copyOf(builder.routes);
        this.fallback = builder.fallback;
    }

    /**
     * Returns a builder for an update router.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Hands the update to the handler registered for its type, or to the fallback.
     *
     * @param update the update; must not be {@code null}
     */
    @Override
    public void onUpdate(Update update) {
        Objects.requireNonNull(update, "update must not be null");
        routes.getOrDefault(update.getClass(), fallback).onUpdate(update);
    }

    /**
     * Builder for {@link UpdateRouter}.
     */
    public static final class Builder {

        private final Map<Class<? extends Update>, UpdateHandler> routes = new LinkedHashMap<>();
        private UpdateHandler fallback = update -> { };

        private Builder() {
        }

        /**
         * Registers the handler for one update type.
         *
         * @param type    the concrete update record, such as {@code MessageCreatedUpdate.class};
         *                must not be {@code null}
         * @param handler receives updates of that type; must not be {@code null}
         * @param <U>     the update type
         * @return this builder
         * @throws IllegalArgumentException if {@code type} is not a concrete update record, or a
         *                                  handler is already registered for it
         */
        public <U extends Update> Builder on(Class<U> type, Consumer<? super U> handler) {
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(handler, "handler must not be null");
            if (!type.isRecord()) {
                throw new IllegalArgumentException(
                        "type must be a concrete update record, got: " + type.getName());
            }
            UpdateHandler route = update -> handler.accept(type.cast(update));
            if (routes.putIfAbsent(type, route) != null) {
                throw new IllegalArgumentException("a handler is already registered for "
                        + type.getSimpleName());
            }
            return this;
        }

        /**
         * Sets the handler for updates no other handler is registered for. By default they are
         * ignored.
         *
         * @param fallback the fallback handler; must not be {@code null}
         * @return this builder
         */
        public Builder otherwise(UpdateHandler fallback) {
            this.fallback = Objects.requireNonNull(fallback, "fallback must not be null");
            return this;
        }

        /**
         * Builds the router.
         *
         * @return the router
         */
        public UpdateRouter build() {
            return new UpdateRouter(this);
        }
    }
}
