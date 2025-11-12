package io.quarkus.apicurio.registry.common;

import org.junit.jupiter.api.Test;

import io.apicurio.registry.client.RegistryClientFactory;

public class ApicurioRegistryInternalsExpectationTest {
    @Test
    public void test() throws NoSuchFieldException {
        // we need this to reset the client in continuous testing
        RegistryClientFactory.class.getDeclaredField("vertx");
    }
}
