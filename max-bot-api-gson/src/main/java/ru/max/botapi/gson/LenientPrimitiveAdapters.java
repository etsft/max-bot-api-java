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
import java.util.Map;
import java.util.function.LongFunction;
import java.util.regex.Pattern;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Adapters for the numeric and boolean types that accept the same input as the Jackson adapter.
 *
 * <p>Gson's built-in adapters are stricter than Jackson's defaults, so one value Jackson reads
 * would fail the whole enclosing document on Gson — a {@code message_created} update would
 * degrade to {@code UnknownUpdate}. These adapters follow Jackson instead:</p>
 * <ul>
 *   <li>{@code null} and blank text read as {@code 0} / {@code false} for primitives and as
 *       {@code null} for boxed types</li>
 *   <li>integers: a fractional number is truncated toward zero and integer text is parsed;
 *       a value outside the type's range fails</li>
 *   <li>doubles: plain decimal text is parsed, and {@code "NaN"}, {@code "Infinity"} and
 *       {@code "INF"} (with an optional sign) are read when they match exactly; hex and suffixed
 *       forms such as {@code "0x1p3"} or {@code "1d"} fail, as do numbers longer than 1000
 *       characters. A number too large for a double reads as infinity; NaN and the infinities
 *       are written as strings</li>
 *   <li>booleans: an integer reads as {@code value != 0}; the text {@code true} or
 *       {@code false} is accepted in lower, capitalized or upper case</li>
 * </ul>
 */
final class LenientPrimitiveAdapters {

    /** Jackson's default {@code StreamReadConstraints.maxNumberLength}. */
    private static final int MAX_NUMBER_LENGTH = 1000;

    private LenientPrimitiveAdapters() {
    }

    /**
     * Registers the adapters for {@code long}, {@code int}, {@code double}, {@code boolean}
     * and their boxed types.
     *
     * @param builder the builder to register the adapters on
     */
    static void register(GsonBuilder builder) {
        builder.registerTypeAdapter(long.class,
                new IntegralAdapter<>(Long.MIN_VALUE, Long.MAX_VALUE, 0L, Long::valueOf));
        builder.registerTypeAdapter(Long.class,
                new IntegralAdapter<>(Long.MIN_VALUE, Long.MAX_VALUE, null, Long::valueOf));
        builder.registerTypeAdapter(int.class,
                new IntegralAdapter<>(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, v -> (int) v));
        builder.registerTypeAdapter(Integer.class,
                new IntegralAdapter<>(Integer.MIN_VALUE, Integer.MAX_VALUE, null, v -> (int) v));
        builder.registerTypeAdapter(double.class, new DoubleAdapter(0.0d));
        builder.registerTypeAdapter(Double.class, new DoubleAdapter(null));
        builder.registerTypeAdapter(boolean.class, new BooleanAdapter(Boolean.FALSE));
        builder.registerTypeAdapter(Boolean.class, new BooleanAdapter(null));
    }

    private static JsonSyntaxException mismatch(String expected, String found, JsonReader in) {
        return new JsonSyntaxException("Expected " + expected + " but was " + found
                + " at path " + in.getPreviousPath());
    }

    /**
     * Reads {@code long} or {@code int} values within {@code [min, max]}.
     *
     * @param <T> the boxed type
     */
    private static final class IntegralAdapter<T extends Number> extends TypeAdapter<T> {

        private final long min;
        private final long max;
        private final T blank;
        private final LongFunction<T> box;

        IntegralAdapter(long min, long max, T blank, LongFunction<T> box) {
            this.min = min;
            this.max = max;
            this.blank = blank;
            this.box = box;
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            out.value(value);
        }

        @Override
        public T read(JsonReader in) throws IOException {
            switch (in.peek()) {
                case NULL -> {
                    in.nextNull();
                    return blank;
                }
                case NUMBER -> {
                    String number = in.nextString();
                    return box.apply(inRange(parseNumber(number, in), number, in));
                }
                case STRING -> {
                    String text = in.nextString().strip();
                    if (text.isEmpty()) {
                        return blank;
                    }
                    try {
                        return box.apply(inRange(Long.parseLong(text), text, in));
                    } catch (NumberFormatException e) {
                        throw mismatch("an integer", "\"" + text + "\"", in);
                    }
                }
                default -> {
                    String found = in.peek().toString();
                    in.skipValue();
                    throw mismatch("an integer", found, in);
                }
            }
        }

        /** Parses a JSON number, truncating a fractional one toward zero. */
        private long parseNumber(String number, JsonReader in) {
            try {
                return Long.parseLong(number);
            } catch (NumberFormatException e) {
                // Fractional or exponent form; goes through double like Jackson, never BigDecimal,
                // whose cost grows with the exponent
                double value = Double.parseDouble(number);
                if (Double.isNaN(value) || value < min || value > max) {
                    throw mismatch("an integer in range", number, in);
                }
                return (long) value;
            }
        }

