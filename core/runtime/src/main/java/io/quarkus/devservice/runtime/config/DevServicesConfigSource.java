package io.quarkus.devservice.runtime.config;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.runtime.DevServicesConfigTracker;

// This should live in the devservices/runtime module, but that module doesn't exist, and adding it is a breaking change
public class DevServicesConfigSource implements ConfigSource {

    DevServicesConfigTracker devServiceTrackerBuildItem = new DevServicesConfigTracker();

    @Override
    public Set<String> getPropertyNames() {
        return Set.of();
    }

    @Override
    public String getValue(String propertyName) {
        for (Object o : devServiceTrackerBuildItem.getAllRunningServices()) {
            Map config = (Map) ((Supplier) o).get();
            return (String) config.get(propertyName);

        }
        return null;
    }

    @Override
    public String getName() {
        return "DevServicesConfigSource";
    }

    @Override
    public int getOrdinal() {
        // greater than any default Microprofile ConfigSource
        return 500;
    }
}
