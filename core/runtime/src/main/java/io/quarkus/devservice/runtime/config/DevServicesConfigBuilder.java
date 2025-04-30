package io.quarkus.devservice.runtime.config;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

// This should live in the devservices/runtime module, but that module doesn't exist, and adding it is a breaking change
public class DevServicesConfigBuilder implements ConfigBuilder {

    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        return builder.withSources(new DevServicesConfigSource());
    }

    @Override
    public int priority() {
        // We feel more important than config in environment variables and application.properties files, but dev services should be looking at those sources and not doing anything if there's existing config,
        // so a very low priority is also arguably correct.

        return 500;
    }
}
