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

package ru.max.botapi.model;

import java.util.List;
import java.util.Objects;

/**
 * Copies lists read from API responses.
 *
 * <p>A response list can hold {@code null} elements: a JSON {@code null}, or an enum value this
 * library version does not know, which the serializers read as {@code null}. Such an element
 * is dropped, so one odd element does not fail the whole response with a
 * {@link NullPointerException} from {@link List#copyOf}. Request bodies keep using
 * {@code List.copyOf}: a {@code null} element there is a bug in the calling code.</p>
 */
final class ModelLists {

    private ModelLists() {
    }

    /**
     * Returns an immutable copy of {@code list} without its {@code null} elements.
     *
     * @param list the list; must not be {@code null}
     * @param name the component name used in the exception message
     * @param <T>  the element type
     * @return the immutable copy
     * @throws NullPointerException if {@code list} is {@code null}
     */
    static <T> List<T> withoutNulls(List<T> list, String name) {
        Objects.requireNonNull(list, () -> name + " must not be null");
        return list.stream().filter(Objects::nonNull).toList();
    }

    /**
     * Returns an immutable copy of {@code list} without its {@code null} elements, or
     * {@code null} if {@code list} is {@code null}.
     *
     * @param list the list, or {@code null}
     * @param <T>  the element type
     * @return the immutable copy, or {@code null}
     */
    static <T> @Nullable List<T> withoutNullsOrNull(@Nullable List<T> list) {
        return list == null ? null : list.stream().filter(Objects::nonNull).toList();
    }
}
