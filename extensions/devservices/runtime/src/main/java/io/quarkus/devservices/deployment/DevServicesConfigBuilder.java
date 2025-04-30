package io.quarkus.devservices.deployment;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

public class DevServicesConfigBuilder implements ConfigBuilder {

    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        return builder.withSources(new io.quarkus.devservices.deployment.DevServicesConfigSource());
    }

    @Override
    public int priority() {
        // We feel more important than config in environment variables and application.properties files, but dev services should be looking at those sources and not doing anything if there's existing config,
        // so a very low priority is also arguably correct.

        return 500;
    }
}
