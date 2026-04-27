package io.quarkus.jackson.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item that allows extensions to register custom field-level serialization
 * filters for the reflection-free Jackson serializer code generator.
 * <p>
 * When a field annotated with the specified annotation is encountered during
 * serialization code generation, the generator will emit bytecode that calls
 * the specified static filter method. The filter method must have the signature:
 * {@code boolean filterMethod(SerializerProvider, String[])} where the String[]
 * comes from the annotation value.
 * <p>
 * If the filter method returns {@code false}, the field is excluded from serialization.
 */
public final class JacksonSerializationFieldFilterBuildItem extends MultiBuildItem {

    private final String annotationName;
    private final String annotationValueName;
    private final String filterClassName;
    private final String filterMethodName;

    /**
     * @param annotationName fully qualified name of the annotation (e.g. "io.quarkus.resteasy.reactive.jackson.SecureField")
     * @param annotationValueName name of the annotation value that provides String[] (e.g. "rolesAllowed")
     * @param filterClassName fully qualified name of the runtime class containing the filter method
     * @param filterMethodName name of the static method with signature {@code boolean(SerializerProvider, String[])}
     */
    public JacksonSerializationFieldFilterBuildItem(String annotationName, String annotationValueName,
            String filterClassName, String filterMethodName) {
        this.annotationName = annotationName;
        this.annotationValueName = annotationValueName;
        this.filterClassName = filterClassName;
        this.filterMethodName = filterMethodName;
    }

    public String getAnnotationName() {
        return annotationName;
    }

    public String getAnnotationValueName() {
        return annotationValueName;
    }

    public String getFilterClassName() {
        return filterClassName;
    }

    public String getFilterMethodName() {
        return filterMethodName;
    }
}
