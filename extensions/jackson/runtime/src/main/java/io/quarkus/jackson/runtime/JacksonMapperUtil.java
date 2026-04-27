package io.quarkus.jackson.runtime;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializerProvider;

public class JacksonMapperUtil {

    public static boolean isViewIncluded(Class<?> activeView, Class<?>[] viewClasses) {
        if (activeView == null) {
            return true;
        }
        for (Class<?> viewClass : viewClasses) {
            if (viewClass.isAssignableFrom(activeView)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes a field name to the JSON generator, translating through the ObjectMapper's
     * {@link PropertyNamingStrategy} if one is configured.
     * When no strategy is set, the pre-encoded {@code defaultName} is used for zero overhead.
     */
    public static void writeFieldName(JsonGenerator gen, PropertyNamingStrategy strategy,
            String javaFieldName, SerializableString defaultName) throws IOException {
        if (strategy == null) {
            gen.writeFieldName(defaultName);
        } else {
            gen.writeFieldName(strategy.nameForField(null, null, javaFieldName));
        }
    }

    /**
     * Builds a reverse-translation index mapping strategy-translated JSON field names back to
     * Java field names. Called once at the start of deserialization so that per-field lookups
     * are O(1) via {@link Map#getOrDefault} instead of O(n) scans.
     */
    public static Map<String, String> buildReverseNameIndex(PropertyNamingStrategy strategy,
            String[] translatableFieldNames) {
        Map<String, String> index = new HashMap<>();
        for (String javaName : translatableFieldNames) {
            index.put(strategy.nameForField(null, null, javaName), javaName);
        }
        return index;
    }

    public static JavaType[] getGenericsJavaTypes(DeserializationContext context, BeanProperty property) {
        JavaType wrapperType = property != null ? property.getType() : context.getContextualType();
        JavaType[] valueTypes = new JavaType[wrapperType.containedTypeCount()];
        for (int i = 0; i < valueTypes.length; i++) {
            valueTypes[i] = wrapperType.containedType(i);
        }
        return valueTypes;
    }

    public static void serializePojo(Object value, JsonGenerator generator, SerializerProvider serializerProvider)
            throws IOException {
        if (value == null || value instanceof Map) {
            generator.writePOJO(value);
            return;
        }
        JsonSerializer<Object> serializer = serializerProvider.findValueSerializer(value.getClass());
        if (serializer != null) {
            serializer.serialize(value, generator, serializerProvider);
        } else {
            generator.writePOJO(value);
        }
    }

    public enum SerializationInclude {

        ALWAYS,
        NON_NULL,
        NON_ABSENT,
        NON_EMPTY;

        public static SerializationInclude decode(Object object, SerializerProvider serializerProvider) {
            JsonInclude.Include include = serializerProvider.getDefaultPropertyInclusion(object.getClass()).getValueInclusion();
            return switch (include) {
                case NON_EMPTY -> NON_EMPTY;
                case NON_NULL -> NON_NULL;
                case NON_ABSENT -> NON_ABSENT;
                default -> ALWAYS;
            };
        }

        public boolean shouldSerialize(Object value) {
            return switch (this) {
                case ALWAYS -> true;
                case NON_NULL -> value != null;
                case NON_ABSENT -> isPresent(value);
                case NON_EMPTY -> hasValue(value);
            };
        }

        private boolean isPresent(Object value) {
            if (value == null) {
                return false;
            }
            if (value instanceof Optional o) {
                return o.isPresent();
            }
            return true;
        }

        private boolean hasValue(Object value) {
            if (!isPresent(value)) {
                return false;
            }
            if (value instanceof String s) {
                return !s.isEmpty();
            }
            if (value instanceof Collection c) {
                return !c.isEmpty();
            }
            if (value instanceof Map m) {
                return !m.isEmpty();
            }
            if (value.getClass().isArray()) {
                return Array.getLength(value) > 0;
            }
            return true;
        }
    }
}
