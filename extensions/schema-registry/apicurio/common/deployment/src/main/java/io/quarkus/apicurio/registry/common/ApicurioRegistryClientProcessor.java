package io.quarkus.apicurio.registry.common;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.smallrye.openapi.deployment.spi.IgnoreStaticDocumentBuildItem;

public class ApicurioRegistryClientProcessor {

    @BuildStep
    public void apicurioRegistryClient(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {
        // TODO cleanup this list of removed classes
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder("io.apicurio.rest.client.auth.exception.NotAuthorizedException",
                        "io.apicurio.rest.client.auth.exception.ForbiddenException",
                        "io.apicurio.rest.client.auth.exception.AuthException",
                        "io.apicurio.rest.client.auth.exception.AuthErrorHandler",
                        "io.apicurio.rest.client.auth.request.TokenRequestsProvider",
                        "io.apicurio.rest.client.request.Request",
                        "io.apicurio.rest.client.auth.AccessTokenResponse",
                        "io.apicurio.rest.client.auth.Auth",
                        "io.apicurio.rest.client.auth.BasicAuth",
                        "io.apicurio.rest.client.auth.OidcAuth",
                        "io.apicurio.registry.serde.Default4ByteIdHandler",
                        "io.apicurio.registry.serde.Legacy8ByteIdHandler").methods().fields().build());
    }

    @BuildStep
    void ignoreIncludedOpenAPIDocument(BuildProducer<IgnoreStaticDocumentBuildItem> ignoreStaticDocumentProducer) {
        // This will ignore the OpenAPI Document in META-INF/openapi.yaml in the apicurio-registry-common dependency
        ignoreStaticDocumentProducer.produce(new IgnoreStaticDocumentBuildItem(
                ".*/io/apicurio/apicurio-registry-common/.*/apicurio-registry-common-.*.jar.*"));
    }

    @BuildStep
    void runtimeInitializedVertxHolder(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitClass) {
        runtimeInitClass
                .produce(new RuntimeInitializedClassBuildItem("io.apicurio.registry.client.DefaultVertxInstance$Holder"));
    }

}