        private long inRange(long value, String raw, JsonReader in) {
            if (value < min || value > max) {
                throw mismatch("an integer in range", raw, in);
            }
            return value;
        }
    }

    /** Reads and writes {@code double} values, with NaN and the infinities as strings. */
    private static final class DoubleAdapter extends TypeAdapter<Double> {

        /** The texts Jackson reads as NaN or an infinity; matched exactly, without trimming. */
        private static final Map<String, Double> SPECIAL_VALUES = Map.of(
                "NaN", Double.NaN,
                "Infinity", Double.POSITIVE_INFINITY,
                "+Infinity", Double.POSITIVE_INFINITY,
                "-Infinity", Double.NEGATIVE_INFINITY,
                "INF", Double.POSITIVE_INFINITY,
                "+INF", Double.POSITIVE_INFINITY,
                "-INF", Double.NEGATIVE_INFINITY);

        /** Plain decimal notation; rejects the hex and suffixed forms that {@code parseDouble} allows. */
        private static final Pattern DECIMAL = Pattern.compile("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");

        private final Double blank;

        DoubleAdapter(Double blank) {
            this.blank = blank;
        }

        @Override
        public void write(JsonWriter out, Double value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else if (value.isNaN() || value.isInfinite()) {
                // Strict JSON has no literal for these; Jackson writes them as strings
                out.value(value.toString());
            } else {
                out.value(value.doubleValue());
            }
        }

        @Override
        public Double read(JsonReader in) throws IOException {
            switch (in.peek()) {
                case NULL -> {
                    in.nextNull();
                    return blank;
                }
                case NUMBER -> {
                    String number = in.nextString();
                    if (number.length() > MAX_NUMBER_LENGTH) {
                        throw mismatch("a number of at most " + MAX_NUMBER_LENGTH + " characters",
                                "a longer one", in);
                    }
                    return Double.parseDouble(number);
                }
                case STRING -> {
                    return readText(in.nextString(), in);
                }
                default -> {
                    String found = in.peek().toString();
                    in.skipValue();
                    throw mismatch("a number", found, in);
                }
            }
        }

        /**
         * Reads numeric text into a {@code double} the way Jackson 3.3 does.
         *
         * <p>{@code NaN} and the infinities must match exactly. Other text is trimmed of ASCII
         * whitespace ({@link String#trim()}, as Jackson does), blank text is the default value, and
         * the rest must be plain decimal notation of at most {@value #MAX_NUMBER_LENGTH} characters.</p>
         */
        private Double readText(String raw, JsonReader in) {
            Double special = SPECIAL_VALUES.get(raw);
            if (special != null) {
                return special;
            }
            String text = raw.trim();
            if (text.isEmpty()) {
                return blank;
            }
            if (text.length() > MAX_NUMBER_LENGTH || !DECIMAL.matcher(text).matches()) {
                throw mismatch("a number", "\"" + truncate(text) + "\"", in);
            }
            return Double.parseDouble(text);
        }

        private static String truncate(String text) {
            return text.length() <= 64 ? text : text.substring(0, 64) + "...";
        }
    }

    /** Reads {@code boolean} values from booleans, integers and boolean text. */
    private static final class BooleanAdapter extends TypeAdapter<Boolean> {

        private final Boolean blank;

        BooleanAdapter(Boolean blank) {
            this.blank = blank;
        }

        @Override
        public void write(JsonWriter out, Boolean value) throws IOException {
            out.value(value);
        }

        @Override
        public Boolean read(JsonReader in) throws IOException {
            switch (in.peek()) {
                case NULL -> {
                    in.nextNull();
                    return blank;
                }
                case BOOLEAN -> {
                    return in.nextBoolean();
                }
                case NUMBER -> {
                    String number = in.nextString();
                    if (!number.matches("-?\\d+")) {
                        throw mismatch("a boolean or an integer", number, in);
                    }
                    return number.chars().anyMatch(c -> c >= '1' && c <= '9');
                }
                case STRING -> {
                    String text = in.nextString().strip();
                    return switch (text) {
                        case "" -> blank;
                        case "true", "True", "TRUE" -> Boolean.TRUE;
                        case "false", "False", "FALSE" -> Boolean.FALSE;
                        default -> throw mismatch("a boolean", "\"" + text + "\"", in);
                    };
                }
                default -> {
                    String found = in.peek().toString();
                    in.skipValue();
                    throw mismatch("a boolean", found, in);
                }
            }
        }
    }
}
