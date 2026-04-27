package io.quarkus.jackson.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.RuntimeInit;
import io.quarkus.runtime.annotations.StaticInit;

@Recorder
public class JacksonRecorder {

    @StaticInit
    public Supplier<JacksonSupport> supplier(Optional<String> propertyNamingStrategyClassName) {
        return new Supplier<>() {
            @Override
            public JacksonSupport get() {
                return new JacksonSupport() {
                    @Override
                    public Optional<PropertyNamingStrategies.NamingBase> configuredNamingStrategy() {
                        if (propertyNamingStrategyClassName.isPresent()) {
                            try {
                                var value = (PropertyNamingStrategies.NamingBase) Class
                                        .forName(propertyNamingStrategyClassName.get(), true,
                                                Thread.currentThread()
                                                        .getContextClassLoader())
                                        .getDeclaredConstructor().newInstance();
                                return Optional.of(value);
                            } catch (Exception e) {
                                // shouldn't happen as propertyNamingStrategyClassName is validated at build time
                                throw new RuntimeException(e);
                            }
                        }
                        return Optional.empty();
                    }
                };
            }
        };
    }

    @StaticInit
    public Supplier<ObjectMapperCustomizer> customizerSupplier(Map<Class<?>, Class<?>> mixinsMap) {
        return new Supplier<>() {
            @Override
            public ObjectMapperCustomizer get() {
                return new ObjectMapperCustomizer() {
                    @Override
                    public void customize(ObjectMapper objectMapper) {
                        for (var entry : mixinsMap.entrySet()) {
                            objectMapper.addMixIn(entry.getKey(), entry.getValue());
                        }
                    }

                    @Override
                    public int priority() {
                        return DEFAULT_PRIORITY + 1;
                    }
                };
            }
        };
    }

    @SuppressWarnings("unchecked")
    @StaticInit
    public void recordGeneratedSerializer(String className) {
        ReflectionFreeSerializersRegister.addSerializer((Class<? extends StdSerializer>) loadClass(className));
    }

    @SuppressWarnings("unchecked")
    @StaticInit
    public void recordGeneratedDeserializer(String className) {
        ReflectionFreeSerializersRegister.addDeserializer((Class<? extends StdDeserializer>) loadClass(className));
    }

    private Class<?> loadClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "Unable to load generated Jackson serializer/deserializer class '" + className + "'", e);
        }
    }

    @RuntimeInit
    public void clearCachesOnShutdown(ShutdownContext shutdownContext) {
        shutdownContext.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                TypeFactory.defaultInstance().clearCache();
            }
        });
    }
}
