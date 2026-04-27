package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.quarkus.jackson.runtime.ReflectionFreeSerializersRegister;

/**
 * @deprecated Replaced by {@link ReflectionFreeSerializersRegister}.
 *             Kept for backwards compatibility — delegates all calls to the jackson runtime equivalent.
 */
@Deprecated(forRemoval = true)
public class GeneratedSerializersRegister {

    public static void addSerializer(Class<? extends StdSerializer> serClass) {
        ReflectionFreeSerializersRegister.addSerializer(serClass);
    }

    public static void addDeserializer(Class<? extends StdDeserializer> deserClass) {
        ReflectionFreeSerializersRegister.addDeserializer(deserClass);
    }
}
