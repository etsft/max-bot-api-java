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
import java.util.Locale;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.Serializers;

/**
 * Jackson module that serializes all enums as lowercase snake_case
 * and deserializes case-insensitively.
 */
final class EnumLowercaseModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    EnumLowercaseModule() {
        super("EnumLowercaseModule");
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.addSerializers(new EnumSerializers());
        context.addDeserializers(new EnumDeserializers());
    }

    private static final class EnumSerializers extends Serializers.Base {
        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public JsonSerializer<?> findSerializer(
                SerializationConfig config,
                com.fasterxml.jackson.databind.JavaType type,
                BeanDescription beanDesc
        ) {
            if (type.isEnumType()) {
                return new LowercaseEnumSerializer((Class<Enum<?>>) type.getRawClass());
            }
            return null;
        }
    }

    private static final class EnumDeserializers extends Deserializers.Base {
        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public JsonDeserializer<?> findEnumDeserializer(
                Class<?> type,
                DeserializationConfig config,
                BeanDescription beanDesc
        ) {
            if (type.isEnum()) {
                return new CaseInsensitiveEnumDeserializer((Class<Enum<?>>) type);
            }
            return null;
        }
    }

    private static final class LowercaseEnumSerializer extends JsonSerializer<Enum<?>> {

        private final Class<Enum<?>> enumType;

        LowercaseEnumSerializer(Class<Enum<?>> enumType) {
            this.enumType = enumType;
        }

        @Override
        public void serialize(Enum<?> value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(value.name().toLowerCase(Locale.ROOT));
        }

        @Override
        public Class<Enum<?>> handledType() {
            return enumType;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final class CaseInsensitiveEnumDeserializer extends JsonDeserializer<Enum<?>> {

        private final Class<? extends Enum> enumType;

        CaseInsensitiveEnumDeserializer(Class<? extends Enum> enumType) {
            this.enumType = enumType;
        }

        @Override
        public Enum<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.isBlank()) {
                return (Enum<?>) ctxt.handleUnexpectedToken(enumType, p);
            }
            try {
                return Enum.valueOf(enumType, text.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                // Forward-compatible: unknown enum values return null instead of throwing,
                // so new API values never crash running bots. Annotate enum fields with
                // @Nullable where unknown values are expected.
                return null;
            }
        }
    }
}
