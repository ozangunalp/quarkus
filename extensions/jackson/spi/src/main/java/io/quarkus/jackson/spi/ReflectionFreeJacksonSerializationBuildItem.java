package io.quarkus.jackson.spi;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * BuildItem used to request build-time generation of reflection-free Jackson
 * serializers and/or deserializers for a given class.
 *
 * Any extension can produce this build item. The Jackson deployment processor
 * will consume all instances, generate {@code StdSerializer} /
 * {@code StdDeserializer} implementations via Gizmo, and register them on
 * the shared {@code ObjectMapper}.
 */
public final class ReflectionFreeJacksonSerializationBuildItem extends MultiBuildItem {

    private final ClassInfo classInfo;

    public ReflectionFreeJacksonSerializationBuildItem(ClassInfo classInfo) {
        this.classInfo = classInfo;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }
}
